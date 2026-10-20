package first.lyra.mixin;

import com.lowdragmc.photon.client.light.DynamicLightRenderer;
import first.lyra.client.render.WholeModelTransparency;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DynamicLightRenderer.class, remap = false)
public final class PhotonLightRendererMixin {

    @Inject(method = "onLevelRendered", at = @At("HEAD"), remap = false)
    private static void lyra$prepareModels(CallbackInfo ci) {
        WholeModelTransparency.prepareLighting();
    }

    @Inject(method = "onLevelRendered", at = @At("TAIL"), remap = false)
    private static void lyra$resolveModels(CallbackInfo ci) {
        WholeModelTransparency.finishFrame();
    }
}
