package first.lyra.common.projectile;

import first.lyra.api.LyraHelper;
import first.lyra.common.attachment.AttachmentEntityData;
import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.entity.AttachmentEntityType;
import first.lyra.common.entity.PathNode;
import first.lyra.common.entity.SyncFieldDispatcher;
import first.lyra.utils.LyraStreamCodecs;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.Collections;
import java.util.function.Supplier;

/**
 * 射弹实体抽象基类，代表由玩家拥有、动量驱动的飞行攻击物。
 */
public abstract class Projectile extends AttachmentEntity {

    protected int maxLife = 200;
    protected Vec3 velocity = Vec3.ZERO;
    protected float drag = 1.0f;
    protected float gravity = 0;

    @Override
    protected void registerSyncFields(SyncFieldDispatcher fields) {
        super.registerSyncFields(fields);
        fields.field(LyraStreamCodecs.INT, this::getMaxLife, this::setMaxLife);
        fields.field(LyraStreamCodecs.VEC_3, this::getVelocity, this::setVelocity);
        fields.field(LyraStreamCodecs.FLOAT, this::getDrag, this::setDrag);
        fields.field(LyraStreamCodecs.FLOAT, this::getGravity, this::setGravity);
    }

    public Projectile(Holder<AttachmentEntityType<?>> type) {
        super(type);
    }

    @Override
    public int getHistoryNodesSize() {
        return 8;
    }

    @Override
    public void tick() {
        if (!owner.level().isClientSide()) {
            tickPhysics();
            checkAlive();
        }
        super.tick();
    }

    public void checkAlive() {
        if (maxLife > 0 && tickCount > maxLife) {
            setRemove();
        }
        if (getPos().distanceTo(owner.getBoundingBox().getCenter()) > 128) {
            setRemove();
        }
    }

    protected void tickPhysics() {
        velocity = velocity.add(0, gravity, 0).scale(drag);
        if (velocity.lengthSqr() > 1e-6) {
            Vec3 dir = velocity.normalize();
            float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
            float pitch = (float) Math.toDegrees(Math.atan2(-dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)));
            currentPathNode = new PathNode(getPos(), yaw, pitch, getRoll());
        }
        setPath(Collections.singletonList(new PathNode(getPos().add(velocity), getYaw(), getPitch(), getRoll())));
    }

    @Override
    public void dimensionChange() {
        setRemove();
    }

    public void join(Player owner) {
        LyraHelper.get(owner).add(AttachmentEntityData.Type.Projectile, this);
    }

    public Vec3 getVelocity() {
        return velocity;
    }

    public void setVelocity(Vec3 velocity) {
        this.velocity = velocity;
    }

    public float getDrag() {
        return drag;
    }

    public void setDrag(float drag) {
        this.drag = Mth.clamp(drag, 0.0f, 1.0f);
    }

    public float getGravity() {
        return gravity;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public int getMaxLife() {
        return maxLife;
    }

    public void setMaxLife(int maxLife) {
        this.maxLife = maxLife;
    }

    public void applyForce(Vec3 force) {
        this.velocity = this.velocity.add(force);
    }
}
