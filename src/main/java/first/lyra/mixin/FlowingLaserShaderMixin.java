package first.lyra.mixin;

import com.mojang.blaze3d.shaders.Uniform;
import first.lyra.client.light.FlowingLaserData;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ShaderInstance.class)
public class FlowingLaserShaderMixin {

    @Shadow @Final private int programId;
    @Shadow @Final private List<String> samplerNames;
    @Shadow @Final private Map<String, Object> samplerMap;
    @Unique private boolean lyra$flowingLaser;

    @Inject(method = "updateLocations", at = @At("HEAD"))
    private void lyra$registerFlowData(CallbackInfo ci) {
        lyra$flowingLaser = Uniform.glGetUniformLocation(programId, "LyraFlowData") >= 0;
        if (lyra$flowingLaser && !samplerNames.contains("LyraFlowData")) {
            samplerNames.add("LyraFlowData");
        }
    }

    @Inject(method = "apply", at = @At("HEAD"))
    private void lyra$bindFlowData(CallbackInfo ci) {
        if (lyra$flowingLaser) {
            samplerMap.put("LyraFlowData", FlowingLaserData.texture());
        }
    }
}
