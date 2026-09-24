package first.lyra.common.attachment;

import first.lyra.register.LyraAttributeRegister;
import first.lyra.register.LyraDamageRegister;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public class HealthData {

    private final LivingEntity owner;
    private float amount = 0;

    public HealthData(IAttachmentHolder holder) {
        if (holder instanceof LivingEntity living) {
            this.owner = living;
        } else {
            throw new IllegalArgumentException(holder + " is not a valid HealthData");
        }
    }

    public void tick() {
        AttributeInstance instance = owner.getAttribute(LyraAttributeRegister.HealthRegen);
        if (instance != null && instance.getValue() != 0) {
            amount += (float) (instance.getValue() / 20);
            if (owner.tickCount % 10 == 0) {
                if (amount > 1) {
                    float heal = amount - 1;
                    owner.heal(heal);
                    amount -= heal;
                } else if (amount < -1) {
                    float damage = -1 - amount;
                    // 自伤不来自 AttachmentEntity，直接走 ImmunityData：清零无敌帧后结算，不写入无敌记录
                    ImmunityData.get(owner).attack(LyraDamageRegister.getDamageSource(DamageTypes.GENERIC, owner.level()), damage);
                    amount += damage;
                }
            }
        }
    }
}
