package first.lyra.client.render.model.json;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import first.lyra.client.render.LyraRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import java.util.List;

/**
 * Static vanilla JSON model renderer. This module deliberately has no animation,
 * texture override, bone visibility or entity state support.
 */
public final class JsonModelRenderer {

    private JsonModelRenderer() {
    }

    public static ResourceLocation resourcePath(ResourceLocation modelId) {
        return ResourceLocation.fromNamespaceAndPath(
                modelId.getNamespace(),
                "lyra_model/json/" + modelId.getPath() + "/" + fileName(modelId.getPath())
        );
    }

    public static ModelResourceLocation standaloneLocation(ResourceLocation modelId) {
        return new ModelResourceLocation(resourcePath(modelId), "standalone");
    }

    private static String fileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    static boolean render(
            ModelResourceLocation modelLocation,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int color,
            int packedLight
    ) {
        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = modelManager.getModel(modelLocation);
        if (model == modelManager.getMissingModel()) {
            return false;
        }

        VertexConsumer consumer = bufferSource.getBuffer(LyraRenderTypes.getModel());
        writeModel(poseStack.last(), consumer, model, color, packedLight);
        return true;
    }

    private static void writeModel(PoseStack.Pose pose, VertexConsumer consumer, BakedModel model, int color, int packedLight) {
        RandomSource random = RandomSource.create();
        for (Direction direction : Direction.values()) {
            writeQuads(pose, consumer, model.getQuads(null, direction, random), color, packedLight);
        }
        writeQuads(pose, consumer, model.getQuads(null, null, random), color, packedLight);
    }

    private static void writeQuads(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            List<BakedQuad> quads,
            int color,
            int packedLight
    ) {
        if (quads.isEmpty()) {
            return;
        }
        writeQuadsSingle(pose, consumer, quads, color, packedLight);
    }

    private static void writeQuadsSingle(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            List<BakedQuad> quads,
            int tintColor,
            int packedLight
    ) {
        boolean tinted = tintColor != -1;
        Vector3f normal = new Vector3f();
        for (BakedQuad quad : quads) {
            normal.set(quad.getDirection().step()).mul(pose.normal()).normalize();
            int[] packed = quad.getVertices();
            for (int vertex = 0; vertex < 4; vertex++) {
                int base = vertex * 8;
                int color = packed[base + 3];
                color = (color & 0xFF00FF00) | ((color & 0xFF) << 16) | ((color >> 16) & 0xFF);
                if (tinted) {
                    color = multiplyColor(tintColor, color);
                }
                consumer.vertex(
                                pose.pose(),
                                Float.intBitsToFloat(packed[base]),
                                Float.intBitsToFloat(packed[base + 1]),
                                Float.intBitsToFloat(packed[base + 2])
                        )
                        .color(FastColor.ARGB32.alpha(color), FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color))
                        .uv(Float.intBitsToFloat(packed[base + 4]), Float.intBitsToFloat(packed[base + 5]))
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight)
                        .normal(normal.x(), normal.y(), normal.z())
                        .endVertex();
            }
        }
    }

    private static int multiplyColor(int tintColor, int color) {
        return FastColor.ARGB32.color(
                FastColor.ARGB32.alpha(tintColor) * FastColor.ARGB32.alpha(color) / 255,
                FastColor.ARGB32.red(tintColor) * FastColor.ARGB32.red(color) / 255,
                FastColor.ARGB32.green(tintColor) * FastColor.ARGB32.green(color) / 255,
                FastColor.ARGB32.blue(tintColor) * FastColor.ARGB32.blue(color) / 255
        );
    }

}
