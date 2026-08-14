package first.lyra.common.minion;

import first.lyra.common.entity.PathNode;
import first.lyra.common.entity.PlannedPath;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于动量物理的仆从抽象基类。
 */
public abstract class MomentumMinion extends Minion {

    // ===================== 动量状态 =====================

    private Vec3 velocity = Vec3.ZERO;
    private float drag = 0.95f;
    private float gravity = 0;
    // ===================== 视角状态 =====================

    private float desiredYaw;
    private float desiredPitch;
    private float desiredRoll;
    private float rotationSpeed = 1f;

    public MomentumMinion() {
        super();
    }

    // ===================== 生命周期 =====================

    @Override
    public void tick() {
        if (!owner.level().isClientSide()) {
            tickOrientation();
            if (!isExecutingPath()) {
                tickPhysics();
            }
        }
        super.tick();
    }

    @Override
    public boolean isExecutingPath() {
        return super.isExecutingPath() && currentPlannedPath.getIdentifier() != "physics";
    }

    // ===================== 物理更新 =====================

    private void tickPhysics() {
        velocity = velocity.add(0, gravity, 0).scale(drag);
        Vec3 newPos = getPos().add(velocity);
        setPath(new PlannedPath("physics", Collections.singletonList(new PathNode(newPos, desiredYaw, desiredPitch, desiredRoll))));
    }

    private void tickOrientation() {
        float deltaYaw = Mth.wrapDegrees(desiredYaw - getYaw());
        float deltaPitch = Mth.wrapDegrees(desiredPitch - getPitch());
        float deltaRoll = Mth.wrapDegrees(desiredRoll - getRoll());
        if (rotationSpeed > 0) {
            desiredYaw = getYaw() + Mth.clamp(deltaYaw, -rotationSpeed, rotationSpeed);
            desiredPitch = getPitch() + Mth.clamp(deltaPitch, -rotationSpeed, rotationSpeed);
            desiredRoll = getRoll() + Mth.clamp(deltaRoll, -rotationSpeed, rotationSpeed);
        } else {
            desiredYaw = getYaw() + deltaYaw;
            desiredPitch = getPitch() + deltaPitch;
            desiredRoll = getRoll() + deltaRoll;
        }
    }

    // ===================== 视角控制 =====================

    /** 看向指定位置 */
    public void lookAtPos(Vec3 targetPos) {
        lookAtDirection(targetPos.subtract(getPos()).normalize());
    }

    /** 看向指定方向 */
    public void lookAtDirection(Vec3 direction) {
        float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float targetPitch = (float) Math.toDegrees(Math.asin(-direction.y));
        setDesiredRotation(targetYaw, targetPitch, getRoll());
    }

    /** 设置期望朝向 */
    public void setDesiredRotation(float yaw, float pitch, float roll) {
        this.desiredYaw = yaw;
        this.desiredPitch = pitch;
        this.desiredRoll = roll;
    }

    // ===================== 动量控制 =====================

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    /**
     * 施加力（累加到速度）
     */
    public void applyForce(Vec3 force) {
        velocity = velocity.add(force);
    }

    /**
     * 向目标位置方向施加力（累加到速度）
     */
    public void applyForce(Vec3 targetPos, float force) {
        Vec3 direction = targetPos.subtract(getPos());
        if (!direction.equals(Vec3.ZERO)) {
            applyForce(direction.normalize().scale(force));
        }
    }

    /** 设置速度 */
    public void setVelocity(Vec3 velocity) {
        this.velocity = velocity;
    }

    /** 瞬移到目标位置 */
    public void teleportTo(Vec3 targetPos) {
        velocity = Vec3.ZERO;
        Vec3 start = getPos();
        float yaw = getYaw(), pitch = getPitch(), roll = getRoll();
        List<PathNode> path = new ArrayList<>();
        int tick = 4;
        for (int i = 1; i <= tick; i++) {
            float t = (float) i / tick;
            Vec3 pos = start.lerp(targetPos, t);
            path.add(new PathNode(pos, yaw, pitch, roll));
        }
        setPath(path);
    }

    public Vec3 getWanderPos(Vec3 lastWanderPos, Vec3 targetPos, float distance, float height) {
        if (lastWanderPos.equals(Vec3.ZERO) || owner.getRandom().nextDouble() < 0.025 || lastWanderPos.distanceToSqr(targetPos) > distance * distance) {
            Vec3 newPos = targetPos.add(targetPos.offsetRandom(owner.getRandom(), 1).subtract(targetPos).normalize().scale(distance));
            while (newPos.y() < targetPos.y() + height) {
                newPos = newPos.add(0, 1, 0);
            }
            return newPos;
        }
        return lastWanderPos;
    }

    // ===================== 访问器 =====================

    public Vec3 getVelocity() { return velocity; }
    public void setDrag(float drag) { this.drag = Mth.clamp(drag, 0.0f, 1.0f); }
    public void setRotationSpeed(float rotationSpeed) { this.rotationSpeed = rotationSpeed; }
    public float getDesiredYaw() { return desiredYaw; }
    public float getDesiredPitch() { return desiredPitch; }
    public float getDesiredRoll() { return desiredRoll; }
}
