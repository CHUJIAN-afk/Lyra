package first.lyra.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import first.lyra.client.dynamicLight.DynamicLightDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @WrapMethod(method = "getPackedLightCoords")
    private int getPackedLightCoords(Entity entity, float partialTickTime, Operation<Integer> original) {
        return DynamicLightDispatcher.getDynamicLight(entity.getLightProbePosition(partialTickTime), original.call(entity, partialTickTime));
    }
}
