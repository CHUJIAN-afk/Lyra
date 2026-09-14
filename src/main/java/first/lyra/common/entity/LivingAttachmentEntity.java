package first.lyra.common.entity;

import first.lyra.common.minion.MinionGoalSelector;
import first.lyra.register.LyraEntityRegister;
import first.lyra.utils.LyraStreamCodecs;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class LivingAttachmentEntity extends AttachmentEntity implements IEntityCollision<LivingAttachmentEntity>, IBlockCollision<LivingAttachmentEntity> {

    protected final MinionGoalSelector goalSelector = new MinionGoalSelector();
    protected HurtEntity hurtEntity = null;
    protected boolean init = false;
    private Vec3 velocity = Vec3.ZERO;
    private float drag = 0.95F;
    private float gravity = 0.0F;
    private boolean blockCollision = true;
    private boolean onGround;

    public LivingAttachmentEntity(Holder<AttachmentEntityType<?>> type) {
        super(type);
        registerGoals(goalSelector);
    }

    public void registerGoals(MinionGoalSelector goalSelector) {
    }

    @Override
    protected void registerSyncFields(SyncFieldDispatcher fields) {
        super.registerSyncFields(fields);
        fields.field(LyraStreamCodecs.INT, () -> hurtEntity != null ? hurtEntity.getId() : -1, (level, id) -> {
            hurtEntity = level.getEntity(id) instanceof HurtEntity entity ? entity : null;
            if (hurtEntity != null) {
                hurtEntity.bind(this);
            }
        });
    }

    @Override
    public void tick() {
        if (!level.isClientSide()) {
            goalSelector.tick();
            if (!isExecutingPath()) {
                tickPhysics();
            }
            if (!init) {
                init = true;
                HurtEntity entity = new HurtEntity(LyraEntityRegister.HurtEntity.get(), level);
                if (level.addFreshEntity(entity)) {
                    entity.bind(this);
                    hurtEntity = entity;
                }
            }
            if (hurtEntity == null || !hurtEntity.isAlive()) {
                setRemove();
            }
            setOnGround(false);
        }
        super.tick();
    }

    @Override
    public boolean isExecutingPath() {
        return super.isExecutingPath() && !Objects.equals(currentPlannedPath.getIdentifier(), "physics");
    }

    // ===================== 物理更新 =====================

    private void tickPhysics() {
        velocity = velocity.add(0, gravity, 0).scale(drag);
        Vec3 newPos = getPos().add(velocity);
        setPath(new PlannedPath("physics", Collections.singletonList(new PathNode(newPos, getYaw(), getPitch(), getRoll()))));
    }

    @Override
    public @NotNull AABB getBlockCollisionBox() {
        return getHitbox();
    }

    @Override
    public boolean canCollideWithBlocks() {
        return blockCollision;
    }

    @Override
    public void onBlockCollision(CollisionContext context) {
        setOnGround(context.bottomSupported());
        setVelocity(IBlockCollision.clearVelocity(getVelocity(), context));
    }

    public AABB getAbsoluteHitbox() {
        AABB localHitbox = getHitbox();
        Vec3 localCenter = localHitbox.getCenter();
        Vec3 size = new Vec3(localHitbox.getXsize(), localHitbox.getYsize(), localHitbox.getZsize());
        Quaternionf rotation = new Quaternionf()
                .rotateY((float) Math.toRadians(-getYaw()))
                .rotateX((float) Math.toRadians(getPitch()))
                .rotateZ((float) Math.toRadians(getRoll()));
        Vector3f rotatedCenter = new Vector3f((float) localCenter.x, (float) localCenter.y, (float) localCenter.z).rotate(rotation);
        Vec3 absoluteCenter = getPos().add(rotatedCenter.x(), rotatedCenter.y(), rotatedCenter.z());
        return new OBB(absoluteCenter, size, getYaw(), getPitch(), getRoll()).getBoundingBox();
    }

    @Override
    public void onRemove() {
        if (hurtEntity != null && !hurtEntity.isRemoved()) {
            hurtEntity.discard();
        }
    }

    protected boolean onHurtEntityDeath(DamageSource damageSource) {
        return false;
    }

    public HurtEntity getHurtEntity() {
        return hurtEntity;
    }

    public float getAttackDamage() {
        return hurtEntity == null ? 0.0F : (float) hurtEntity.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public float getHealth() {
        return hurtEntity == null ? 0.0F : hurtEntity.getHealth();
    }

    public float getMaxHealth() {
        return hurtEntity == null ? 0.0F : hurtEntity.getMaxHealth();
    }

    public void setHealth(float health) {
        if (hurtEntity != null) {
            hurtEntity.setHealth(health);
        }
    }

    public void heal(float amount) {
        if (hurtEntity != null) {
            hurtEntity.heal(amount);
        }
    }

    public boolean onGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public double getBbHeight() {
        return getHitbox().getYsize();
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public float getCustomGravity() {
        return gravity;
    }

    public void setDrag(float drag) {
        this.drag = Mth.clamp(drag, 0.0F, 1.0F);
    }

    public float getDrag() {
        return drag;
    }

    public void setBlockCollision(boolean blockCollision) {
        this.blockCollision = blockCollision;
    }

    public boolean hasBlockCollision() {
        return blockCollision;
    }

    public void setVelocity(Vec3 velocity) {
        this.velocity = velocity == null ? Vec3.ZERO : velocity;
    }

    public Vec3 getVelocity() {
        return velocity;
    }

    public void applyForce(Vec3 force) {
        if (force != null) {
            this.velocity = this.velocity.add(force);
        }
    }

    public void applyForce(Vec3 targetPos, float force) {
        Vec3 direction = targetPos.subtract(getPos());
        applyForce(direction.normalize().scale(force));
    }

    public void setPathRotation(float yaw, float pitch, float roll) {
        currentPathNode = new PathNode(currentPathNode.pos(), yaw, pitch, roll);
    }

    public void lookAtPathTarget(Vec3 targetPos) {
        Vec3 direction = targetPos.subtract(getPos()).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) Math.toDegrees(Math.asin(-direction.y));
        setPathRotation(yaw, pitch, getRoll());
    }

    public MinionGoalSelector getGoalSelector() {
        return goalSelector;
    }

    public static class HurtEntity extends Monster {

        protected WeakReference<LivingAttachmentEntity> livingAttachmentEntity = new WeakReference<>(null);

        public HurtEntity(EntityType<? extends Monster> entityType, Level level) {
            super(entityType, level);
            setNoGravity(true);
            setNoAi(true);
            setPersistenceRequired();
        }

        public void bind(LivingAttachmentEntity livingAttachmentEntity) {
            if (!this.livingAttachmentEntity.refersTo(livingAttachmentEntity)) {
                this.livingAttachmentEntity = new WeakReference<>(livingAttachmentEntity);
            }
        }

        @Override
        public void die(@NotNull DamageSource damageSource) {
            LivingAttachmentEntity entity = livingAttachmentEntity.get();
            if (entity != null && entity.onHurtEntityDeath(damageSource)) {
                setHealth(getMaxHealth());
                return;
            }
            super.die(damageSource);
        }

        @Override
        public void tick() {
            super.tick();
            LivingAttachmentEntity entity = livingAttachmentEntity.get();
            if (entity != null) {
                this.setPos(entity.getPos());
                this.setYRot(entity.getYaw());
                this.setXRot(entity.getPitch());
                this.setBoundingBox(entity.getAbsoluteHitbox());
                this.setDeltaMovement(Vec3.ZERO);
            } else if (!level().isClientSide()) {
                discard();
            }
        }
    }
}
