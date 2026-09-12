package first.lyra.common.attachment;

import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.entity.AttachmentEntityType;
import first.lyra.common.minion.Minion;
import first.lyra.common.minion.MinionSlotType;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraAttributeRegister;
import first.lyra.register.LyraRegistries;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 统一的世界级附件实体数据附件。
 * <p>
 * 使用 AttachmentEntityType 分组存储实体。
 * 移除通过 setRemove() 标记完成，添加通过延迟队列在 tick 后统一处理。
 * </p>
 */
public class AttachmentEntityData implements AttachmentSyncHandler<AttachmentEntityData> {

    private final Map<AttachmentEntityType<?>, List<AttachmentEntity>> pendingAdd = new HashMap<>();
    private final Map<AttachmentEntityType<?>, List<AttachmentEntity>> groups = new HashMap<>();
    private final List<AttachmentEntity> renderCache = new ArrayList<>();
    private boolean changed = false;
    private final AtomicReference<List<byte[]>> pendingPayloads = new AtomicReference<>(List.of());

    public void tick(Level level) {
        if (level.isClientSide()) {
            applyPendingSync(level);
            if (!isRunning()) {
                renderCache.clear();
                return;
            }
        }
        if (isRunning()) {
            updateEntities(level);
            tickEntity(level);
            syncToClient(level);
        }
    }

    public boolean isRunning() {
        return !groups.isEmpty() || !pendingAdd.isEmpty() || changed;
    }

    private void syncToClient(Level level) {
        if (!level.isClientSide() && (!groups.isEmpty() || changed)) {
            changed = false;
            level.syncData(LyraAttachmentRegister.EntityData);
        }
    }

    private void tickEntity(Level level) {
        renderCache.clear();
        boolean clientSide = level.isClientSide();
        for (List<AttachmentEntity> list : groups.values()) {
            for (AttachmentEntity entity : list) {
                entity.setLevel(level);
                entity.tick();
                if (clientSide) {
                    renderCache.add(entity);
                }
            }
        }
    }

    private void updateEntities(Level level) {
        if (!level.isClientSide()) {
            // 将待添加队列合并到主分组
            if (!pendingAdd.isEmpty()) {
                for (Map.Entry<AttachmentEntityType<?>, List<AttachmentEntity>> entry : pendingAdd.entrySet()) {
                    List<AttachmentEntity> entities = groups.computeIfAbsent(entry.getKey(), key -> new ArrayList<>());
                    for (AttachmentEntity attachmentEntity : entry.getValue()) {
                        attachmentEntity.setLevel(level);
                        entities.add(attachmentEntity);
                    }
                }
                pendingAdd.clear();
                changed = true;
            }
            enforceMinionLimits();
            // 清理所有分组中标记移除的实体
            groups.values().removeIf(list -> {
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
            Map<Long, List<Minion>> sameCache = new HashMap<>();
            groups.values().forEach(list -> list.forEach(entity -> {
                if (entity instanceof Minion minion) {
                    sameCache.computeIfAbsent(minion.getSameHash(), k -> new ArrayList<>()).add(minion);
                }
            }));
            sameCache.values().forEach(list -> list.forEach(minion -> {
                minion.setOrder(list.indexOf(minion));
                minion.setSameSize(list.size());
            }));
        }
    }

    private void enforceMinionLimits() {
        Map<OwnerSlotKey, List<Minion>> minionsByOwner = new HashMap<>();
        for (Minion minion : get(Minion.class)) {
            Player owner = minion.getOwner();
            MinionSlotType slotType = minion.getSlotType();
            if (owner != null && slotType != MinionSlotType.None) {
                minionsByOwner.computeIfAbsent(new OwnerSlotKey(owner.getUUID(), slotType), key -> new ArrayList<>()).add(minion);
            }
        }
        for (Map.Entry<OwnerSlotKey, List<Minion>> entry : minionsByOwner.entrySet()) {
            List<Minion> minions = entry.getValue();
            Player owner = minions.getFirst().getOwner();
            if (owner != null) {
                AttributeInstance limit = owner.getAttribute(switch (entry.getKey().slotType()) {
                    case Minion -> LyraAttributeRegister.MinionMaxCount;
                    case Sentry -> LyraAttributeRegister.SentryMaxCount;
                    case None -> null;
                });
                if (limit != null) {
                    int used = minions.stream().filter(minion -> !minion.isRemove()).mapToInt(Minion::getSlotCost).sum();
                    int max = (int) limit.getValue();
                    for (Minion minion : minions) {
                        if (used <= max) {
                            break;
                        }
                        if (!minion.isRemove()) {
                            minion.setRemove();
                            used -= minion.getSlotCost();
                            changed = true;
                        }
                    }
                }
            }
        }
    }

    public void add(AttachmentEntity entity) {
        pendingAdd.computeIfAbsent(entity.getType(), key -> new ArrayList<>()).add(entity);
    }

    @SuppressWarnings("unchecked")
    public <T extends AttachmentEntity> List<T> get(AttachmentEntityType<T> attachmentEntityType) {
        List<T> result = new ArrayList<>();
        groups.getOrDefault(attachmentEntityType, new ArrayList<>())
                .stream()
                .filter(entity -> !entity.isRemove())
                .map(entity -> (T) entity)
                .forEach(result::add);
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends AttachmentEntity> List<T> get(Class<T> classType) {
        List<T> result = new ArrayList<>();
        groups.values().stream()
                .flatMap(Collection::stream)
                .filter(entity -> !entity.isRemove())
                .filter(classType::isInstance)
                .map(entity -> (T) entity)
                .forEach(result::add);
        return result;
    }

    public void remove(AttachmentEntityType<?> entityType) {
        getGroups().getOrDefault(entityType, new ArrayList<>())
                .forEach(AttachmentEntity::setRemove);
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf, AttachmentEntityData data, boolean initialSync) {
        // 写入 AttachmentEntityType → 实体列表的结构
        buf.writeVarInt(data.groups.size());
        for (Map.Entry<AttachmentEntityType<?>, List<AttachmentEntity>> entityEntry : data.groups.entrySet()) {
            buf.writeInt(LyraRegistries.ATTACHMENT_ENTITY_TYPES.getId(entityEntry.getKey()));
            List<AttachmentEntity> list = entityEntry.getValue();
            buf.writeVarInt(list.size());
            for (AttachmentEntity entity : list) {
                buf.writeUUID(entity.getUuid());
                entity.syncFieldRegistrar().encode(buf, entity.getLevel(), initialSync);
            }
        }
    }

    // ===================== 网络同步 =====================

    @Override
    public AttachmentEntityData read(@NotNull IAttachmentHolder holder, @NotNull RegistryFriendlyByteBuf buf, @Nullable AttachmentEntityData oldData) {
        AttachmentEntityData data = oldData != null ? oldData : new AttachmentEntityData();
        ByteBuf copy = buf.copy();
        byte[] payload = new byte[copy.readableBytes()];
        copy.readBytes(payload);
        if (payload.length == 0) {
            return data;
        }
        data.pendingPayloads.updateAndGet(payloads -> {
            List<byte[]> updated = new ArrayList<>(payloads.size() + 1);
            updated.addAll(payloads);
            updated.add(payload);
            return List.copyOf(updated);
        });
        return data;
    }

    /**
     * 网络包到达时只暂存载荷，在客户端 tick 起点执行真实解码。
     */
    private void applyPendingSync(Level level) {
        List<byte[]> snapshot = pendingPayloads.getAndSet(List.of());
        for (byte[] payload : snapshot) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload), level.registryAccess(), ConnectionType.NEOFORGE);
            // 保留现有实体的缓存引用
            Map<UUID, AttachmentEntity> existing = new HashMap<>();
            groups.values().forEach(list -> list.forEach(entity -> existing.put(entity.getUuid(), entity)));
            // 清空分组
            groups.clear();
            // 读取 AttachmentEntityType → 实体列表
            int typeCount = buf.readVarInt();
            for (int i = 0; i < typeCount; i++) {
                AttachmentEntityType<?> entityType = LyraRegistries.ATTACHMENT_ENTITY_TYPES.getHolder(buf.readInt()).orElseThrow().value();
                List<AttachmentEntity> list = groups.computeIfAbsent(entityType, k -> new ArrayList<>());
                int listSize = buf.readVarInt();
                for (int k = 0; k < listSize; k++) {
                    UUID uuid = buf.readUUID();
                    AttachmentEntity entity = existing.get(uuid);
                    boolean firstSync = false;
                    if (entity == null) {
                        firstSync = true;
                        entity = entityType.factory().get();
                        entity.setUuid(uuid);
                    }
                    entity.setLevel(level);
                    entity.syncFieldRegistrar().decode(buf, level);
                    if (firstSync) {
                        entity.init(entity.getCurrentPathNode());
                    }
                    list.add(entity);
                }
            }
        }
    }

    public Map<AttachmentEntityType<?>, List<AttachmentEntity>> getGroups() {
        return groups;
    }

    public List<AttachmentEntity> getRenderCache() {
        return renderCache;
    }

    private record OwnerSlotKey(UUID owner, MinionSlotType slotType) {
    }
}
