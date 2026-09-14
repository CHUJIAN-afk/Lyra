package first.lyra.common.minion;

import first.lyra.common.attachment.InvincibleData;
import first.lyra.common.attachment.TargetCache;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.AttachmentEntityGoalSelector;
import first.lyra.common.attachmentEntity.AttachmentEntityType;
import first.lyra.common.attachmentEntity.SyncFieldDispatcher;
import first.lyra.mixin.ClientLevelAccessor;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.utils.LyraStreamCodecs;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class Minion extends AttachmentEntity {

    protected LivingEntity owner = null;
    protected LivingEntity target = null;
    protected MinionSlotType slotType = MinionSlotType.None;
    protected int slotCost = 1;
    protected int order = 0;
    protected int sameSize = 1;

    public Minion(Holder<AttachmentEntityType<?>> type) {
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
        fields.field(LyraStreamCodecs.INT, () -> target != null ? target.getId() : -1, (level, id) -> target = level.getEntity(id) instanceof LivingEntity living ? living : null);
        fields.field(LyraStreamCodecs.MINION_SLOT_TYPE, this::getSlotType, this::setSlotType);
        fields.field(LyraStreamCodecs.INT, this::getSlotCost, this::setSlotCost);
        fields.field(LyraStreamCodecs.INT, this::getOrder, this::setOrder);
        fields.field(LyraStreamCodecs.INT, this::getSameSize, this::setSameSize);
    }

    @Override
    public @NotNull DamageSource getDamageSource() {
        return damageSourceProvider.getDamageSource(level, owner);
    }

    public abstract int getSearchDistance();

    public long getSameHash() {
        return Objects.hash(this.getType()) * 43L;
    }

    public int getSlotCost() {
        return slotCost;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setSameSize(int sameSize) {
        this.sameSize = sameSize;
    }

    public void setSlotCost(int slotCost) {
        this.slotCost = slotCost;
    }

    public int getSameSize() {
        return sameSize;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide()) {
            setTarget(searchTarget());
            goalSelector.tick();
            if (owner == null || !owner.isAlive()) {
                setRemove();
            }
        }
    }

    /**
     * 在所有者周围搜索有效目标
     */
    public LivingEntity searchTarget() {
        int distance = this.getSearchDistance();
        if (distance > 0 && owner != null) {
            TargetCache targetCache = level.getData(LyraAttachmentRegister.TargetCache);
            List<LivingEntity> targets = targetCache.getEntitiesInRadius(owner.getBoundingBox().getCenter(), distance, living -> targetCache.isVisibility(owner, living) && isTarget(living));
            if (!targets.isEmpty()) {
                return targetCache.getNewTarget(this, targets, 0, true);
            }
        }
        return null;
    }

    public boolean isTarget(LivingEntity target) {
        if (owner != target && target != null && target.isAlive()) {
            if (owner instanceof Player && target instanceof Enemy) {
                return true;
            }
            if (owner instanceof Targeting targeting && targeting.getTarget() == target) {
                return true;
            }
            if (target instanceof Targeting targeting && targeting.getTarget() == owner) {
                return true;
            }
            if (owner != null) {
                if (InvincibleData.get(target).hasAttack(owner.getUUID())) {
                    return true;
                }
                if (InvincibleData.get(owner).hasAttack(target.getUUID())) {
                    return true;
                }
            }
        }
        return false;
    }

    public @Nullable LivingEntity getOwner() {
        return owner;
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    public AttachmentEntityGoalSelector getGoalSelector() {
        return goalSelector;
    }

    public MinionSlotType getSlotType() {
        return slotType;
    }

    public void setSlotType(MinionSlotType slotType) {
        this.slotType = slotType;
    }
}
