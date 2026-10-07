package first.lyra.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import first.lyra.common.attachmentEntity.AttachmentEntityDamageSource;
import first.lyra.register.LyraAttributeRegister;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public class CombatRulesMixin {

    @WrapOperation(
            method = "getDamageAfterArmorAbsorb",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(FFF)F"
            )
    )
    private float modifyArmorValue(float damage, float totalArmor, float toughnessAttribute, Operation<Float> original, @Local(argsOnly = true) DamageSource source) {
        if (source instanceof AttachmentEntityDamageSource damageSource) {
            totalArmor -= damageSource.getArmorPierce();
            Player owner = damageSource.getOwner();
            AttributeInstance instance = owner.getAttribute(LyraAttributeRegister.SummonArmorPierce);
            if (instance != null) {
                totalArmor -= (float) instance.getValue();
            }
        }
        return original.call(damage, Math.max(totalArmor, 0), toughnessAttribute);
    }
}
