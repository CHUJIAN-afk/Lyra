package first.lyra.common.attachment;

import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.AttachmentEntityType;
import first.lyra.common.attachmentEntity.PathNode;
import first.lyra.common.minion.Minion;
import first.lyra.common.minion.MinionSlotType;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraRegistries;
import first.lyra.utils.LyraStreamCodecs;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class AttachmentEntityData {

    private final Map<AttachmentEntityType<?>, List<AttachmentEntity>> pendingAdd = new HashMap<>();
    private final Map<AttachmentEntityType<?>, List<AttachmentEntity>> groups = new HashMap<>();
    private final Map<UUID, AttachmentEntity> uuidToEntity = new HashMap<>();
    private final List<AttachmentEntity> renderCache = new ArrayList<>();
    private final Map<MinionSlotType, Integer> slotLimits = new EnumMap<>(MinionSlotType.class);
    private final Object pendingPayloadLock = new Object();
    private final Player owner;
    private List<byte[]> pendingPayloads;
    private Level level = null;
    private boolean changed = false;
    private boolean slotCostChanged = false;

    public AttachmentEntityData(IAttachmentHolder owner) {
        if (owner instanceof Player player) {
            this.owner = player;
        } else {
            throw new IllegalArgumentException(owner + " is not a valid AttachmentEntityData");
        }
    }

    public void tick() {
        uuidToEntity.clear();
        for (List<AttachmentEntity> list : groups.values()) {
            for (AttachmentEntity entity : list) {
                uuidToEntity.put(entity.getUuid(), entity);
            }
        }
        if (isClientSide()) {
            applyPendingSync();
            renderCache.clear();
            if (isRunning()) {
                for (List<AttachmentEntity> list : groups.values()) {
                    for (AttachmentEntity entity : list) {
                        entity.tickCurrentPathNode();
                        renderCache.add(entity);
                    }
                }
            }
        } else {
            if (isRunning()) {
                boolean levelChange = level != null && level != owner.level();
                level = owner.level();
                Long2ObjectOpenHashMap<List<Minion>> sameCache = null;
                // tick实体
                for (List<AttachmentEntity> list : groups.values()) {
                    for (AttachmentEntity entity : list) {
                        if (entity instanceof Minion minion) {
                            if (sameCache == null) {
                                sameCache = new Long2ObjectOpenHashMap<>();
                            }
                            List<Minion> minionList = sameCache.computeIfAbsent(minion.getSameHash(), key -> new ArrayList<>());
                            minion.setOrder(minionList.size());
                            minionList.add(minion);
                            for (int i = 0; i < minionList.size(); i++) {
                                minionList.get(i).setSameSize(list.size());
                            }
                        }
                        if (levelChange) {
                            entity.onLevelChange();
                        }
                        entity.tick();
                        entity.tickCurrentPathNode();
                    }
                }
                // 将待添加队列合并到主分组
                if (!pendingAdd.isEmpty()) {
                    for (Map.Entry<AttachmentEntityType<?>, List<AttachmentEntity>> entry : pendingAdd.entrySet()) {
                        List<AttachmentEntity> entities = groups.computeIfAbsent(entry.getKey(), key -> new ArrayList<>());
                        for (AttachmentEntity attachmentEntity : entry.getValue()) {
                            if (!attachmentEntity.getCurrentPathNode().pos().equals(Vec3.ZERO)) {
                                attachmentEntity.setOwner(owner);
                                entities.add(attachmentEntity);
                                uuidToEntity.put(attachmentEntity.getUuid(), attachmentEntity);
                                if (attachmentEntity instanceof Minion) {
                                    slotCostChanged = true;
                                }
                            }
                        }
                    }
                    pendingAdd.clear();
                    changed = true;
                }
                boolean checkSlots = slotCostChanged;
                for (MinionSlotType type : MinionSlotType.values()) {
                    int limit = type.getLimit(owner);
                    Integer previous = slotLimits.put(type, limit);
                    if (previous != null && limit < previous) {
                        checkSlots = true;
                    }
                }
                if (checkSlots) {
                    slotCostChanged = false;
                    Map<MinionSlotType, Integer> limits = new EnumMap<>(slotLimits);
                    for (Minion minion : get(Minion.class)) {
                        int limit = limits.get(minion.getSlotType());
                        int slotCost = minion.getSlotCost();
                        if (slotCost > 0) {
                            if (limit >= slotCost) {
                                limits.put(minion.getSlotType(), limit - slotCost);
                            } else {
                                minion.setRemove();
                            }
                        }
                    }
                }
                // 清理所有分组中标记移除的实体
                Predicate<AttachmentEntity> removeEntity = entity -> {
                    if (!entity.isAlive()) {
                        entity.setRemove();
                    }
                    if (entity.isRemove()) {
                        uuidToEntity.remove(entity.getUuid());
                        entity.onRemove();
                        changed = true;
                        return true;
                    }
                    return false;
                };
                groups.values().removeIf(list -> {
                    boolean repeat;
                    do {
                        list.removeIf(removeEntity);
                        if (list.isEmpty()) {
                            break;
                        }
                        repeat = false;
                        for (AttachmentEntity entity : list) {
                            if (entity.isRemove()) {
                                repeat = true;
                                break;
                            }
                        }
                    } while (repeat);
                    return list.isEmpty();
                });
                if (!groups.isEmpty() || changed) {
                    changed = false;
                    owner.syncData(LyraAttachmentRegister.EntityData);
                }
            }
        }
    }

    public @Nullable AttachmentEntity getEntity(UUID uuid) {
        return uuidToEntity.get(uuid);
    }

    public Level getLevel() {
        return owner.level();
    }

    public boolean isClientSide() {
        return owner.level().isClientSide();
    }

    public boolean isRunning() {
        if (isClientSide()) {
            return !groups.isEmpty();
        } else {
            return !groups.isEmpty() || !pendingAdd.isEmpty() || changed;
        }
    }

    public void add(AttachmentEntity entity) {
        pendingAdd.computeIfAbsent(entity.getType(), key -> new ArrayList<>()).add(entity);
    }

    public void markSlotCostChanged(Minion minion) {
        if (uuidToEntity.get(minion.getUuid()) == minion && !minion.isRemove()) {
            slotCostChanged = true;
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends AttachmentEntity> List<T> get(AttachmentEntityType<T> attachmentEntityType) {
        List<T> result = new ArrayList<>();
        for (AttachmentEntity entity : groups.getOrDefault(attachmentEntityType, Collections.emptyList())) {
            if (!entity.isRemove()) {
                result.add((T) entity);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends AttachmentEntity> List<T> get(Class<T> classType) {
        Objects.requireNonNull(classType);
        List<T> result = new ArrayList<>();
        for (List<AttachmentEntity> list : groups.values()) {
            for (AttachmentEntity entity : list) {
                if (!entity.isRemove() && classType.isInstance(entity)) {
                    result.add((T) entity);
                }
            }
        }
        return result;
    }

    public void remove(AttachmentEntityType<?> entityType) {
        getGroups().getOrDefault(entityType, Collections.emptyList()).forEach(AttachmentEntity::setRemove);
    }

    public Map<AttachmentEntityType<?>, List<AttachmentEntity>> getGroups() {
        return groups;
    }

    public List<AttachmentEntity> getRenderCache() {
        return renderCache;
    }

    // 在客户端 tick 起点按接收顺序解码同步载荷。
    private void applyPendingSync() {
        Level level = getLevel();
        List<byte[]> snapshot;
        synchronized (pendingPayloadLock) {
            snapshot = pendingPayloads == null ? List.of() : pendingPayloads;
            pendingPayloads = null;
        }
        Map<UUID, AttachmentEntity> existing = null;
        for (byte[] payload : snapshot) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload), level.registryAccess(), ConnectionType.NEOFORGE);
            try {
                if (existing == null) {
                    existing = new HashMap<>();
                } else {
                    existing.clear();
                }
                for (List<AttachmentEntity> list : groups.values()) {
                    for (AttachmentEntity entity : list) {
                        existing.put(entity.getUuid(), entity);
                    }
                }
                groups.clear();
                int typeCount = buf.readVarInt();
                for (int i = 0; i < typeCount; i++) {
                    AttachmentEntityType<?> entityType = LyraRegistries.ATTACHMENT_ENTITY_TYPES.getHolder(buf.readInt()).orElseThrow().value();
                    List<AttachmentEntity> list = groups.computeIfAbsent(entityType, k -> new ArrayList<>());
                    int listSize = buf.readVarInt();
                    for (int k = 0; k < listSize; k++) {
                        UUID uuid = buf.readUUID();
                        AttachmentEntity entity = existing.get(uuid);
                        if (entity == null) {
                            entity = entityType.factory().get();
                            entity.setUuid(uuid);
                        }
                        entity.setOwner(owner);
                        entity.getSyncFieldDispatcher().decode(buf, level);
                        if (!buf.readBoolean()) {
                            PathNode pathNode = LyraStreamCodecs.PATH_NODE.decode(buf);
                            ArrayList<PathNode> historyNodes = entity.getHistoryNodes();
                            historyNodes.clear();
                            historyNodes.add(pathNode);
                            historyNodes.add(pathNode);
                        }
                        list.add(entity);
                    }
                }
            } finally {
                buf.release();
            }
        }
    }

    // ===================== 网络同步 =====================
    public static final class SyncHandler implements AttachmentSyncHandler<AttachmentEntityData> {

        @Override
        public void write(RegistryFriendlyByteBuf buf, AttachmentEntityData data, boolean initialSync) {
            // NeoForge 初始同步会将多个附件写入同一缓冲区，必须限定本附件的载荷边界。
            int lengthIndex = buf.writerIndex();
            buf.writeInt(0);
            int start = buf.writerIndex();
            buf.writeVarInt(data.groups.size());
            for (Map.Entry<AttachmentEntityType<?>, List<AttachmentEntity>> entityEntry : data.groups.entrySet()) {
                buf.writeInt(LyraRegistries.ATTACHMENT_ENTITY_TYPES.getId(entityEntry.getKey()));
                List<AttachmentEntity> list = entityEntry.getValue();
                buf.writeVarInt(list.size());
                for (AttachmentEntity entity : list) {
                    buf.writeUUID(entity.getUuid());
                    entity.getSyncFieldDispatcher().encode(buf, entity.getLevel(), initialSync);
                    boolean clientInit = !initialSync && entity.isClientInit();
                    buf.writeBoolean(clientInit);
                    if (!clientInit) {
                        if (!initialSync) {
                            entity.setClientInit(true);
                        }
                        LyraStreamCodecs.PATH_NODE.encode(buf, entity.getHistoryNodes().getFirst());
                    }
                }
            }
            buf.setInt(lengthIndex, buf.writerIndex() - start);
        }

        @Override
        public AttachmentEntityData read(@NotNull IAttachmentHolder holder, @NotNull RegistryFriendlyByteBuf buf, @Nullable AttachmentEntityData oldData) {
            AttachmentEntityData data = oldData != null ? oldData : new AttachmentEntityData(holder);
            byte[] payload = new byte[buf.readInt()];
            buf.readBytes(payload);
            if (payload.length != 0) {
                synchronized (data.pendingPayloadLock) {
                    if (data.pendingPayloads == null) {
                        data.pendingPayloads = new ArrayList<>();
                    }
                    data.pendingPayloads.add(payload);
                }
            }
            return data;
        }
    }
}
