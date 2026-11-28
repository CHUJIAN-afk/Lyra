package first.lyra.common.attachment;

import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.minion.Minion;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraAttributeRegister;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntToDoubleFunction;
import java.util.function.Predicate;

public class TargetCache {

    private final Long2ByteOpenHashMap visibilityCache = new Long2ByteOpenHashMap();
    private final Int2BooleanOpenHashMap targetCache = new Int2BooleanOpenHashMap();
    private final Int2IntOpenHashMap hurterHistory = new Int2IntOpenHashMap();
    private final Int2FloatOpenHashMap distanceCache = new Int2FloatOpenHashMap();
    private final Int2FloatOpenHashMap assignedDamage = new Int2FloatOpenHashMap();
    private final Long2ObjectOpenHashMap<List<LivingEntity>> spatialGroups = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<LevelChunk> chunkCache = new Long2ObjectOpenHashMap<>();
    private final LineOfSight lineOfSight = new LineOfSight(this::getVisibilitySection);
    private final ServerPlayer owner;
    private ServerLevel serverLevel;
    private boolean assignmentsReady;

    public TargetCache(IAttachmentHolder owner) {
        if (owner instanceof ServerPlayer player) {
            this.owner = player;
        } else {
            throw new IllegalArgumentException(owner + " is not a valid TargetCache");
        }
    }

    public void tick() {
        this.serverLevel = owner.serverLevel();
        visibilityCache.clear();
        targetCache.clear();
        hurterHistory.replaceAll((key, value) -> value - 1);
        hurterHistory.values().removeIf(value -> value <= 0);
        distanceCache.clear();
        assignedDamage.clear();
        assignmentsReady = false;
        spatialGroups.clear();
        chunkCache.clear();
        lineOfSight.reset(serverLevel);
    }

    public void record(LivingEntity target, int time) {
        hurterHistory.put(target.getUUID().hashCode(), time);
    }

    public boolean isTarget(@Nullable LivingEntity target) {
        if (target != null && owner != target && target.isAlive()) {
            return targetCache.computeIfAbsent(target.getUUID().hashCode(), k -> {
                if (target instanceof Enemy) {
                    return true;
                }
                if (target instanceof Targeting targeting && targeting.getTarget() == owner) {
                    return true;
                }
                return hurterHistory.containsKey(target.getUUID().hashCode());
            });
        }
        return false;
    }

    private static long cellKey(int x, int y, int z) {
        return SectionPos.asLong(x, y, z);
    }

    public List<LivingEntity> getEntitiesInRadius(Vec3 pos, double radius, @Nullable Predicate<LivingEntity> filter) {
        List<LivingEntity> result = new ArrayList<>();
        if (radius <= 0 || serverLevel == null) {
            return result;
        }
        double radiusSq = radius * radius;
        int minCellX = Mth.floor(pos.x - radius) >> 4;
        int maxCellX = Mth.floor(pos.x + radius) >> 4;
        int minCellY = Mth.floor(pos.y - radius) >> 4;
        int maxCellY = Mth.floor(pos.y + radius) >> 4;
        int minCellZ = Mth.floor(pos.z - radius) >> 4;
        int maxCellZ = Mth.floor(pos.z + radius) >> 4;
        AABB searchBox = null;
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
                for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                    if (!spatialGroups.containsKey(cellKey(cellX, cellY, cellZ))) {
                        AABB cellBox = cellBox(cellX, cellY, cellZ);
                        searchBox = searchBox == null ? cellBox : searchBox.minmax(cellBox);
                    }
                }
            }
        }
        if (searchBox != null) {
            for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox)) {
                if (owner != entity && entity.isAlive()) {
                    int entityCellX = Mth.floor(entity.getX()) >> 4;
                    int entityCellY = Mth.floor(entity.getY()) >> 4;
                    int entityCellZ = Mth.floor(entity.getZ()) >> 4;
                    if (entityCellX >= minCellX && entityCellX <= maxCellX && entityCellY >= minCellY && entityCellY <= maxCellY && entityCellZ >= minCellZ && entityCellZ <= maxCellZ) {
                        List<LivingEntity> cell = spatialGroups.computeIfAbsent(cellKey(entityCellX, entityCellY, entityCellZ), key -> new ArrayList<>());
                        if (cell.stream().noneMatch(existing -> existing.getUUID().equals(entity.getUUID()))) {
                            cell.add(entity);
                        }
                    }
                }
            }
        }
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
                for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                    List<LivingEntity> cell = spatialGroups.get(cellKey(cellX, cellY, cellZ));
                    if (cell == null) {
                        continue;
                    }
                    for (LivingEntity entity : cell) {
                        if (entity.isAlive() && entity.getBoundingBox().getCenter().distanceToSqr(pos) <= radiusSq) {
                            if (filter == null || filter.test(entity)) {
                                result.add(entity);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private static AABB cellBox(int cellX, int cellY, int cellZ) {
        return new AABB(cellX << 4, cellY << 4, cellZ << 4, (cellX + 1) << 4, (cellY + 1) << 4, (cellZ + 1) << 4);
    }

    // 用已分配仆从的一次攻击伤害估算集火压力。
    public LivingEntity getNewTarget(Minion minion, List<LivingEntity> targets, float ownerWarningDistance, boolean selfCenter) {
        if (!assignmentsReady && !targets.isEmpty()) {
            assignmentsReady = true;
            for (List<AttachmentEntity> group : owner.getData(LyraAttachmentRegister.EntityData).getGroups().values()) {
                for (AttachmentEntity entity : group) {
                    if (entity instanceof Minion other && !other.isRemove() && other.isAlive()) {
                        updateAssignment(null, other.getTarget(), other.getDamage());
                    }
                }
            }
        }
        LivingEntity currentTarget = minion.getTarget();
        Vec3 ownerPos = owner.getEyePosition();
        Vec3 minionPos = minion.getPos();
        float damage = Math.max(1, minion.getDamage());
        LivingEntity newTarget = null;
        int bestPriority = -1;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity entity : targets) {
            if (!entity.isAlive() || entity == owner || entity.level() != owner.level()) {
                continue;
            }
            Vec3 center = entity.getBoundingBox().getCenter();
            double ownerDistance = ownerPos.distanceTo(center);
            int priority = isTarget(entity) ? 2 : 0;
            if (ownerDistance < ownerWarningDistance) {
                priority++;
            }
            if (priority < bestPriority) {
                continue;
            }
            double score = selfCenter ? minionPos.distanceTo(center) * 0.65 + ownerDistance * 0.35 : ownerDistance;
            float otherDamage = assignedDamage.get(entity.getId());
            if (entity == currentTarget) {
                otherDamage -= damage;
                score -= 3;
            }
            double armor = Mth.clamp(entity.getArmorValue() - minion.getArmorPierce(), 0, 20);
            double pressure = Math.max(0, otherDamage) * (1 - armor / 25) / Math.max(1, entity.getHealth() + entity.getAbsorptionAmount());
            score += 8 * pressure + 32 * Math.max(0, pressure - 0.75);
            if (priority > bestPriority || score < bestScore) {
                bestPriority = priority;
                bestScore = score;
                newTarget = entity;
            }
        }
        return newTarget;
    }

    public void updateAssignment(@Nullable LivingEntity previous, @Nullable LivingEntity next, float damage) {
        if (assignmentsReady) {
            float estimate = Math.max(1, damage);
            if (previous != null) {
                assignedDamage.addTo(previous.getId(), -estimate);
            }
            if (next != null) {
                assignedDamage.addTo(next.getId(), estimate);
            }
        }
    }

    public float getDistance(Minion minion, LivingEntity living) {
        int key = minion.getUuid().hashCode() + living.getUUID().hashCode();
        return distanceCache.computeIfAbsent(key, (IntToDoubleFunction) (k -> (float) minion.getPos().distanceTo(living.getBoundingBox().getCenter())));
    }

    public float getDistance(LivingEntity living1, LivingEntity living2) {
        int key = living1.getUUID().hashCode() + living2.getUUID().hashCode();
        return distanceCache.computeIfAbsent(key, (IntToDoubleFunction)(k -> (float) living1.getEyePosition().distanceTo(living2.getBoundingBox().getCenter())));
    }

    public float getSummonSearchRange(@Nullable Player player, float distance) {
        if (player != null) {
            AttributeInstance instance = player.getAttribute(LyraAttributeRegister.SummonSearchRange);
            if (instance != null) {
                distance *= (float) instance.getValue();
            }
        }
        return distance;
    }

    public boolean isVisibility(AttachmentEntity attachmentEntity, LivingEntity living) {
        return living.level() == serverLevel && hasLineOfSight(attachmentEntity.getPos(), living.getBoundingBox().getCenter());
    }

    public boolean isVisibility(@Nullable LivingEntity living1, @Nullable LivingEntity living2) {
        if (living1 != null && living2 != null && living1.level() == serverLevel && living2.level() == serverLevel) {
            long key = ChunkPos.asLong(living1.getId(), living2.getId());
            byte visible = visibilityCache.get(key);
            if (visible == 0) {
                visible = hasLineOfSight(living1.getEyePosition(), living2.getEyePosition()) ? (byte) 1 : (byte) -1;
                visibilityCache.put(key, visible);
            }
            return visible > 0;
        }
        return false;
    }

    private boolean hasLineOfSight(Vec3 from, Vec3 to) {
        return serverLevel != null && lineOfSight.hasLineOfSight(from, to);
    }

    private @Nullable LevelChunkSection getVisibilitySection(int x, int y, int z) {
        long key = ChunkPos.asLong(x, z);
        LevelChunk chunk = chunkCache.get(key);
        if (chunk == null && !chunkCache.containsKey(key)) {
            chunk = serverLevel.getChunkSource().getChunkNow(x, z);
            chunkCache.put(key, chunk);
        }
        return chunk == null ? null : chunk.getSection(y - chunk.getMinSection());
    }
}
