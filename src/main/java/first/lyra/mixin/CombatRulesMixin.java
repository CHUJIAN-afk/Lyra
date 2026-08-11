package first.lyra.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
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

    @WrapMethod(method = "getDamageAfterAbsorb")
    private static float modifyArmorValue(LivingEntity victim, float damage, DamageSource source, float totalArmor, float armorToughness, Operation<Float> original) {
        if (source instanceof ServantDamageSource servantDamageSource) {
            Servant servant = servantDamageSource.getServant();
            totalArmor -= servant.getArmorPierce();
            Player owner = servant.getOwner();
            AttributeInstance instance = owner.getAttribute(LyraAttributeRegister.ServantArmorPierce);
            if (instance != null) {
                totalArmor -= (float) instance.getValue();
            }
        }
        return original.call(victim, damage, source, Math.max(totalArmor, 0), armorToughness);
    }
}
