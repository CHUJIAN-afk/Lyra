package first.lyra.common.minion;

import first.lyra.api.LyraHelper;
import first.lyra.common.attachment.AttachmentEntityData;
import first.lyra.common.attachment.TargetCache;
import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.entity.AttachmentEntityType;
import first.lyra.common.entity.PathNode;
import first.lyra.common.entity.SyncFieldDispatcher;
import net.minecraft.core.Holder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.function.Supplier;

/**
 * 仆从实体抽象基类，代表由玩家拥有、AI驱动、自主行动的战斗单位。
 */
public abstract class Minion extends AttachmentEntity {

    private final MinionGoalSelector goalSelector = new MinionGoalSelector();
    private LivingEntity target = null;
    private boolean targetChange = false;
    private int slotCost = 1;
    private int order = 0;
    private int sameSize = 1;

    @Override
    protected void registerSyncFields(SyncFieldDispatcher fields) {
        super.registerSyncFields(fields);
        fields.field(ByteBufCodecs.INT, this.getTarget()::getId, (level, id) -> target = level.getEntity(id) instanceof LivingEntity living ? living : null);
        fields.field(ByteBufCodecs.BOOL, this::isTargetChange, this::setTargetChange);
        fields.field(ByteBufCodecs.INT, this::getSlotCost, this::setSlotCost);
        fields.field(ByteBufCodecs.INT, this::getOrderCache, this::setOrder);
        fields.field(ByteBufCodecs.INT, this::getSameSizeCache, this::setSameSize);
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
        if (!owner.level().isClientSide()) {
            setTargetChange(false);
            setTarget(searchTarget());
            goalSelector.tick();
        }
        super.tick();
    }

    /**
     * 在所有者周围搜索有效目标
     */
    public LivingEntity searchTarget() {
        int distance = this.getSearchDistance();
        if (distance > 0) {
            LyraHelper helper = LyraHelper.get(owner);
            TargetCache targetCache = helper.getTargetCache();
            if (!targetCache.isEmpty()) {
                List<LivingEntity> targets = targetCache.getEntitiesInRadius(getPos(), targetCache.getSummonSearchRange(this.getOwner(), distance), living -> targetCache.isVisibility(owner, living) && isTarget(living));
                if (!targets.isEmpty()) {
                    return targetCache.getNewTarget(this, targets, 0, true);
                }
            }
        }
        return null;
    }

    @Override
    public void dimensionChange() {
        init(new PathNode(owner.getBoundingBox().getCenter(), 0, 0, 0));
    }

    /**
     * 获取目标仆从在其 AttachmentEntityType 分组中的未移除顺序缓存
     */
    public int getOrderCache() {
        return order;
    }

    /**
     * 获取目标仆从在其 AttachmentEntityType 分组中的未移除顺序
     */
    public int getOrder() {
        return LyraHelper.get(owner)
                .getEntityData()
                .get(AttachmentEntityData.Type.Minion, getType())
                .indexOf(this);
    }

    /**
     * 获取目标仆从在其 AttachmentEntityType 分组中的未移除数量缓存
     */
    public int getSameSizeCache() {
        return sameSize;
    }

    /**
     * 获取目标仆从在其 AttachmentEntityType 分组中的未移除数量
     */
    public int getSameSize() {
        return LyraHelper.get(owner)
                .getEntityData()
                .get(AttachmentEntityData.Type.Minion, getType())
                .size();
    }

    public LivingEntity getTarget() { return target; }

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
}
