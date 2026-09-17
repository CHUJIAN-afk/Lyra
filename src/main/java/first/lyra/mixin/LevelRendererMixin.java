package first.lyra.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import first.lyra.client.dynamicLight.DynamicLightDispatcher;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @ModifyReturnValue(
            method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN")
    )
    private static int getLightColor(int original, BlockAndTintGetter level, BlockState state, BlockPos pos) {
        return DynamicLightDispatcher.getDynamicLight(level, state, pos, original);
    }
}
