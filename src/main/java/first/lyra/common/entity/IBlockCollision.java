package first.lyra.common.entity;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 方块碰撞接口，使用原版 Entity.collideBoundingBox 算法实现精确碰撞检测。
 *
 * @param <T> 附件实体类型
 */
public interface IBlockCollision<T extends AttachmentEntity> extends ICollision {

    /**
     * 清零碰撞轴的速度分量
     */
    static Vec3 clearVelocity(Vec3 velocity, CollisionContext context) {
        return new Vec3(
                context.collisionX() ? 0 : velocity.x,
                context.collisionY() ? 0 : velocity.y,
                context.collisionZ() ? 0 : velocity.z
        );
    }

    /**
     * 反转碰撞轴的速度分量
     */
    static Vec3 bounceVelocity(Vec3 velocity, CollisionContext context, double damping, double threshold) {
        return new Vec3(
                context.collisionX() ? (Math.abs(velocity.x) * damping < threshold ? 0 : -velocity.x * damping) : velocity.x,
                context.collisionY() ? (Math.abs(velocity.y) * damping < threshold ? 0 : -velocity.y * damping) : velocity.y,
                context.collisionZ() ? (Math.abs(velocity.z) * damping < threshold ? 0 : -velocity.z * damping) : velocity.z
        );
    }

    static Vec3 bounceVelocity(Vec3 velocity, CollisionContext context) {
        return bounceVelocity(velocity, context, 0.8, 0.001);
    }

    /**
     * 获取碰撞箱（相对于实体中心）
     */
    @NotNull
    AABB getBlockCollisionBox();

    /**
     * 是否启用碰撞检测
     */
    default boolean canCollideWithBlocks() {
        return true;
    }

    /**
     * 碰撞回调，实现类可在此处理碰撞后的速度变化
     */
    default void onBlockCollision(CollisionContext context) {
    }

    /**
     * 执行方块碰撞检测并修正位置
     * 使用原版 Entity.collideBoundingBox 算法
     */
    default void processBlockCollision(T entity) {

        ArrayList<PathNode> history = entity.getHistoryNodes();
        if (history.size() < 2) return;

        Vec3 from = history.getFirst().pos();
        Vec3 to = entity.currentPathNode.pos();
        Vec3 motion = to.subtract(from);

        if (motion.lengthSqr() < 1e-10) return;

        AABB box = getBlockCollisionBox().move(from);
        Level level = entity.getOwner().level();

        // 使用原版碰撞检测算法
        Vec3 correctedMotion = collideBoundingBox(box, motion, level);

        if (!correctedMotion.equals(motion)) {
            Vec3 correctedPos = from.add(correctedMotion);
            entity.setCurrentPathNode(getCollisionAfterPathNode(correctedPos, entity.getCurrentPathNode()));
            boolean collisionX = correctedMotion.x != motion.x;
            boolean collisionY = correctedMotion.y != motion.y;
            boolean collisionZ = correctedMotion.z != motion.z;
            // 被底部方块支撑：原运动向下且 Y 轴被碰撞截断（即落地）
            boolean bottomSupported = motion.y < 0 && collisionY;
            onBlockCollision(new CollisionContext(correctedPos, collisionX, collisionY, collisionZ, bottomSupported));
        }
    }

    /**
     * 原版碰撞检测算法 - Entity.collideBoundingBox
     */
    private Vec3 collideBoundingBox(AABB box, Vec3 motion, Level level) {
        // 收集碰撞体素
        List<VoxelShape> colliders = collectColliders(level, box.expandTowards(motion));
        return collideWithShapes(motion, box, colliders);
    }

    /**
     * 收集碰撞体素 - 使用原版 Level.getBlockCollisions
     */
    private List<VoxelShape> collectColliders(Level level, AABB searchBox) {
        List<VoxelShape> colliders = new ArrayList<>();
        for (VoxelShape shape : level.getBlockCollisions(null, searchBox)) {
            colliders.add(shape);
        }
        return colliders;
    }

    /**
     * 原版碰撞计算 - Entity.collideWithShapes
     */
    private Vec3 collideWithShapes(Vec3 motion, AABB box, List<VoxelShape> colliders) {
        if (colliders.isEmpty()) return motion;

        double dx = motion.x;
        double dy = motion.y;
        double dz = motion.z;

        // Y轴优先处理
        if (dy != 0.0) {
            dy = net.minecraft.world.phys.shapes.Shapes.collide(net.minecraft.core.Direction.Axis.Y, box, colliders, dy);
            if (dy != 0.0) {
                box = box.move(0.0, dy, 0.0);
            }
        }

        // 根据移动大小决定X/Z处理顺序
        boolean xMajor = Math.abs(dx) >= Math.abs(dz);

        if (xMajor) {
            if (dz != 0.0) {
                dz = net.minecraft.world.phys.shapes.Shapes.collide(net.minecraft.core.Direction.Axis.Z, box, colliders, dz);
                if (dz != 0.0) {
                    box = box.move(0.0, 0.0, dz);
                }
            }
            if (dx != 0.0) {
                dx = net.minecraft.world.phys.shapes.Shapes.collide(net.minecraft.core.Direction.Axis.X, box, colliders, dx);
            }
        } else {
            if (dx != 0.0) {
                dx = net.minecraft.world.phys.shapes.Shapes.collide(net.minecraft.core.Direction.Axis.X, box, colliders, dx);
                if (dx != 0.0) {
                    box = box.move(dx, 0.0, 0.0);
                }
            }
            if (dz != 0.0) {
                dz = net.minecraft.world.phys.shapes.Shapes.collide(net.minecraft.core.Direction.Axis.Z, box, colliders, dz);
            }
        }

        return new Vec3(dx, dy, dz);
    }

    /**
     * 碰撞上下文。
     *
     * @param position         碰撞修正后位置
     * @param collisionX       X 轴是否被碰撞截断
     * @param collisionY       Y 轴是否被碰撞截断
     * @param collisionZ       Z 轴是否被碰撞截断
     * @param bottomSupported  是否被底部方块支撑（原运动向下且 Y 轴被截断，即落地）
     */
    record CollisionContext(Vec3 position, boolean collisionX, boolean collisionY, boolean collisionZ, boolean bottomSupported) {
    }
}