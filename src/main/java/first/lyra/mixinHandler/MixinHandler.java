package first.lyra.mixinHandler;

import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.entity.AttachmentEntityDamageSource;
import first.lyra.register.LyraAttributeRegister;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

public class MixinHandler {

    public static float getModifyDamage(LivingEntity living, Player owner, AttachmentEntity minion, float damage, AttachmentEntityDamageSource source) {
        AttributeInstance instance = owner.getAttribute(LyraAttributeRegister.SummonDamage);
        damage *= instance != null ? (float) instance.getValue() : 1;
        damage *= 0.85f + owner.getRandom().nextFloat() * 0.3f;
        return damage;
    }

    public static float getModifyArmorPierce(LivingEntity living, Player owner, AttachmentEntity minion, float totalArmor, AttachmentEntityDamageSource source) {
        totalArmor -= minion.getArmorPierce();
        AttributeInstance instance = owner.getAttribute(LyraAttributeRegister.SummonArmorPierce);
        if (instance != null) {
            totalArmor -= (float) instance.getValue();
        }
        return totalArmor;
    }
}
