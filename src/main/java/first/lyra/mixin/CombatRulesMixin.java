package first.lyra.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import first.lyra.common.minion.Minion;
import first.lyra.common.minion.MinionDamageSource;
import first.lyra.mixinHandler.MixinHandler;
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
        if (source instanceof MinionDamageSource minionDamageSource) {
            Minion minion = minionDamageSource.getMinion();
            Player owner = minion.getOwner();
            damage = MixinHandler.getModifyDamage(victim, owner, minion, damage, minionDamageSource);
            totalArmor = MixinHandler.getModifyArmorPierce(victim, owner, minion, totalArmor, minionDamageSource);
        }
        return original.call(victim, damage, source, Math.max(totalArmor, 0), armorToughness);
    }
}
