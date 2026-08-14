package first.lyra.common.projectile;

import first.lyra.api.LyraHelper;
import first.lyra.common.attachment.AttachmentEntityData;
import first.lyra.common.minion.MinionDamageSource;
import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.entity.PathNode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collections;

/**
 * 射弹实体抽象基类，代表由玩家拥有、动量驱动的飞行攻击物。
 */
public abstract class Projectile extends AttachmentEntity {

    // ===================== 物理属性 =====================

    /**
     * 生命周期计数器
     */
    protected int life = 0;
    /**
     * 当前速度向量（格/tick）
     */
    protected Vec3 velocity = Vec3.ZERO;
    /**
     * 速度衰减系数 [0, 1]，1 = 无阻力
     */
    protected float drag = 1.0f;
    /**
     * 重力
     */
    protected float gravity = 0;
    /**
     * 最大速度上限（格/tick）
     */
    protected float maxSpeed = 2.0f;

    // ===================== 生命周期 =====================
    /**
     * 伤害来源（可为null）
     */
    protected DamageSource damageSource;
    /**
     * 最大生命周期（tick）
     */
    protected int maxLife = 200;

    /** 拖尾计时器 */
    protected int trailTimer = 0;

    // ===================== 构造方法 =====================

    public Projectile() {
        super();
    }

    public Projectile(Vec3 startPos, Vec3 direction) {
        super();
        init(new PathNode(startPos, 0, 0, 0));
        if (direction != null) {
            setVelocity(direction);
            updateRotationFromVelocity();
        }
    }

    @Nullable
    public DamageSource getDamageSource() {
        return damageSource;
    }

    // ===================== AttachmentEntity 实现 =====================

    @Override
    public int getHistoryNodesSize() {
        return 8;
    }

    @Override
    public void tick() {
        life++;
        if (!owner.level().isClientSide()) {
            tickPhysics();
            if ((life >= maxLife && maxLife > 0) || getPos().distanceToSqr(owner.position()) > getMaxDistance() * getMaxDistance()) {
                setRemove();
            }
        }
        if (trailTimer > 0) {
            trailTimer--;
        }
        super.tick();
    }

    // ===================== 物理更新 =====================

    protected void tickPhysics() {
        // 应用阻力
        velocity = velocity.scale(drag).add(0, gravity, 0);

        // 限制速度
        double speed = velocity.length();
        if (speed > maxSpeed) {
            velocity = velocity.scale(maxSpeed / speed);
        }

        // 更新朝向为速度方向
        updateRotationFromVelocity();

        // 更新位置
        Vec3 newPos = getPos().add(velocity);
        setPath(Collections.singletonList(new PathNode(newPos, getYaw(), getPitch(), getRoll() + getSpinSpeed())));
    }

    /**
     * 根据速度方向更新朝向
     */
    protected void updateRotationFromVelocity() {
        if (velocity.lengthSqr() > 1e-6) {
            Vec3 dir = velocity.normalize();
            float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
            float pitch = (float) Math.toDegrees(Math.atan2(-dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)));
            currentPathNode = new PathNode(getPos(), yaw, pitch, getRoll());
        }
    }

    @Override
    public void dimensionChange() {
        setRemove();
    }

    /**
     * 加入到玩家的射弹数据
     */
    public void join(Player owner) {
        LyraHelper.get(owner).add(AttachmentEntityData.Type.Projectile, this);
    }

    // ===================== 可重写配置 =====================

    public float getSpinSpeed() {
        return 0f;
    }

    public int getTrailDuration() { return 15; }

    public double getMaxDistance() { return 128.0; }

    // ===================== 网络序列化 =====================

    @Override
    public void writeBase(RegistryFriendlyByteBuf buf) {
        super.writeBase(buf);
        buf.writeDouble(velocity.x);
        buf.writeDouble(velocity.y);
        buf.writeDouble(velocity.z);
        buf.writeInt(trailTimer);
    }

    @Override
    public void readBase(RegistryFriendlyByteBuf buf) {
        super.readBase(buf);
        velocity = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        trailTimer = buf.readInt();
    }

    // ===================== 访问器 =====================

    public Vec3 getVelocity() {
        return velocity;
    }

    public void setVelocity(Vec3 velocity) {
        this.velocity = velocity;
        updateRotationFromVelocity();
    }

    public float getDrag() { return drag; }

    public void setDrag(float drag) { this.drag = Mth.clamp(drag, 0.0f, 1.0f); }

    public float getGravity() {
        return gravity;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public float getMaxSpeed() { return maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public void setMaxLife(int maxLife) {
        this.maxLife = maxLife;
    }

    public int getLife() {
        return life;
    }

    public void setLife(int life) {
        this.life = life;
    }

    public int getTrailTimer() {
        return trailTimer;
    }

    public void setTrailTimer(int timer) {
        this.trailTimer = timer;
    }

    public void applyForce(Vec3 force) {
        this.velocity = this.velocity.add(force);
    }

    public void setDamageSource(DamageSource damageSource) {
        this.damageSource = damageSource;
        if (damageSource instanceof MinionDamageSource MinionDamageSource) {
            AttachmentEntity entity = MinionDamageSource.getMinion();
            setDamage(entity.getDamage());
            setKnockback(entity.getKnockback());
            setArmorPierce(entity.getArmorPierce());
        }
    }
}
