package first.lyra.common.attachment;

import first.lyra.Lyra;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraAttributeRegister;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Lyra.MODID)
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
                    InvincibleData.attack(owner)
                            .damageAmount(damage)
                            .global()
                            .apply();
                    amount += damage;
                }
            }
        }
    }
}
