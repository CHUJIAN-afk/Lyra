package first.lyra.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.entity.AttachmentEntityDamageSource;
import first.lyra.mixinHandler.MixinHandler;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraAttributeRegister;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(
            method = "tick",
            at = @At("TAIL")
    )
    private void tick(CallbackInfo ci) {
        LivingEntity living = (LivingEntity) (Object) this;
        living.getData(LyraAttachmentRegister.InvincibleData).tick();
    }

    @WrapMethod(method = "hurt")
    public boolean hurt(DamageSource source, float amount, Operation<Boolean> original) {
        if (source instanceof AttachmentEntityDamageSource attachmentEntityDamageSource) {
            AttachmentEntity minion = attachmentEntityDamageSource.getAttachmentEntity();
            Player owner = minion.getOwner();
            amount = MixinHandler.getModifyDamage((LivingEntity) (Object) this, owner, minion, amount, attachmentEntityDamageSource);
        }
        return original.call(source, amount);
    }

    @ModifyArg(
            method = "hurt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"
            ),
            index = 0
    )
    private double knockback(double strength, @Local(argsOnly = true) DamageSource damageSource) {
        if (damageSource instanceof AttachmentEntityDamageSource AttachmentEntityDamageSource) {
            AttachmentEntity minion = AttachmentEntityDamageSource.getAttachmentEntity();
            Player owner = minion.getOwner();
            AttributeInstance instance = owner != null ? owner.getAttribute(LyraAttributeRegister.SummonKnockback) : null;
            double scale = instance != null ? instance.getValue() : 1;
            if (owner != null) {
                scale *= 0.8 + (0.4 * owner.getRandom().nextDouble());
            }
            return minion.getKnockback() * scale;
        }
        return strength;
    }
}
