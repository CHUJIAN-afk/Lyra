package first.lyra.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import first.lyra.common.attachmentEntity.AttachmentEntityDamageSource;
import first.lyra.common.minion.Minion;
import first.lyra.mixinHandler.MixinHandler;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
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
            if (damageSource.getAttachmentEntity() instanceof Minion minion) {
                LivingEntity owner = minion.getOwner();
                totalArmor = MixinHandler.getModifyArmorPierce(LivingEntity.class.cast(this), owner, minion, totalArmor, damageSource);
            }
        }
        return original.call(damage, totalArmor, toughnessAttribute);
    }
}
