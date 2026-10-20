package first.lyra.client.render.model.json;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import first.lyra.client.render.ColorBufferSource;
import first.lyra.client.render.LyraRenderTypes;
import first.lyra.client.render.WholeModelTransparency;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class JsonModelRenderer {

    public static ResourceLocation resourcePath(ResourceLocation modelId) {
        return ResourceLocation.fromNamespaceAndPath(
                modelId.getNamespace(),
                "lyra_model/json/" + modelId.getPath() + "/" + fileName(modelId.getPath())
        );
    }

    public static ModelResourceLocation standaloneLocation(ResourceLocation modelId) {
        return ModelResourceLocation.standalone(resourcePath(modelId));
    }

    private static String fileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    static boolean render(ModelResourceLocation modelLocation, PoseStack poseStack, MultiBufferSource bufferSource, int color, int packedLight) {
        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = modelManager.getModel(modelLocation);
        if (model != modelManager.getMissingModel()) {
            WholeModelTransparency.render(poseStack, bufferSource, ((color >>> 24) & 255) / 255f, packedLight, source -> {
                ColorBufferSource tinted = new ColorBufferSource(source).setColor(color | 0xFF000000);
                VertexConsumer consumer = tinted.getBuffer(LyraRenderTypes.getModel());
                Minecraft.getInstance().getItemRenderer().renderModelLists(model, ItemStack.EMPTY, packedLight, OverlayTexture.NO_OVERLAY, poseStack, consumer);
            });
        }
        return true;
    }
}
