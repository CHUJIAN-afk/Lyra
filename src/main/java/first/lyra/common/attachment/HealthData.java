package first.lyra.common.attachment;

import first.lyra.register.LyraAttributeRegister;
import first.lyra.register.LyraDamageRegister;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.mesdag.portlib.attachment.IPortAttachmentHolder;

public class HealthData {

    private final LivingEntity owner;
    private float amount = 0;

    public HealthData(IPortAttachmentHolder holder) {
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
                    ImmunityData.get(owner).attack(LyraDamageRegister.getDamageSource(DamageTypes.GENERIC, owner.level()), damage);
                    amount += damage;
                }
            }
        }
    }
}
