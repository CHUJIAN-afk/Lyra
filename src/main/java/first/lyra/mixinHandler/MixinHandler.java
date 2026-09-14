package first.lyra.mixinHandler;

import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.AttachmentEntityDamageSource;
import first.lyra.register.LyraAttributeRegister;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class MixinHandler {

    public static float getModifyDamage(LivingEntity living, @Nullable LivingEntity summoner, AttachmentEntity minion, float damage, AttachmentEntityDamageSource source) {
        if (summoner != null) {
            AttributeInstance instance = summoner.getAttribute(LyraAttributeRegister.SummonDamage);
            damage *= instance != null ? (float) instance.getValue() : 1;
            damage *= 0.85f + summoner.getRandom().nextFloat() * 0.3f;
        }
        return damage;
    }

    public static float getModifyArmorPierce(LivingEntity living, @Nullable LivingEntity summoner, AttachmentEntity minion, float totalArmor, AttachmentEntityDamageSource source) {
        totalArmor -= minion.getArmorPierce();
        AttributeInstance instance = summoner != null ? summoner.getAttribute(LyraAttributeRegister.SummonArmorPierce) : null;
        if (instance != null) {
            totalArmor -= (float) instance.getValue();
        }
        return totalArmor;
    }
}
