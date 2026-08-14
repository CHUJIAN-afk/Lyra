package first.lyra.common.minion;

import first.lyra.common.entity.IBlockCollision;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 具有地面寻路能力、动能物理与方块碰撞的仆从基类。
 * <p>
 * 通过组合 {@link PathNavigator}（A* 寻路）实现和正常实体一样的地面寻路：
 * 行走、绕障、跳过1格高障碍、追踪目标。物理与碰撞由 {@link MomentumMinion} 和
 * {@link IBlockCollision} 基类处理，本类只决定"往哪走"。
 * </p>
 * <p>
 * 该仆从无视任何伤害——子类不应处理受击逻辑。
 * </p>
 */
public abstract class PathfinderMinion extends MomentumMinion implements IBlockCollision<PathfinderMinion> {

    private final PathNavigator navigator = new PathNavigator();
    private List<BlockPos> currentPath = new ArrayList<>();
    private int pathIndex = 0;
    private Vec3 lastTargetPos = Vec3.ZERO;
    private int recomputeCooldown = 0;
    /** 跳跃冷却，防止连跳 */
    private int jumpCooldown = 0;
    /** 是否处于落地状态（用于判断可否起跳） */
    private boolean onGround = false;

    public PathfinderMinion() {
        setGravity(-0.08f);  // 略大于哨兵，更接近原版实体重力
        setDrag(0.5f);       // 地面阻力较大，避免滑行
        setRotationSpeed(12f);
    }

    @Override
    public @NotNull AABB getBlockCollisionBox() {
        // 类似玩家尺寸：宽0.6，高1.8
        return new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
    }

    @Override
    public void onBlockCollision(CollisionContext context) {
        if (context.bottomSupported()) {
            onGround = true;
            Vec3 v = getVelocity();
            // 落地清零 Y 速度，保留 XZ
            setVelocity(new Vec3(v.x(), 0, v.z()));
        } else if (context.collisionY()) {
            // 头顶撞墙也清零 Y
            Vec3 v = getVelocity();
            if (v.y() > 0) {
                setVelocity(new Vec3(v.x(), 0, v.z()));
            }
        }
    }

    /**
     * 寻路并沿路径推进。在子类的 Goal.tick() 中调用。
     * <p>
     * 目标驱动重算：目标移动超过阈值、路径耗尽、或冷却到期时重算路径。
     * 通过 {@link #applyForce} 沿当前路径节点推进；遇到上坡节点触发跳跃。
     * </p>
     *
     * @param targetPos 目标世界坐标
     */
    public void navigateTo(Vec3 targetPos) {
        if (jumpCooldown > 0) {
            jumpCooldown--;
        }

        // 重算条件
        boolean needRecompute = currentPath.isEmpty()
                || pathIndex >= currentPath.size()
                || targetPos.distanceToSqr(lastTargetPos) > 4.0
                || recomputeCooldown <= 0;
        if (needRecompute) {
            ServerLevel level = (ServerLevel) owner.level();
            currentPath = navigator.findPath(level, getPos(), targetPos, 32);
            pathIndex = 0;
            lastTargetPos = targetPos;
            recomputeCooldown = 20; // 1秒内不重算
        }
        recomputeCooldown--;

        // 沿路径推进
        if (pathIndex < currentPath.size()) {
            BlockPos nodeBlock = currentPath.get(pathIndex);
            Vec3 nodeCenter = Vec3.atCenterOf(nodeBlock).add(0, 0.5, 0);
            double distSq = getPos().distanceToSqr(nodeCenter);

            if (distSq < 0.5) {
                // 到达当前节点，前进
                pathIndex++;
            } else {
                // 朝节点方向施加水平力
                Vec3 currentPos = getPos();
                Vec3 horizontal = new Vec3(nodeCenter.x() - currentPos.x(), 0, nodeCenter.z() - currentPos.z());
                double hLen = horizontal.length();
                if (hLen > 1e-4) {
                    horizontal = horizontal.scale(1.0 / hLen);
                    applyForce(horizontal.scale(0.12));
                }

                // 朝向移动方向（仅 yaw，保持水平移动姿态）
                if (hLen > 1e-4) {
                    float yaw = (float) Math.toDegrees(Math.atan2(-horizontal.x(), horizontal.z()));
                    setDesiredRotation(yaw, getPitch(), getRoll());
                }

                // 跳跃判定：目标节点高于当前脚位 且 落地中 且 跳跃冷却结束
                if (onGround && jumpCooldown <= 0 && nodeBlock.getY() > BlockPos.containing(currentPos).getY()) {
                    applyForce(new Vec3(0, 0.45, 0)); // 起跳冲量
                    jumpCooldown = 10;
                    onGround = false;
                }
            }
        }
    }

    /**
     * 清除当前路径（目标丢失时调用，让仆从停下）。
     */
    public void clearPath() {
        currentPath.clear();
        pathIndex = 0;
    }

    /**
     * 是否还有未走完的路径。
     */
    public boolean hasPath() {
        return pathIndex < currentPath.size();
    }
}
