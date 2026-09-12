package first.lyra.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import first.lyra.common.damageInfo.IDamageSourceCritical;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public class PlayerMixin {

    @WrapOperation(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/event/entity/player/CriticalHitEvent;isCriticalHit()Z",
                    ordinal = 0
            )
    )
    private boolean isCriticalHit(CriticalHitEvent instance, Operation<Boolean> original, @Local DamageSource damageSource) {
        Boolean call = original.call(instance);
        if (damageSource instanceof IDamageSourceCritical iDamageSourceCritical) {
            iDamageSourceCritical.lyra$setCritical(call);
        }
        return call;
    }

}
