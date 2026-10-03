package first.lyra.client.render.model.json;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import first.lyra.client.render.ColorBufferSource;
import first.lyra.client.render.LyraRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class JsonModelRenderer {

    static boolean render(ModelResourceLocation modelLocation, PoseStack poseStack, MultiBufferSource bufferSource, int color, int packedLight) {
        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = modelManager.getModel(modelLocation);
        if (model != modelManager.getMissingModel()) {
            ColorBufferSource colorBufferSource = new ColorBufferSource(bufferSource);
            colorBufferSource.setColor(color);
            VertexConsumer consumer = colorBufferSource.getBuffer(LyraRenderTypes.getModel());
            Minecraft.getInstance().getItemRenderer().renderModelLists(model, ItemStack.EMPTY, packedLight, OverlayTexture.NO_OVERLAY, poseStack, consumer);
        }
        return true;
    }
}
