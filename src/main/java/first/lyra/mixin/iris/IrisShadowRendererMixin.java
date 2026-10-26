package first.lyra.mixin.iris;

import com.mojang.blaze3d.vertex.PoseStack;
import first.lyra.client.render.AttachmentEntityRenderDispatcher;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShadowRenderer.class)
public class IrisShadowRendererMixin {

    @Inject(method = "renderEntities", at = @At("RETURN"), remap = false)
    private void lyra$renderShadows(@Coerce Object levelRenderer, EntityRenderDispatcher dispatcher, MultiBufferSource.BufferSource bufferSource, PoseStack modelView, float tickDelta, Frustum frustum, double cameraX, double cameraY, double cameraZ, CallbackInfoReturnable<Integer> cir) {
        AttachmentEntityRenderDispatcher.renderShadows(Minecraft.getInstance().level, modelView, bufferSource, tickDelta, cameraX, cameraY, cameraZ);
    }
}
