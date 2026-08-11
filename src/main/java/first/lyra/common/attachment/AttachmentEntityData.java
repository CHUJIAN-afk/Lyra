package first.lyra.common.attachment;

import first.lyra.api.LyraHelper;
import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.entity.AttachmentEntityType;
import first.lyra.register.LyraRegistries;
import first.lyra.common.servant.Servant;
import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 统一的附件实体数据附件。
 * <p>
 * 使用 Type → AttachmentEntityType 两级分组存储实体。
 * 移除通过 setRemove() 标记完成，添加通过延迟队列在 tick 后统一处理。
 * </p>
 */
public class AttachmentEntityData implements AttachmentSyncHandler<AttachmentEntityData> {

    private final Map<Type, Map<AttachmentEntityType<?>, List<AttachmentEntity>>> pendingAdd = new EnumMap<>(Type.class);
    private final Map<Type, Map<AttachmentEntityType<?>, List<AttachmentEntity>>> groups = new EnumMap<>(Type.class);
    private final List<AttachmentEntity> renderCache = new ArrayList<>();
    private ResourceKey<Level> dimension = null;
    private boolean changed = false;

    public void tick(Player player) {
        if (isRunning()) {
            updateLevel(player);
            updateServantSlot(player);
            tickEntity(player);
            syncToClient(player);
        }
    }

    private void updateLevel(Player player) {
        Level level = player.level();
        if (!level.isClientSide() && dimension != level.dimension()) {
            if (dimension != null) {
                groups.values()
                        .stream()
                        .map(Map::values)
                        .flatMap(Collection::stream)
                        .flatMap(Collection::stream)
                        .forEach(AttachmentEntity::dimensionChange);
            }
            dimension = level.dimension();
        }
    }

    public boolean isRunning() {
        return !groups.isEmpty() || !pendingAdd.isEmpty() || changed;
    }

    private void syncToClient(Player player) {
        if (!player.level().isClientSide() && (!groups.isEmpty() || changed)) {
            changed = false;
            player.syncData(LyraAttachmentRegister.EntityData);
        }
    }

    private void tickEntity(Player player) {
        renderCache.clear();
        boolean clientSide = player.level().isClientSide();
        for (Map<AttachmentEntityType<?>, List<AttachmentEntity>> map : groups.values()) {
            for (List<AttachmentEntity> list : map.values()) {
                for (AttachmentEntity entity : list) {
                    entity.setOwner(player);
                    entity.tick();
                    if (clientSide) {
                        renderCache.add(entity);
                    }
                }
            }
        }
    }

    private void updateServantSlot(Player player) {
        if (!player.level().isClientSide()) {
            // 将待添加队列合并到主分组
            if (!pendingAdd.isEmpty()) {
                for (Map.Entry<Type, Map<AttachmentEntityType<?>, List<AttachmentEntity>>> entry : pendingAdd.entrySet()) {
                    Type type = entry.getKey();
                    for (List<AttachmentEntity> value : entry.getValue().values()) {
                        for (AttachmentEntity attachmentEntity : value) {
                            List<AttachmentEntity> entities = groups.computeIfAbsent(type, key1 -> new HashMap<>())
                                    .computeIfAbsent(attachmentEntity.getType(), key -> new ArrayList<>());
                            entities.add(attachmentEntity);
                            for (AttachmentEntity entity : entities) {
                                if (entity instanceof Servant servant) {
                                    servant.setOrder(servant.getOrder());
                                    servant.setSameSize(servant.getSameSize());
                                }
                            }
                        }
                    }
                }
                pendingAdd.clear();
                changed = true;
            }
            // 标记溢出实体
            Type[] types = new Type[]{Type.Servant, Type.SentryServant};
            LyraHelper helper = LyraHelper.get(player);
            for (Type type : types) {
                List<Servant> servants = get(type, Servant.class);
                while (true) {
                    if (servants.isEmpty()) {
                        break;
                    }
                    if (servants.stream().allMatch(AttachmentEntity::isRemove)) {
                        break;
                    }
                    if (!helper.canSummon(type, 0)) {
                        Servant first = servants.getFirst();
                        first.setRemove();
                        servants.remove(first);
                        changed = true;
                    } else {
                        break;
                    }
                }
            }
            // 清理所有分组中标记移除的实体
            for (Map<AttachmentEntityType<?>, List<AttachmentEntity>> map : groups.values()) {
                map.values().removeIf(list -> {
                    while (true) {
                        list.removeIf(entity -> {
                            if (entity.isRemove()) {
                                entity.onRemove();
                                changed = true;
                                return true;
                            }
                            return false;
                        });
                        if (list.isEmpty()) {
                            break;
                        }
                        if (list.stream().noneMatch(AttachmentEntity::isRemove)) {
                            break;
                        }
                    }
                    return list.isEmpty();
                });
            }
        }
    }

    public void add(Type type, AttachmentEntity entity) {
        pendingAdd.computeIfAbsent(type, k -> new HashMap<>()).computeIfAbsent(entity.getType(), k -> new ArrayList<>()).add(entity);
    }

    @SuppressWarnings("unchecked")
    public <T extends AttachmentEntity> List<T> get(Type type, AttachmentEntityType<T> attachmentEntityType) {
        List<T> result = new ArrayList<>();
        groups.getOrDefault(type, new HashMap<>())
                .getOrDefault(attachmentEntityType, new ArrayList<>())
                .stream()
                .filter(entity -> !entity.isRemove())
                .map(entity -> (T) entity)
                .forEach(result::add);
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends AttachmentEntity> List<T> get(Type type, Class<T> classType) {
        List<T> result = new ArrayList<>();
        groups.getOrDefault(type, new HashMap<>())
                .values()
                .stream()
                .flatMap(Collection::stream)
                .filter(entity -> !entity.isRemove())
                .filter(classType::isInstance)
                .map(entity -> (T) entity)
                .forEach(result::add);
        return result;
    }

    public void remove(Type type, AttachmentEntityType<?> entityType) {
        getGroups().getOrDefault(type, new HashMap<>())
                .getOrDefault(entityType, new ArrayList<>())
                .forEach(AttachmentEntity::setRemove);
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf, AttachmentEntityData data, boolean isSelf) {
        // 写入 Type → AttachmentEntityType → 实体列表的三层结构
        buf.writeVarInt(data.groups.size());
        for (Map.Entry<Type, Map<AttachmentEntityType<?>, List<AttachmentEntity>>> typeEntry : data.groups.entrySet()) {
            buf.writeEnum(typeEntry.getKey());
            Map<AttachmentEntityType<?>, List<AttachmentEntity>> inner = typeEntry.getValue();
            buf.writeVarInt(inner.size());
            for (Map.Entry<AttachmentEntityType<?>, List<AttachmentEntity>> entityEntry : inner.entrySet()) {
                Identifier typeId = LyraRegistries.ATTACHMENT_ENTITY_TYPES.getKey(entityEntry.getKey());
                assert typeId != null;
                buf.writeIdentifier(typeId);
                List<AttachmentEntity> list = entityEntry.getValue();
                buf.writeVarInt(list.size());
                for (AttachmentEntity entity : list) {
                    buf.writeUUID(entity.getUuid());
                    entity.writeBase(buf);
                    entity.writeAdditional(buf);
                }
            }
        }
    }

    // ===================== 网络同步 =====================

    @Override
    public AttachmentEntityData read(@NotNull IAttachmentHolder holder, @NotNull RegistryFriendlyByteBuf buf, @Nullable AttachmentEntityData oldData) {
        AttachmentEntityData data = oldData != null ? oldData : new AttachmentEntityData();

        // 保留现有实体的缓存引用
        Map<UUID, AttachmentEntity> existing = new HashMap<>();
        for (Map<AttachmentEntityType<?>, List<AttachmentEntity>> inner : data.groups.values()) {
            for (List<AttachmentEntity> list : inner.values()) {
                for (AttachmentEntity e : list) {
                    existing.put(e.getUuid(), e);
                }
            }
        }

        // 清空分组和待添加队列
        data.groups.clear();
        data.pendingAdd.clear();

        // 读取 Type → AttachmentEntityType → 实体列表的三层结构
        int typeCount = buf.readVarInt();
        for (int i = 0; i < typeCount; i++) {
            Type type = buf.readEnum(Type.class);
            Map<AttachmentEntityType<?>, List<AttachmentEntity>> inner = data.groups.computeIfAbsent(type, k -> new HashMap<>());
            int entityCount = buf.readVarInt();
            for (int j = 0; j < entityCount; j++) {
                Identifier typeId = buf.readIdentifier();
                // 26.2: Registry.get 返回 Optional<Holder.Reference>
                AttachmentEntityType<?> entityType = LyraRegistries.ATTACHMENT_ENTITY_TYPES.get(typeId).map(Holder.Reference::value).orElse(null);
                assert entityType != null;
                List<AttachmentEntity> list = inner.computeIfAbsent(entityType, k -> new ArrayList<>());
                int listSize = buf.readVarInt();
                for (int k = 0; k < listSize; k++) {
                    UUID uuid = buf.readUUID();
                    AttachmentEntity entity = existing.get(uuid);
                    if (entity == null) {
                        entity = entityType.factory().get();
                        entity.setUuid(uuid);
                    }
                    entity.readBase(buf);
                    entity.readAdditional(buf);
                    list.add(entity);
                }
            }
        }

        return data;
    }

    public Map<Type, Map<AttachmentEntityType<?>, List<AttachmentEntity>>> getGroups() {
        return groups;
    }

    public List<AttachmentEntity> getRenderCache() {
        return renderCache;
    }

    public enum Type {
        Servant,
        SentryServant,
        ExtraServant,
        Projectile
    }
}