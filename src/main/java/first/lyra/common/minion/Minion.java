package first.lyra.common.minion;

import first.lyra.common.attachment.TargetCache;
import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.entity.AttachmentEntityType;
import first.lyra.common.entity.SyncFieldDispatcher;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.utils.LyraStreamCodecs;
import net.minecraft.core.Holder;
import net.minecraft.data.HashCache;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Objects;

public abstract class Minion extends AttachmentEntity {

    private final MinionGoalSelector goalSelector = new MinionGoalSelector();
    private LivingEntity target = null;
    private boolean targetChange = false;

    private MinionSlotType slotType = MinionSlotType.None;
    private int slotCost = 1;
    private int order = 0;
    private int sameSize = 1;

    @Override
    protected void registerSyncFields(SyncFieldDispatcher fields) {
        super.registerSyncFields(fields);
        fields.field(LyraStreamCodecs.MINION_SLOT_TYPE, this::getSlotType, this::setSlotType);
        fields.field(LyraStreamCodecs.INT, this::getSlotCost, this::setSlotCost);
        fields.field(LyraStreamCodecs.INT, this::getSameOrder, this::setOrder);
        fields.field(LyraStreamCodecs.INT, this::getSameSize, this::setSameSize);
    }

    public Minion(Holder<AttachmentEntityType<?>> type) {
        super(type);
        registerGoals(goalSelector);
    }

    public abstract int getSearchDistance();

    /**
     * 注册AI目标
     */
    public void registerGoals(MinionGoalSelector goalSelector) {
    }

    /**
     * 获取占用栏位数
     */
    public int getSlotCost() {
        return slotCost;
    }

    public void setSlotCost(int slotCost) {
        this.slotCost = slotCost;
    }

    @Override
    public void tick() {
        if (!level.isClientSide()) {
            setTargetChange(false);
            setTarget(searchTarget());
            goalSelector.tick();
            if (owner == null || !owner.isAlive()) {
                setRemove();
            }
        }
        super.tick();
    }

    /**
     * 在所有者周围搜索有效目标
     */
    public LivingEntity searchTarget() {
        int distance = this.getSearchDistance();
        if (distance > 0 && owner != null) {
            TargetCache targetCache = level.getData(LyraAttachmentRegister.TargetCache);
            List<LivingEntity> targets = targetCache.getEntitiesInRadius(owner.getBoundingBox().getCenter(), targetCache.getSummonSearchRange(getOwner(), distance), living -> targetCache.isVisibility(owner, living) && isTarget(living));
            if (!targets.isEmpty()) {
                return targetCache.getNewTarget(this, targets, 0, true);
            }
        }
        return null;
    }

    public long getSameHash() {
        return Objects.hash(this.getType()) * 43L;
    }

    public int getSameOrder() {
        return order;
    }

    public int getSameSize() {
        return sameSize;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public void setTarget(LivingEntity target) {
        if (this.target != null && this.target != target) {
            setTargetChange(true);
        }
        this.target = target;
    }

    public boolean isTargetChange() {
        return targetChange;
    }

    public void setTargetChange(boolean targetChange) {
        this.targetChange = targetChange;
    }

    public MinionGoalSelector getGoalSelector() {
        return goalSelector;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setSameSize(int sameSize) {
        this.sameSize = sameSize;
    }

    public MinionSlotType getSlotType() {
        return slotType;
    }

    public void setSlotType(MinionSlotType slotType) {
        this.slotType = slotType;
    }
}
