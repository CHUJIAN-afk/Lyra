package first.lyra.common.attachment;

import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.entity.AttachmentEntityType;
import first.lyra.common.servant.Servant;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraAttributeRegister;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;
import java.util.function.IntToDoubleFunction;

/**
 * 目标缓存，存储玩家周围的实体列表。
 * <p>
 * 每tick更新一次，所有仆从共享同一份缓存，避免重复查询。
 * 存储为玩家附件，仅服务端使用。
 * </p>
 */
public class TargetCache {

    private static final double MAX_DISTANCE_SQ = 128.0 * 128.0;
    private static final int MAX_STEPS = 128;
    private static final double EPSILON = 1.0E-7;

    /**
     * 缓存的实体列表
     */
    private final List<LivingEntity> entities = new ArrayList<>();
    private final Int2BooleanOpenHashMap visibilityCache = new Int2BooleanOpenHashMap();
    private final Int2FloatOpenHashMap distanceCache = new Int2FloatOpenHashMap();

    // ==================== Chunk 缓存（每tick预加载） ====================

    private ServerLevel cachedLevel;
    private final Long2ObjectOpenHashMap<LevelChunk> chunkCache = new Long2ObjectOpenHashMap<>();
    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

    public LivingEntity getNewTarget(Servant servant, List<LivingEntity> targets, float ownerWarningDistance, boolean selfCenter) {
        Player owner = servant.getOwner();
        LivingEntity currentTarget = servant.getTarget();
        LivingEntity newTarget = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity entity : targets) {
            double score = selfCenter ? getDistance(servant, entity) : getDistance(owner, entity);
            if (ownerWarningDistance > 0 && getDistance(owner, entity) < ownerWarningDistance) {
                score -= 10000.0;
            }
            if (entity == currentTarget) {
                score -= 1000.0;
            }
            score += ((entity.getId() * 31 + servant.hashCode() * 17) % 5) * 40;
            if (score < bestScore) {
                bestScore = score;
                newTarget = entity;
            }
        }
        return newTarget;
    }

    public float getDistance(Servant servant, LivingEntity living) {
        int key = servant.getUuid().hashCode() + living.getUUID().hashCode();
        return distanceCache.computeIfAbsent(key,  (IntToDoubleFunction)(k -> (float) servant.getPos().distanceTo(living.getBoundingBox().getCenter())));
    }

    public float getDistance(Player player, LivingEntity living) {
        int key = player.getUUID().hashCode() + living.getUUID().hashCode();
        return distanceCache.computeIfAbsent(key, (IntToDoubleFunction)(k -> (float) player.getEyePosition().distanceTo(living.getBoundingBox().getCenter())));
    }

    public float getServantSearchRange(Player player, float distance) {
        AttributeInstance instance = player.getAttribute(LyraAttributeRegister.ServantSearchRange);
        if (instance != null) {
            distance *= (float) instance.getValue();
        }
        return distance;
    }

    //此缓存不能被共享，极易卡顿
    public boolean isVisibility(Servant servant, LivingEntity living) {
        Integer key = servant.getUuid().hashCode() + living.getUUID().hashCode();
        return visibilityCache.computeIfAbsent(key, k -> hasLineOfSight(servant.getPos(), living.getBoundingBox().getCenter()));
    }

    public boolean isVisibility(Player player, LivingEntity living) {
        Integer key = player.getUUID().hashCode() + living.getUUID().hashCode();
        return visibilityCache.computeIfAbsent(key, k -> hasLineOfSight(player.getBoundingBox().getCenter(), living.getBoundingBox().getCenter()));
    }

    /**
     * DDA 光线步进视线检测，使用预加载的 chunk 缓存。
     *
     * @param from 起点坐标
     * @param to   终点坐标
     * @return true = 无遮挡（可见）
     */
    private boolean hasLineOfSight(Vec3 from, Vec3 to) {
        if (cachedLevel == null) {
            return false;
        }
        if (from.distanceToSqr(to) > MAX_DISTANCE_SQ) {
            return false;
        }

        double startX = Mth.lerp(-EPSILON, from.x, to.x);
        double startY = Mth.lerp(-EPSILON, from.y, to.y);
        double startZ = Mth.lerp(-EPSILON, from.z, to.z);
        double endX = Mth.lerp(-EPSILON, to.x, from.x);
        double endY = Mth.lerp(-EPSILON, to.y, from.y);
        double endZ = Mth.lerp(-EPSILON, to.z, from.z);

        int curX = Mth.floor(endX);
        int curY = Mth.floor(endY);
        int curZ = Mth.floor(endZ);

        double dx = startX - endX;
        double dy = startY - endY;
        double dz = startZ - endZ;

        int stepX = Mth.sign(dx);
        int stepY = Mth.sign(dy);
        int stepZ = Mth.sign(dz);

        double tDeltaX = stepX == 0 ? Double.MAX_VALUE : (double) stepX / dx;
        double tDeltaY = stepY == 0 ? Double.MAX_VALUE : (double) stepY / dy;
        double tDeltaZ = stepZ == 0 ? Double.MAX_VALUE : (double) stepZ / dz;

        double tMaxX = tDeltaX * (stepX > 0 ? 1.0 - Mth.frac(endX) : Mth.frac(endX));
        double tMaxY = tDeltaY * (stepY > 0 ? 1.0 - Mth.frac(endY) : Mth.frac(endY));
        double tMaxZ = tDeltaZ * (stepZ > 0 ? 1.0 - Mth.frac(endZ) : Mth.frac(endZ));

        ServerLevel level = cachedLevel;
        int minBuildHeight = level.getMinBuildHeight();
        int maxBuildHeight = level.getMaxBuildHeight();

        // 局部缓存：同一条射线内复用 chunk/section 引用
        long lastChunkKey = Long.MIN_VALUE;
        LevelChunk chunk = null;
        int lastSectionY = Integer.MIN_VALUE;
        LevelChunkSection section = null;

        for (int steps = 0; steps < MAX_STEPS; steps++) {
            if (tMaxX > 1.0 && tMaxY > 1.0 && tMaxZ > 1.0) {
                return true;
            }

            // 步进
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    curX += stepX;
                    tMaxX += tDeltaX;
                } else {
                    curZ += stepZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    curY += stepY;
                    tMaxY += tDeltaY;
                } else {
                    curZ += stepZ;
                    tMaxZ += tDeltaZ;
                }
            }

            // 高度裁剪
            if (curY < minBuildHeight || curY >= maxBuildHeight) {
                continue;
            }

            // 从缓存取 chunk，未命中则加载并写入缓存
            int chunkX = SectionPos.blockToSectionCoord(curX);
            int chunkZ = SectionPos.blockToSectionCoord(curZ);
            long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
            if (chunkKey != lastChunkKey) {
                chunk = chunkCache.get(chunkKey);
                if (chunk == null) {
                    chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                    if (chunk != null) {
                        chunkCache.put(chunkKey, chunk);
                    }
                }
                lastChunkKey = chunkKey;
                lastSectionY = Integer.MIN_VALUE;
            }

            if (chunk == null) {
                return false;
            }

            // 缓存 Section
            int sectionY = chunk.getSectionIndex(curY);
            if (sectionY != lastSectionY) {
                section = chunk.getSection(sectionY);
                lastSectionY = sectionY;
            }

            if (section == null || section.hasOnlyAir()) {
                continue;
            }

            BlockState blockState = section.getBlockState(curX & 15, curY & 15, curZ & 15);

            if (!blockState.canOcclude()) {
                continue;
            }

            if (blockState.isCollisionShapeFullBlock(level, mutablePos.set(curX, curY, curZ))) {
                return false;
            }

            VoxelShape voxelShape = blockState.getCollisionShape(level, mutablePos);
            if (voxelShape.isEmpty()) {
                continue;
            }

            BlockHitResult hitResult = voxelShape.clip(from, to, mutablePos);
            if (hitResult != null) {
                return false;
            }
        }

        return true;
    }

    /**
     * 更新缓存。每tick调用一次。
     *
     * @param player 玩家
     */
    public void tick(Player player) {
        entities.clear();
        visibilityCache.clear();
        distanceCache.clear();
        chunkCache.clear();
        AttachmentEntityData data = player.getData(LyraAttachmentRegister.EntityData);
        if (data.isRunning()) {
            Level level = player.level();
            if (level instanceof ServerLevel serverLevel) {
                cachedLevel = serverLevel;
            } else {
                cachedLevel = null;
                return;
            }
            AABB box = player.getBoundingBox();
            Vec3 center = box.getCenter();
            Vec3 maxVec3 = null;
            float maxDistance = 0;
            Map<AttachmentEntityData.Type, Map<AttachmentEntityType<?>, List<AttachmentEntity>>> groups = data.getGroups();
            Set<Map.Entry<AttachmentEntityData.Type, Map<AttachmentEntityType<?>, List<AttachmentEntity>>>> entries = groups.entrySet();
            for (Map.Entry<AttachmentEntityData.Type, Map<AttachmentEntityType<?>, List<AttachmentEntity>>> entry : entries) {
                Map<AttachmentEntityType<?>, List<AttachmentEntity>> entryValue = entry.getValue();
                Collection<List<AttachmentEntity>> values = entryValue.values();
                for (List<AttachmentEntity> value : values) {
                    for (AttachmentEntity entity : value) {
                        Vec3 pos = entity.getPos();
                        if (pos.distanceToSqr(center) > maxDistance) {
                            maxVec3 = pos;
                        }
                    }
                }
            }
            double distance;
            if (maxVec3 != null) {
                distance = getServantSearchRange(player, Math.max(32, (float) maxVec3.distanceTo(center) + 4));
            } else {
                distance = getServantSearchRange(player, 32);
            }
            List<LivingEntity> result = new ArrayList<>();
            List<LivingEntity> livingEntityList = level.getEntitiesOfClass(LivingEntity.class, box.inflate(distance));
            for (LivingEntity living : livingEntityList) {
                if (living != player && living.isAlive()) {
                    if (getDistance(player, living) <= distance) {
                        result.add(living);
                    }
                }
            }
            entities.addAll(result);
        }
    }

    /**
     * 获取缓存的实体列表。
     *
     * @return 实体列表（只读）
     */
    public List<LivingEntity> getEntities() {
        return entities;
    }

    /**
     * 检查缓存是否为空。
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return entities.isEmpty();
    }
}
