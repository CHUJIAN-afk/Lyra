package first.lyra.common.attachmentEntity;

import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 碰撞攻击接口，为附件实体提供基于历史轨迹的精确碰撞检测与攻击触发能力。
 *
 * @param <T> 附件实体类型
 */
public interface IEntityCollision<T extends AttachmentEntity> {

    /**
     * 获取用于碰撞检测的局部碰撞盒
     */
    @NotNull AABB getHitbox();

    /**
     * 当碰撞检测命中目标时调用，执行具体的攻击逻辑
     *
     * @param hitContexts 命中上下文列表，按碰撞先后顺序排序
     */
    void onCollisionAttack(List<HitContext> hitContexts);

    /**
     * 是否在客户端渲染碰撞箱
     */
    default boolean renderHitbox() {
        return true;
    }

    /**
     * 是否启用碰撞攻击检测
     */
    default boolean canCollideAttack() {
        return true;
    }

    /**
     * 判断目标是否为有效的碰撞对象
     */
    default boolean isValidCollisionTarget(T entity, LivingEntity target) {
        return true;
    }

    /**
     * 执行基于历史轨迹的精确碰撞检测，并触发攻击
     */
    default void processCollision(AttachmentEntity entity) {
        ArrayList<PathNode> historyNodes = entity.getHistoryNodes();
        if (canCollideAttack() && historyNodes.size() > 2) {

            // 采样点：上一tick、上上tick、当前位置
            PathNode current = entity.currentPathNode;    // 当前位置
            PathNode prevTick = historyNodes.get(0);      // 上一tick
            PathNode prevPrevTick = historyNodes.get(1);  // 上上tick

            AABB localBox = getHitbox();
            Vec3 boxSize = new Vec3(localBox.getXsize(), localBox.getYsize(), localBox.getZsize());
            Vec3 boxCenterOffset = localBox.getCenter();
            boolean hasCenterOffset = boxCenterOffset.lengthSqr() > 1e-5;

            Sweep sweep = buildSweep(prevPrevTick, prevTick, current, boxSize, boxCenterOffset, hasCenterOffset);
            if (sweep == null) {
                return;
            }

            List<LivingEntity> potentialTargets = findPotentialTargets(entity, sweep);
            if (potentialTargets.isEmpty()) {
                return;
            }

            // 精确碰撞检测并收集碰撞点
            Map<LivingEntity, Vec3> hitPoints = new HashMap<>();
            for (LivingEntity target : potentialTargets) {
                Vec3 hitPoint = findHitPoint(sweep, target.getBoundingBox(), prevTick.pos());
                if (hitPoint != null) {
                    hitPoints.put(target, hitPoint);
                }
            }

            if (!hitPoints.isEmpty()) {
                // 按碰撞点距离上一tick位置的远近排序
                List<HitContext> hitContexts = hitPoints.entrySet().stream().sorted(Comparator.comparingDouble(e -> e.getValue().distanceToSqr(prevTick.pos()))).map(e -> new HitContext(e.getKey(), e.getValue())).toList();
                onCollisionAttack(hitContexts);
            }
        }
    }

    /**
     * 构建扫掠 OBB 序列
     * <p>
     * 采样标准：连续两个 OBB 至少 50% 重合
     * </p>
     */
    private Sweep buildSweep(PathNode prevPrev, PathNode prev, PathNode current, Vec3 boxSize, Vec3 boxCenterOffset, boolean hasCenterOffset) {
        List<SampledOBB> result = new ArrayList<>();

        // 贝塞尔曲线控制点：使用上上tick位置作为控制点方向参考
        Vec3 P0 = prev.pos();           // 起点（上一tick）
        Vec3 P1 = current.pos();        // 终点（当前位置）
        Vec3 controlPoint = P0.add(P0.subtract(prevPrev.pos()).scale(0.5)); // 控制点

        // 计算曲线总长度估算
        double estimatedLength = P0.distanceTo(P1);
        double minDim = Math.min(Math.min(boxSize.x, boxSize.y), boxSize.z);

        // 计算需要的采样数量（确保相邻 OBB 50% 重合）
        // 重合 50% 意味着步进距离不超过 minDim 的一半
        double stepSize = minDim * 0.5;
        int steps = Math.max(2, (int) Math.ceil(estimatedLength / stepSize));

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            SampledOBB sampled = createSampledOBB(P0, controlPoint, P1, prev, current, t, boxSize, boxCenterOffset, hasCenterOffset);
            result.add(sampled);

            AABB bounds = sampled.bounds();
            minX = Math.min(minX, bounds.minX);
            minY = Math.min(minY, bounds.minY);
            minZ = Math.min(minZ, bounds.minZ);
            maxX = Math.max(maxX, bounds.maxX);
            maxY = Math.max(maxY, bounds.maxY);
            maxZ = Math.max(maxZ, bounds.maxZ);
        }

        return result.isEmpty() ? null : new Sweep(result, new AABB(minX, minY, minZ, maxX, maxY, maxZ));
    }

    @SuppressWarnings("unchecked")
    private List<LivingEntity> findPotentialTargets(AttachmentEntity entity, Sweep sweep) {
        AABB bounds = sweep.bounds();
        Vec3 center = bounds.getCenter();
        double radius = center.distanceTo(new Vec3(bounds.maxX, bounds.maxY, bounds.maxZ));
        return entity.getLevel().getData(LyraAttachmentRegister.TargetCache).getEntitiesInRadius(center, radius, target -> isValidCollisionTarget((T) entity, target));
    }

    /**
     * 创建采样的 OBB
     */
    private SampledOBB createSampledOBB(Vec3 P0, Vec3 controlPoint, Vec3 P1, PathNode prev, PathNode current, float t, Vec3 boxSize, Vec3 boxCenterOffset, boolean hasCenterOffset) {
        // 二次贝塞尔曲线插值位置: B(t) = (1-t)²P0 + 2(1-t)tC + t²P1
        double mt = 1.0 - t;
        Vec3 pos = P0.scale(mt * mt).add(controlPoint.scale(2 * mt * t)).add(P1.scale(t * t));

        // 欧拉角插值
        float yaw = Mth.rotLerp(t, prev.yaw(), current.yaw());
        float pitch = Mth.rotLerp(t, prev.pitch(), current.pitch());
        float roll = Mth.rotLerp(t, prev.roll(), current.roll());

        // 应用碰撞盒中心偏移（考虑旋转）
        Vec3 hitCenter = pos;
        if (hasCenterOffset) {
            hitCenter = hitCenter.add(boxCenterOffset.xRot((float) Math.toRadians(-pitch)).yRot((float) Math.toRadians(-yaw)));
        }

        OBB obb = new OBB(hitCenter, boxSize, yaw, pitch, roll);
        return new SampledOBB(obb, obb.getBoundingBox(), t);
    }

    /**
     * 查找碰撞点
     *
     * @return 返回最靠近上一tick位置的碰撞点，如果没有碰撞返回 null
     */
    private Vec3 findHitPoint(Sweep sweep, AABB targetBox, Vec3 prevPos) {
        Vec3 closestHitPoint = null;
        double closestDist = Double.MAX_VALUE;

        for (SampledOBB sampled : sweep.samples()) {
            if (overlaps(sampled.bounds(), targetBox) && sampled.obb().intersects(targetBox)) {
                // 计算 OBB 中心到目标碰撞箱的最近点
                Vec3 hitPoint = getClosestPointOnAABB(sampled.obb(), targetBox);

                double dist = hitPoint.distanceToSqr(prevPos);
                if (dist < closestDist) {
                    closestDist = dist;
                    closestHitPoint = hitPoint;
                }
            }
        }

        return closestHitPoint;
    }

    /**
     * 获取点到 AABB 的最近点
     */
    private Vec3 getClosestPointOnAABB(OBB obb, AABB box) {
        return new Vec3(Mth.clamp(obb.center.x, box.minX, box.maxX), Mth.clamp(obb.center.y, box.minY, box.maxY), Mth.clamp(obb.center.z, box.minZ, box.maxZ));
    }

    /**
     * 使用包含边界的比较，避免快速粗筛漏掉相切碰撞。
     */
    private static boolean overlaps(AABB first, AABB second) {
        return first.minX <= second.maxX && first.maxX >= second.minX && first.minY <= second.maxY && first.maxY >= second.minY && first.minZ <= second.maxZ && first.maxZ >= second.minZ;
    }

    /**
     * 采样的 OBB、其缓存包围盒和曲线参数
     */
    record SampledOBB(OBB obb, AABB bounds, float t) {
    }

    /**
     * 完整扫掠结果及其总包围盒
     */
    record Sweep(List<SampledOBB> samples, AABB bounds) {
    }

    /**
     * 碰撞命中上下文
     */
    record HitContext(LivingEntity entity, Vec3 hitPoint) {
    }
}
