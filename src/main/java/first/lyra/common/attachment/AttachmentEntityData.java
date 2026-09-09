package first.lyra.common.attachment;

import first.lyra.api.LyraHelper;
import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.entity.AttachmentEntityType;
import first.lyra.common.minion.Minion;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraRegistries;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.network.connection.ConnectionType;
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

    /** 尚未应用到客户端 tick 的原始同步载荷 */
    private byte[] pendingPayload;

    public void tick(Player player) {
        if (isRunning()) {
            applyPendingSync(player);
            updateLevel(player);
            updateMinionSlot(player);
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
        return pendingPayload != null || !groups.isEmpty() || !pendingAdd.isEmpty() || changed;
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

    private void updateMinionSlot(Player player) {
        if (!player.level().isClientSide()) {
            // 将待添加队列合并到主分组
            if (!pendingAdd.isEmpty()) {
                for (Map.Entry<Type, Map<AttachmentEntityType<?>, List<AttachmentEntity>>> entry : pendingAdd.entrySet()) {
                    Type type = entry.getKey();
                    for (List<AttachmentEntity> value : entry.getValue().values()) {
                        for (AttachmentEntity attachmentEntity : value) {
                            List<AttachmentEntity> entities = groups.computeIfAbsent(type, key1 -> new HashMap<>()).computeIfAbsent(attachmentEntity.getType(), key -> new ArrayList<>());
                            entities.add(attachmentEntity);
                            for (AttachmentEntity entity : entities) {
                                if (entity instanceof Minion minion) {
                                    minion.setOrder(minion.getOrder());
                                    minion.setSameSize(minion.getSameSize());
                                }
                            }
                        }
                    }
                }
                pendingAdd.clear();
                changed = true;
            }
            // 标记溢出实体
            Type[] types = new Type[]{Type.Minion, Type.Sentry};
            LyraHelper helper = LyraHelper.get(player);
            for (Type type : types) {
                List<Minion> minions = get(type, Minion.class);
                while (true) {
                    if (minions.isEmpty()) {
                        break;
                    }
                    if (player.isDeadOrDying() || !player.isAlive()) {
                        for (Minion minion : minions) {
                            minion.setRemove();
                            changed = true;
                        }
                        break;
                    }
                    if (minions.stream().allMatch(AttachmentEntity::isRemove)) {
                        break;
                    }
                    if (!helper.canSummon(type, 0)) {
                        Minion first = minions.getFirst();
                        first.setRemove();
                        minions.remove(first);
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
                ResourceLocation typeId = LyraRegistries.ATTACHMENT_ENTITY_TYPES.getKey(entityEntry.getKey());
                assert typeId != null;
                buf.writeResourceLocation(typeId);
                List<AttachmentEntity> list = entityEntry.getValue();
                buf.writeVarInt(list.size());
                for (AttachmentEntity entity : list) {
                    buf.writeUUID(entity.getUuid());
                    entity.syncFieldRegistrar().encode(buf, entity.getOwner().level());
                }
            }
        }
    }

    // ===================== 网络同步 =====================

    @Override
    public AttachmentEntityData read(@NotNull IAttachmentHolder holder, @NotNull RegistryFriendlyByteBuf buf, @Nullable AttachmentEntityData oldData) {
        AttachmentEntityData data = oldData != null ? oldData : new AttachmentEntityData();
        ByteBuf copy = buf.copy();
        data.pendingPayload = new byte[copy.readableBytes()];
        copy.readBytes(data.pendingPayload);
        return data;
    }

    /** 网络包到达时只暂存载荷，在客户端 tick 起点执行真实解码。 */
    private void applyPendingSync(Player player) {
        if (this.pendingPayload != null) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(this.pendingPayload), player.registryAccess(), ConnectionType.NEOFORGE);
            // 保留现有实体的缓存引用
            Map<UUID, AttachmentEntity> existing = new HashMap<>();
            for (Map<AttachmentEntityType<?>, List<AttachmentEntity>> inner : groups.values()) {
                for (List<AttachmentEntity> list : inner.values()) {
                    for (AttachmentEntity e : list) {
                        existing.put(e.getUuid(), e);
                    }
                }
            }

            // 清空分组和待添加队列
            groups.clear();
            pendingAdd.clear();

            // 读取 Type → AttachmentEntityType → 实体列表的三层结构
            int typeCount = buf.readVarInt();
            for (int i = 0; i < typeCount; i++) {
                Type type = buf.readEnum(Type.class);
                Map<AttachmentEntityType<?>, List<AttachmentEntity>> inner = groups.computeIfAbsent(type, k -> new HashMap<>());
                int entityCount = buf.readVarInt();
                for (int j = 0; j < entityCount; j++) {
                    ResourceLocation typeId = buf.readResourceLocation();
                    AttachmentEntityType<?> entityType = LyraRegistries.ATTACHMENT_ENTITY_TYPES.get(typeId);
                    assert entityType != null;
                    List<AttachmentEntity> list = inner.computeIfAbsent(entityType, k -> new ArrayList<>());
                    int listSize = buf.readVarInt();
                    for (int k = 0; k < listSize; k++) {
                        UUID uuid = buf.readUUID();
                        AttachmentEntity entity = existing.get(uuid);
                        boolean firstSync = entity == null;
                        if (entity == null) {
                            entity = entityType.factory().get();
                            entity.setUuid(uuid);
                            entity.setOwner(player);
                        }
                        entity.syncFieldRegistrar().decode(buf, player.level());
                        if (firstSync) {
                            entity.init(entity.getCurrentPathNode());
                        }
                        list.add(entity);
                    }
                }
            }
            this.pendingPayload = null;
        }
    }

    public Map<Type, Map<AttachmentEntityType<?>, List<AttachmentEntity>>> getGroups() {
        return groups;
    }

    public List<AttachmentEntity> getRenderCache() {
        return renderCache;
    }

    public enum Type {
        Minion,
        Sentry,
        ExtraMinion,
        Projectile
    }
}
