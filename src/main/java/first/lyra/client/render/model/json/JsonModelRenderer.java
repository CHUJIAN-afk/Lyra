package first.lyra.client.render.model.json;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import first.lyra.client.render.LyraRenderTypes;
import first.lyra.mixin.BufferBuilderAccessor;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
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
        return ModelResourceLocation.standalone(resourcePath(modelId));
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
        if (consumer instanceof BufferBuilder builder) {
            writeQuadsBatch(pose, builder, quads, color, packedLight);
        } else {
            writeQuadsSingle(pose, consumer, quads, color, packedLight);
        }
    }

    private static void writeQuadsBatch(
            PoseStack.Pose pose,
            BufferBuilder builder,
            List<BakedQuad> quads,
            int tintColor,
            int packedLight
    ) {
        int vertexCount = quads.size() * 4;
        boolean tinted = tintColor != -1;
        BufferBuilderAccessor accessor = (BufferBuilderAccessor) builder;
        int vertexSize = accessor.getFormat().getVertexSize();
        boolean entityFormat = vertexSize == 36;
        long totalBytes = (long) vertexCount * vertexSize;
        long pointer = accessor.getBuffer().reserve((int) totalBytes);
        Unsafe unsafe = UNSAFE;
        long offset = 0;
        Vector3f position = new Vector3f();
        Vector3f normal = new Vector3f();
        for (BakedQuad quad : quads) {
            pose.transformNormal(quad.getDirection().step(), normal);
            byte nx = normalByte(normal.x());
            byte ny = normalByte(normal.y());
            byte nz = normalByte(normal.z());
            int[] packed = quad.getVertices();
            for (int vertex = 0; vertex < 4; vertex++) {
                int base = vertex * 8;
                position.set(
                        Float.intBitsToFloat(packed[base]),
                        Float.intBitsToFloat(packed[base + 1]),
                        Float.intBitsToFloat(packed[base + 2])
                );
                pose.pose().transformPosition(position);
                int color = packed[base + 3];
                color = (color & 0xFF00FF00) | ((color & 0xFF) << 16) | ((color >> 16) & 0xFF);
                if (tinted) {
                    color = multiplyColor(tintColor, color);
                }
                unsafe.putFloat(pointer + offset, position.x());
                offset += 4;
                unsafe.putFloat(pointer + offset, position.y());
                offset += 4;
                unsafe.putFloat(pointer + offset, position.z());
                offset += 4;
                unsafe.putInt(pointer + offset, FastColor.ABGR32.fromArgb32(color));
                offset += 4;
                unsafe.putFloat(pointer + offset, Float.intBitsToFloat(packed[base + 4]));
                offset += 4;
                unsafe.putFloat(pointer + offset, Float.intBitsToFloat(packed[base + 5]));
                offset += 4;
                if (entityFormat) {
                    unsafe.putInt(pointer + offset, OverlayTexture.NO_OVERLAY);
                    offset += 4;
                }
                unsafe.putInt(pointer + offset, packedLight);
                offset += 4;
                unsafe.putByte(pointer + offset, nx);
                offset += 1;
                unsafe.putByte(pointer + offset, ny);
                offset += 1;
                unsafe.putByte(pointer + offset, nz);
                offset += 1;
                offset += 1;
            }
        }
        accessor.setVertices(accessor.getVertices() + vertexCount);
        accessor.setElementsToFill(0);
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
            pose.transformNormal(quad.getDirection().step(), normal);
            int[] packed = quad.getVertices();
            for (int vertex = 0; vertex < 4; vertex++) {
                int base = vertex * 8;
                int color = packed[base + 3];
                color = (color & 0xFF00FF00) | ((color & 0xFF) << 16) | ((color >> 16) & 0xFF);
                if (tinted) {
                    color = multiplyColor(tintColor, color);
                }
                consumer.addVertex(
                                pose.pose(),
                                Float.intBitsToFloat(packed[base]),
                                Float.intBitsToFloat(packed[base + 1]),
                                Float.intBitsToFloat(packed[base + 2])
                        )
                        .setColor(color)
                        .setUv(Float.intBitsToFloat(packed[base + 4]), Float.intBitsToFloat(packed[base + 5]))
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight)
                        .setNormal(normal.x(), normal.y(), normal.z());
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

    private static byte normalByte(float value) {
        return (byte) ((int) (Mth.clamp(value, -1.0F, 1.0F) * 127.0F) & 0xFF);
    }

    private static final Unsafe UNSAFE = getUnsafe();

    private static Unsafe getUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to obtain Unsafe", exception);
        }
    }
}
