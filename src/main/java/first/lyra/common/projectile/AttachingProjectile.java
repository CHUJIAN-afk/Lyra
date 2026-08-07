package first.lyra.common.projectile;

import first.lyra.common.entity.PathNode;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;

/**
 * 黏着射弹抽象基类，可附着在指定位置并跟随黏着目标移动。
 * <p>
 * 通过设置黏着位置（绝对坐标）和黏着目标，射弹会保持与目标的相对位置。
 * </p>
 */
public abstract class AttachingProjectile extends Projectile {

    /**
     * 黏着位置（绝对坐标），null 表示未黏着
     */
    protected Vec3 attachedPosition;

    /**
     * 黏着目标，射弹会跟随目标移动
     */
    protected LivingEntity attachedTarget;

    /**
     * 附着时相对于目标碰撞箱中心的偏移
     */
    protected Vec3 attachedOffset;

    // ===================== 构造方法 =====================

    public AttachingProjectile() {
        super();
    }

    public AttachingProjectile(Vec3 startPos, Vec3 direction) {
        super(startPos, direction);
    }

    // ===================== 状态管理 =====================

    /**
     * 是否处于黏着状态
     */
    public boolean isAttached() {
        return attachedPosition != null;
    }

    /**
     * 黏着到指定位置
     */
    public void attachTo(Vec3 position) {
        this.attachedPosition = position;
        this.velocity = Vec3.ZERO;
        this.trailTimer = 0;
        // 如果有黏着目标，计算相对偏移
        if (attachedTarget != null) {
            this.attachedOffset = position.subtract(attachedTarget.getBoundingBox().getCenter());
        }
    }

    /**
     * 获取黏着目标
     */
    public LivingEntity getAttachedTarget() {
        return attachedTarget;
    }

    /**
     * 设置黏着目标，射弹会跟随目标移动
     */
    public void setAttachedTarget(LivingEntity target) {
        this.attachedTarget = target;
        // 如果已经附着，重新计算偏移
        if (attachedPosition != null && target != null) {
            this.attachedOffset = attachedPosition.subtract(target.getBoundingBox().getCenter());
        }
    }

    /**
     * 解除黏着
     */
    public void detach() {
        this.attachedPosition = null;
        this.attachedTarget = null;
        this.attachedOffset = null;
    }

    // ===================== 物理更新 =====================

    @Override
    protected void tickPhysics() {
        if (isAttached()) {
            // 如果有黏着目标且目标存活，更新黏着位置
            if (attachedTarget != null && attachedTarget.isAlive() && attachedOffset != null) {
                attachedPosition = attachedTarget.getBoundingBox().getCenter().add(attachedOffset);
            }
            // 保持黏着位置
            setPath(Collections.singletonList(new PathNode(attachedPosition, getYaw(), getPitch(), getRoll() + getSpinSpeed())));
        } else {
            super.tickPhysics();
        }
    }

}