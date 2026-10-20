package first.lyra.mixin;

import com.lowdragmc.photon.client.light.DynamicLight;
import first.lyra.client.light.FlowingLaserData;
import first.lyra.client.light.FlowingLaserProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DynamicLight.class, remap = false)
public class FlowingLaserLightMixin implements FlowingLaserData.Access {

    @Unique
    private FlowingLaserProfile lyra$flowingLaser;

    @Override
    public FlowingLaserProfile lyra$getFlowingLaser() {
        return lyra$flowingLaser;
    }

    @Override
    public void lyra$setFlowingLaser(FlowingLaserProfile profile) {
        lyra$flowingLaser = profile;
    }

    @Inject(method = "set", at = @At("TAIL"), remap = false)
    private void lyra$copyProfile(DynamicLight source, CallbackInfoReturnable<DynamicLight> ci) {
        lyra$flowingLaser = ((FlowingLaserData.Access) source).lyra$getFlowingLaser();
    }

    @Inject(method = {"reset", "point", "spot"}, at = @At("TAIL"), remap = false)
    private void lyra$clearProfile(CallbackInfoReturnable<DynamicLight> ci) {
        lyra$flowingLaser = null;
    }
}
