package first.lyra.common.attachment;

import first.lyra.Lyra;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraAttributeRegister;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Lyra.MODID)
public class HealthData {

    private float amount = 0;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void healthRegenTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide()) {
            AttributeInstance instance = living.getAttribute(LyraAttributeRegister.HealthRegen);
            if (instance != null && instance.getValue() != 0) {
                float amount = living.getData(LyraAttachmentRegister.HealthData).getAmount();
                amount += (float) (instance.getValue() / 20);
                if (living.tickCount % 10 == 0) {
                    if (amount > 1) {
                        float heal = amount - 1;
                        living.heal(heal);
                        amount -= heal;
                    } else if (amount < -1) {
                        float damage = -1 - amount;
                        InvincibleData.attack(living)
                                .damageAmount(damage)
                                .global()
                                .apply();
                        amount += damage;
                    }
                }
                living.getData(LyraAttachmentRegister.HealthData).setAmount(amount);
            }
        }
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }
}
