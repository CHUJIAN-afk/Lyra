package first.lyra.common.projectile;

import first.lyra.common.attachmentEntity.*;
import first.lyra.mixin.ClientLevelAccessor;
import first.lyra.register.LyraDamageRegister;
import first.lyra.utils.LyraStreamCodecs;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public abstract class Projectile extends MomentumAttachmentEntity {

    protected LivingEntity owner = null;
    protected int maxTickCount = 200;

    public Projectile(Holder<AttachmentEntityType<?>> type) {
        super(type);
    }

    @Override
    protected void registerSyncFields(SyncFieldDispatcher fields) {
        super.registerSyncFields(fields);
        fields.field(LyraStreamCodecs.OPTIONAL_UUID, () -> Optional.ofNullable(owner).map(LivingEntity::getUUID), (level, optional) -> {
            owner = null;
            optional.ifPresent(uuid -> {
                if (((ClientLevelAccessor) level).callGetEntities().get(uuid) instanceof LivingEntity living) {
                    owner = living;
                }
            });
        });
    }

    @Override
    public @NotNull DamageSource getDamageSource() {
        return new AttachmentEntityDamageSource(LyraDamageRegister.getDamageTypeHolder(LyraDamageRegister.Summon, getLevel()), null, owner, getPos(), this);
    }

    @Override
    public void tick() {
        super.tick();
        if (getMaxTickCount() > 0 && getTickCount() >= getMaxTickCount()) {
            setRemove();
        }
    }

    @Override
    public void tickPhysics() {
        super.tickPhysics();
        if (isPhysics()) {
            Vec3 velocity = getVelocity();
            float yaw = (float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z));
            float pitch = (float) Math.toDegrees(Math.atan2(-velocity.y, Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z)));
            PathNode pathNode = getCurrentPathNode();
            setCurrentPathNode(pathNode.modifyEuler(yaw, pitch, pathNode.roll()));
        }
    }

    public void setMaxTickCount(int maxTickCount) {
        this.maxTickCount = maxTickCount;
    }

    public int getMaxTickCount() {
        return maxTickCount;
    }
}
