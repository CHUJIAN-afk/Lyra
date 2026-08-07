package first.lyra.mixin;

import first.lyra.common.servant.Servant;
import first.lyra.common.servant.ServantDamageSource;
import first.lyra.register.LyraAttributeRegister;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CombatRules.class)
public class CombatRulesMixin {

    @ModifyVariable(
            method = "getDamageAfterAbsorb",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 1
    )
    private static float modifyArmorValue(float armorValue, LivingEntity entity, float damage, DamageSource damageSource, float armorToughness) {
        if (damageSource instanceof ServantDamageSource servantDamageSource) {
            Servant servant = servantDamageSource.getServant();
            armorValue -= servant.getArmorPierce();
            Player owner = servant.getOwner();
            AttributeInstance instance = owner.getAttribute(LyraAttributeRegister.ServantArmorPierce);
            if (instance != null) {
                armorValue -= (float) instance.getValue();
            }
        }
        return Math.max(armorValue, 0);
    }
}
