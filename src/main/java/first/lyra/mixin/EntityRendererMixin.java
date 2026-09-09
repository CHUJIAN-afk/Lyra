package first.lyra.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import first.lyra.client.dynamicLight.DynamicLightDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @ModifyReturnValue(method = "getPackedLightCoords", at = @At("RETURN"))
    private int getPackedLightCoords(int original, Entity entity, float partialTicks) {
        return DynamicLightDispatcher.getDynamicLight(entity.getLightProbePosition(partialTicks), original);
    }
}
