package first.lyra.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import first.lyra.common.attachmentEntity.AttachmentEntityDamageSource;
import first.lyra.common.minion.Minion;
import first.lyra.mixinHandler.MixinHandler;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CombatRules.class)
public class CombatRulesMixin {

    @WrapMethod(method = "getDamageAfterAbsorb")
    private static float modifyArmorValue(LivingEntity victim, float damage, DamageSource source, float totalArmor, float armorToughness, Operation<Float> original) {
        if (source instanceof AttachmentEntityDamageSource damageSource) {
            if (damageSource.getAttachmentEntity() instanceof Minion minion) {
                LivingEntity owner = minion.getOwner();
                totalArmor = MixinHandler.getModifyArmorPierce(victim, owner, minion, totalArmor, damageSource);
            }
        }
        return original.call(victim, damage, source, Math.max(totalArmor, 0), armorToughness);
    }
}
