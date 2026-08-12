package first.lyra.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import first.lyra.client.dynamicLight.DynamicLightDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = LightCoordsUtil.BrightnessGetter.class, priority = 900)
public interface LightCoordsUtilMixin {

    @WrapMethod(method = "lambda$static$0")
    private static int onGetBrightness(BlockAndLightGetter level, BlockPos pos, Operation<Integer> original) {
        return DynamicLightDispatcher.getDynamicLight(level, level.getBlockState(pos), pos, original.call(level, pos));
    }
}
