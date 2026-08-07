package first.lyra.common.servant;

import first.lyra.api.LyraHelper;
import first.lyra.common.attachment.AttachmentEntityData;
import first.lyra.common.attachment.InvincibleData;
import first.lyra.common.attachment.TargetCache;
import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.entity.PathNode;
import first.lyra.register.LyraDamageRegister;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.monster.Enemy;

import java.util.ArrayList;
import java.util.List;

/**
 * 仆从实体抽象基类，代表由玩家拥有、AI驱动、自主行动的战斗单位。
 */
public abstract class Servant extends AttachmentEntity {

    // ===================== AI系统 =====================

    private final ServantGoalSelector goalSelector = new ServantGoalSelector();
    private LivingEntity target = null;
    private int slotCost = 1;
    private boolean targetChange = false;
    private int order = 0;
    private int sameSize = 1;

    public Servant() {
        super();
        registerGoals(goalSelector);
    }

    // ===================== 抽象方法 =====================

    /**
     * 注册AI目标
     */
    public void registerGoals(ServantGoalSelector goalSelector) {
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

    // ===================== 生命周期 =====================

    @Override
    public void tick() {
        if (!owner.level().isClientSide()) {
            setTargetChange(false);
            setTarget(searchTarget());
            goalSelector.tick();
        }
        super.tick();
    }

    // ===================== 目标搜索 =====================

    /**
     * 在所有者周围搜索有效目标
     */
    public LivingEntity searchTarget() {
        int distance = this.getSearchDistance();
        if (distance > 0) {
            LyraHelper helper = LyraHelper.get(owner);
            TargetCache targetCache = helper.getTargetCache();
            if (!targetCache.isEmpty()) {
                float searchRange = targetCache.getServantSearchRange(this.getOwner(), distance);
                List<LivingEntity> targets = new ArrayList<>();
                List<LivingEntity> entities = targetCache.getEntities();
                for (LivingEntity living : entities) {
                    if (targetCache.isVisibility(owner, living)) {
                        if (targetCache.getDistance(owner, living) < searchRange) {
                            if (isTarget(living)) {
                                targets.add(living);
                            }
                        }
                    }
                }
                return targetCache.getNewTarget(this, targets, 0, true);
            }
        }
        return null;
    }

    public abstract int getSearchDistance();

    /**
     * 判断生物是否为有效攻击目标
     */
    public boolean isTarget(LivingEntity target) {
        if (target != null && owner != target && target.isAlive()) {
            if (target instanceof Enemy) {
                return true;
            }
            if (target instanceof Targeting targeting && targeting.getTarget() == owner) {
                return true;
            }
            if (InvincibleData.get(target).hasAttack(owner.getUUID())) {
                return true;
            }
            if (InvincibleData.get(owner).hasAttack(target.getUUID())) {
                return true;
            }
            if (InvincibleData.get(target).hasAttack(this.getUuid())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void dimensionChange() {
        init(new PathNode(owner.getBoundingBox().getCenter(), 0, 0, 0));
    }

    // ===================== 伤害来源 =====================

    /**
     * 构造仆从专属伤害来源
     */
    public ServantDamageSource getDamageSource() {
        Holder<DamageType> holder = LyraDamageRegister.getDamageTypeHolder(LyraDamageRegister.Servant, owner.level());
        return new ServantDamageSource(holder, null, owner, getCurrentPathNode().pos(), this);
    }

    // ===================== 排序 =====================

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
                .get(AttachmentEntityData.Type.Servant, getType())
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
                .get(AttachmentEntityData.Type.Servant, getType())
                .size();
    }

    @Override
    public void writeBase(RegistryFriendlyByteBuf buf) {
        super.writeBase(buf);
        buf.writeInt(slotCost);
        buf.writeInt(order);
        buf.writeInt(sameSize);
    }

    @Override
    public void readBase(RegistryFriendlyByteBuf buf) {
        super.readBase(buf);
        slotCost = buf.readInt();
        order = buf.readInt();
        sameSize = buf.readInt();
    }

    // ===================== 目标访问器 =====================

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

    public ServantGoalSelector getGoalSelector() { return goalSelector; }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setSameSize(int sameSize) {
        this.sameSize = sameSize;
    }
}