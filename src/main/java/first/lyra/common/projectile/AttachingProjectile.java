package first.lyra.common.projectile;

import first.lyra.common.entity.AttachmentEntityType;
import first.lyra.common.entity.PathNode;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.function.Supplier;

public abstract class AttachingProjectile extends Projectile {

    protected Vec3 attachedPosition;
    protected LivingEntity attachedTarget;
    protected Vec3 attachedOffset;

    public AttachingProjectile(Holder<AttachmentEntityType<?>> type) {
        super(type);
    }

    public boolean isAttached() {
        return attachedPosition != null;
    }

    public void attachTo(Vec3 position) {
        this.attachedPosition = position;
        this.velocity = Vec3.ZERO;
        if (attachedTarget != null) {
            this.attachedOffset = position.subtract(attachedTarget.getBoundingBox().getCenter());
        }
    }

    public LivingEntity getAttachedTarget() {
        return attachedTarget;
    }

    public void setAttachedTarget(LivingEntity target) {
        this.attachedTarget = target;
        if (attachedPosition != null && target != null) {
            this.attachedOffset = attachedPosition.subtract(target.getBoundingBox().getCenter());
        }
    }

    public void detach() {
        this.attachedPosition = null;
        this.attachedTarget = null;
        this.attachedOffset = null;
    }

    @Override
    protected void tickPhysics() {
        if (isAttached()) {
            if (attachedTarget != null && attachedTarget.isAlive() && attachedOffset != null) {
                attachedPosition = attachedTarget.getBoundingBox().getCenter().add(attachedOffset);
            }
            setPath(Collections.singletonList(new PathNode(attachedPosition, getYaw(), getPitch(), getRoll())));
        } else {
            super.tickPhysics();
        }
    }
}