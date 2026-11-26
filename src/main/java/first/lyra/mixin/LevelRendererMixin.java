package first.lyra.mixin;

import first.lyra.client.light.LyraLights;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
    private void lyra$lightOcclusion(int x, int y, int z, boolean mainThread, CallbackInfo ci) {
        LyraLights.occlusion.markDirty(x, y, z);
    }
}
