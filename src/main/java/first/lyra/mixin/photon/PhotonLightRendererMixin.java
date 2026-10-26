package first.lyra.mixin.photon;

import com.lowdragmc.photon.client.light.DynamicLight;
import com.lowdragmc.photon.client.light.DynamicLightRenderer;
import first.lyra.client.light.FlowingLaserData;
import first.lyra.client.render.WholeModelTransparency;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;

@Mixin(DynamicLightRenderer.class)
public final class PhotonLightRendererMixin {

    @Shadow @Final private static FloatBuffer DATA;
    @Shadow @Final private static int[] VOLUME_INDEX;
    @Shadow private static int lightCount;

    @Inject(method = "prepare", at = @At("HEAD"), remap = false)
    private static void lyra$beginFlowData(@Coerce Object frame, CallbackInfo ci) {
        FlowingLaserData.begin();
    }

    @Inject(method = "pack", at = @At("TAIL"), remap = false)
    private static void lyra$packFlowData(DynamicLight light, int index, boolean shadowed, @Coerce Object frame, CallbackInfo ci) {
        FlowingLaserData.pack(light, index, DATA);
    }

    @Inject(method = "uploadData", at = @At("TAIL"), remap = false)
    private static void lyra$uploadFlowData(int count, CallbackInfo ci) {
        FlowingLaserData.upload(count);
    }

    @ModifyArg(method = "uploadVolumes", at = @At(value = "INVOKE", target = "Lcom/lowdragmc/photon/client/light/DynamicLightRenderer;upload(IIIILjava/nio/FloatBuffer;)I"), index = 4, remap = false)
    private static FloatBuffer lyra$tagFlowVolumes(FloatBuffer volumes) {
        return FlowingLaserData.volumes(volumes, DATA, VOLUME_INDEX, lightCount);
    }

    @Inject(method = "onLevelRendered", at = @At("HEAD"), remap = false)
    private static void lyra$prepareModels(CallbackInfo ci) {
        WholeModelTransparency.prepareLighting();
    }

    @Inject(method = "onLevelRendered", at = @At("TAIL"), remap = false)
    private static void lyra$resolveModels(CallbackInfo ci) {
        WholeModelTransparency.finishFrame();
    }
}
