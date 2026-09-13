package first.lyra.common.entity;

import first.lyra.register.LyraEntityRegister;
import first.lyra.utils.LyraStreamCodecs;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.ref.WeakReference;

public abstract class LivingAttachmentEntity extends AttachmentEntity implements IEntityCollision<LivingAttachmentEntity> {

    protected HurtEntity hurtEntity = null;
    protected boolean init = false;

    public LivingAttachmentEntity(Holder<AttachmentEntityType<?>> type) {
        super(type);
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
        }
        super.tick();
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
