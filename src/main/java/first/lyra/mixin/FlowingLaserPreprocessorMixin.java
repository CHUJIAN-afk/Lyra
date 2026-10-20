package first.lyra.mixin;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import first.lyra.client.light.FlowingLaserShaders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(GlslPreprocessor.class)
public class FlowingLaserPreprocessorMixin {

    @ModifyVariable(method = "process", at = @At("HEAD"), argsOnly = true)
    private String lyra$importFlowingLaser(String source) {
        return FlowingLaserShaders.importLibrary(source);
    }

    @Inject(method = "process", at = @At("RETURN"), cancellable = true)
    private void lyra$specializeLights(String source, CallbackInfoReturnable<List<String>> ci) {
        ci.setReturnValue(FlowingLaserShaders.specialize(ci.getReturnValue()));
    }
}
