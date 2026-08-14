package first.lyra.mixin;

import first.lyra.common.minion.Minion;
import first.lyra.common.minion.MinionDamageSource;
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
        if (damageSource instanceof MinionDamageSource MinionDamageSource) {
            Minion minion = MinionDamageSource.getMinion();
            armorValue -= minion.getArmorPierce();
            Player owner = minion.getOwner();
            AttributeInstance instance = owner.getAttribute(LyraAttributeRegister.SummonArmorPierce);
            if (instance != null) {
                armorValue -= (float) instance.getValue();
            }
        }
        return Math.max(armorValue, 0);
    }
}
