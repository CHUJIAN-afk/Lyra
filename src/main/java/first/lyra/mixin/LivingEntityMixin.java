package first.lyra.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import first.lyra.common.minion.Minion;
import first.lyra.common.minion.MinionDamageSource;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraAttributeRegister;
import net.minecraft.server.level.ServerLevel;
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

    // 26.2 中 hurt(DamageSource, float) 已移除,服务端伤害入口改为 hurtServer(ServerLevel, DamageSource, float)
    @WrapMethod(method = "hurtServer")
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
        if (source instanceof MinionDamageSource minionDamageSource) {
            Minion minion = minionDamageSource.getMinion();
            Player owner = minion.getOwner();
            AttributeInstance instance = owner.getAttribute(LyraAttributeRegister.SummonDamage);
            float scale = instance != null ? (float) instance.getValue() : 1;
            amount *= scale;
            amount *= 0.85f + owner.getRandom().nextFloat() * 0.3f;
        }
        return original.call(level, source, amount);
    }

    // 26.2 中 knockback(DDD)V 已删除;伤害流程为 hurtServer -> dealDefaultKnockback -> knockback(DDD, DamageSource, float)
    @ModifyArg(
            method = "dealDefaultKnockback",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDDLnet/minecraft/world/damagesource/DamageSource;F)V"
            ),
            index = 0
    )
    private double knockback(double strength, @Local(argsOnly = true, name = "source") DamageSource source) {
        if (source instanceof MinionDamageSource minionDamageSource) {
            Minion minion = minionDamageSource.getMinion();
            Player owner = minion.getOwner();
            AttributeInstance instance = owner.getAttribute(LyraAttributeRegister.SummonKnockback);
            double scale = instance != null ? instance.getValue() : 1;
            scale *= 0.8 + (0.4 * owner.getRandom().nextDouble());
            return minion.getKnockback() * scale;
        }
        return strength;
    }
}
