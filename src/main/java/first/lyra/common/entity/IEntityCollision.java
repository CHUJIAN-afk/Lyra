package first.lyra.common.entity;

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
public interface IEntityCollision<T extends AttachmentEntity> extends ICollision {

    /**
     * 获取用于碰撞检测的局部碰撞盒
     */
    @NotNull
    AABB getHitbox();

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
        return entity.isTarget(target);
    }

    /**
     * 当碰撞检测命中目标时调用，执行具体的攻击逻辑
     *
     * @param hitContexts 命中上下文列表，按碰撞先后顺序排序
     */
    void onCollisionAttack(List<HitContext> hitContexts);

    /**
     * 执行基于历史轨迹的精确碰撞检测，并触发攻击
     */
    default void processCollision(T entity) {
        ArrayList<PathNode> historyNodes = entity.getHistoryNodes();
        if (historyNodes.size() < 2) return;

        // 采样点：上一tick、上上tick、当前位置
        PathNode current = entity.currentPathNode;    // 当前位置
        PathNode prevTick = historyNodes.get(0);      // 上一tick
        PathNode prevPrevTick = historyNodes.get(1);  // 上上tick

        AABB localBox = getHitbox();
        Vec3 boxSize = new Vec3(localBox.getXsize(), localBox.getYsize(), localBox.getZsize());
        Vec3 boxCenterOffset = localBox.getCenter();
        boolean hasCenterOffset = boxCenterOffset.lengthSqr() > 1e-5;

        // 构建贝塞尔曲线采样 OBB 序列
        List<SampledOBB> sweepOBBs = buildSweepOBBs(prevPrevTick, prevTick, current, boxSize, boxCenterOffset, hasCenterOffset);
        if (sweepOBBs.isEmpty()) return;

        // 粗筛潜在目标：以扫掠路径覆盖的包围盒为中心，按半径从空间分组查询
        AABB sweepBounds = null;
        for (SampledOBB sampled : sweepOBBs) {
            AABB obbBox = sampled.obb.getBoundingBox();
            sweepBounds = sweepBounds == null ? obbBox : sweepBounds.minmax(obbBox);
        }
        Vec3 sweepCenter = sweepBounds.getCenter();
        double sweepRadius = sweepCenter.distanceTo(new Vec3(sweepBounds.maxX, sweepBounds.maxY, sweepBounds.maxZ));
        List<LivingEntity> potentialTargets = entity.getLevel()
                .getData(LyraAttachmentRegister.TargetCache)
                .getEntitiesInRadius(sweepCenter, sweepRadius, target -> isValidCollisionTarget(entity, target));

        // 精确碰撞检测并收集碰撞点
        Map<LivingEntity, Vec3> hitPoints = new HashMap<>();
        for (LivingEntity target : potentialTargets) {
            Vec3 hitPoint = findHitPoint(sweepOBBs, target.getBoundingBox(), prevTick.pos());
            if (hitPoint != null) {
                hitPoints.put(target, hitPoint);
            }
        }

        if (!hitPoints.isEmpty()) {
            // 按碰撞点距离上一tick位置的远近排序
            List<HitContext> hitContexts = hitPoints.entrySet().stream()
                    .sorted(Comparator.comparingDouble(e -> e.getValue().distanceToSqr(prevTick.pos())))
                    .map(e -> new HitContext(e.getKey(), e.getValue()))
                    .toList();
            onCollisionAttack(hitContexts);
        }
    }

    /**
     * 构建扫掠 OBB 序列
     * <p>
     * 采样标准：连续两个 OBB 至少 50% 重合
     * </p>
     */
    private List<SampledOBB> buildSweepOBBs(PathNode prevPrev, PathNode prev, PathNode current, Vec3 boxSize, Vec3 boxCenterOffset, boolean hasCenterOffset) {
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

        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            SampledOBB sampled = createSampledOBB(P0, controlPoint, P1, prev, current, t, boxSize, boxCenterOffset, hasCenterOffset);
            result.add(sampled);
        }

        return result;
    }

    /**
     * 创建采样的 OBB
     */
    private SampledOBB createSampledOBB(Vec3 P0, Vec3 controlPoint, Vec3 P1,
                                        PathNode prev, PathNode current, float t,
                                        Vec3 boxSize, Vec3 boxCenterOffset, boolean hasCenterOffset) {
        // 二次贝塞尔曲线插值位置: B(t) = (1-t)²P0 + 2(1-t)tC + t²P1
        double mt = 1.0 - t;
        Vec3 pos = P0.scale(mt * mt)
                .add(controlPoint.scale(2 * mt * t))
                .add(P1.scale(t * t));

        // 欧拉角插值
        float yaw = Mth.rotLerp(t, prev.yaw(), current.yaw());
        float pitch = Mth.rotLerp(t, prev.pitch(), current.pitch());
        float roll = Mth.rotLerp(t, prev.roll(), current.roll());

        // 应用碰撞盒中心偏移（考虑旋转）
        Vec3 hitCenter = pos;
        if (hasCenterOffset) {
            hitCenter = hitCenter.add(boxCenterOffset.xRot((float) Math.toRadians(-pitch))
                    .yRot((float) Math.toRadians(-yaw)));
        }

        return new SampledOBB(new OBB(hitCenter, boxSize, yaw, pitch, roll), t);
    }

    /**
     * 查找碰撞点
     *
     * @return 返回最靠近上一tick位置的碰撞点，如果没有碰撞返回 null
     */
    private Vec3 findHitPoint(List<SampledOBB> sweepOBBs, AABB targetBox, Vec3 prevPos) {
        Vec3 closestHitPoint = null;
        double closestDist = Double.MAX_VALUE;

        for (SampledOBB sampled : sweepOBBs) {
            if (sampled.obb.intersects(targetBox)) {
                // 计算 OBB 中心到目标碰撞箱的最近点
                Vec3 obbCenter = new Vec3(sampled.obb.center.x, sampled.obb.center.y, sampled.obb.center.z);
                Vec3 hitPoint = getClosestPointOnAABB(obbCenter, targetBox);

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
    private Vec3 getClosestPointOnAABB(Vec3 point, AABB box) {
        return new Vec3(
                Mth.clamp(point.x, box.minX, box.maxX),
                Mth.clamp(point.y, box.minY, box.maxY),
                Mth.clamp(point.z, box.minZ, box.maxZ)
        );
    }

    /**
     * 采样的 OBB，包含参数 t 用于追踪曲线位置
     */
    record SampledOBB(OBB obb, float t) {
    }

    /**
     * 碰撞命中上下文
     */
    record HitContext(LivingEntity entity, Vec3 hitPoint) {
    }
}
