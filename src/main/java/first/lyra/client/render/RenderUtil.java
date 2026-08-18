package first.lyra.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import first.lyra.mixin.BufferBuilderAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 独立模型渲染工具（静态方法调用，全参数签名，26.2 向下移植）。
 * <p>
 * 渲染经 {@code ModelEvent.RegisterAdditional} 注册的独立模型（{@link ModelResourceLocation}）。
 * 模型顶点数据预烘焙为紧凑数组（消除逐帧 record/解包开销），提交时一次
 * {@code ByteBufferBuilder.reserve} + <b>Unsafe 批量直写</b>（跳过逐顶点 addVertex 检查，
 * Java 21 无 java.lang.foreign 正式 API，用 Unsafe 替代 26.2 的 MemorySegment）。
 * 管线 {@link Sheets#translucentItemSheet()}（items atlas，ITEM 格式 36 字节/顶点）。
 * </p>
 */
public final class RenderUtil {

    /** 全亮光照常量（packed）。 */
    public static final int FULL_LIGHT = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;

    private static final Map<ModelResourceLocation, CachedModel> CACHE = new HashMap<>();

    /** 缓存结果：预烘焙紧凑数组。vertices 每顶点 5 float（x,y,z 局部 + atlas u,v），colors 每顶点 1 int（ARGB）。 */
    record CachedModel(BakedModel model, float[] vertices, int[] colors) {}

    private RenderUtil() {
    }

    /**
     * 渲染独立模型（全参数版本）。
     *
     * @param key          模型注册键（ModelResourceLocation）
     * @param poseStack    姿态栈（模型视图空间）
     * @param bufferSource 渲染缓冲源
     * @param tintColor    整体染色 ARGB，-1 不染色
     * @param packedLight  打包光照
     */
    public static void renderStandalone(ModelResourceLocation key, PoseStack poseStack, MultiBufferSource bufferSource, int tintColor, int packedLight) {
        CachedModel cached = getCached(key);
        if (cached == null) {
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        writeModel(poseStack.last().pose(), consumer, cached, tintColor, packedLight);
    }

    /**
     * 模型批量写入（对齐 26.2：BLOCK 格式 32 字节/顶点——Position 12 + Color 4 + UV0 8 + UV2 4 + Normal 3 + 对齐 1；
     * 兼容 ITEM 36B——额外 UV1 overlay 4）。
     * consumer 为 BufferBuilder 时一次 reserve + Unsafe 直写（无检查、无 JNI 边界）；其他实现回退逐顶点。
     */
    private static void writeModel(Matrix4f poseMatrix, VertexConsumer consumer, CachedModel cached, int tintColor, int packedLight) {
        if (consumer instanceof BufferBuilder builder) {
            BufferBuilderAccessor accessor = (BufferBuilderAccessor) builder;
            int vertexCount = cached.colors().length;
            int vertexSize = accessor.getFormat().getVertexSize();
            long totalBytes = (long) vertexCount * vertexSize;
            long pointer = accessor.getBuffer().reserve((int) totalBytes);
            float[] vertices = cached.vertices();
            int[] colors = cached.colors();
            boolean tinted = tintColor != -1;
            boolean entityFormat = vertexSize == 36;
            Vector3f position = new Vector3f();
            Unsafe unsafe = UNSAFE;
            long offset = 0;
            for (int i = 0; i < vertexCount; i++) {
                int sourceIndex = i * 5;
                position.set(vertices[sourceIndex], vertices[sourceIndex + 1], vertices[sourceIndex + 2]);
                poseMatrix.transformPosition(position);
                int color = colors[i];
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
                unsafe.putFloat(pointer + offset, vertices[sourceIndex + 3]);
                offset += 4;
                unsafe.putFloat(pointer + offset, vertices[sourceIndex + 4]);
                offset += 4;
                if (entityFormat) {
                    unsafe.putInt(pointer + offset, OverlayTexture.NO_OVERLAY);
                    offset += 4;
                }
                unsafe.putInt(pointer + offset, packedLight);
                offset += 4;
                unsafe.putByte(pointer + offset, (byte) 0);
                offset += 1;
                unsafe.putByte(pointer + offset, (byte) 0);
                offset += 1;
                unsafe.putByte(pointer + offset, (byte) 127);
                offset += 1;
                offset += 1; // 对齐
            }
            accessor.setVertices(accessor.getVertices() + vertexCount);
            accessor.setElementsToFill(0);
        } else {
            // 回退：逐顶点写入（非 BufferBuilder consumer）
            for (int i = 0; i < cached.colors().length; i++) {
                int sourceIndex = i * 5;
                int color = cached.colors()[i];
                if (tintColor != -1) {
                    color = multiplyColor(tintColor, color);
                }
                consumer.addVertex(poseMatrix, cached.vertices()[sourceIndex], cached.vertices()[sourceIndex + 1], cached.vertices()[sourceIndex + 2])
                        .setColor(color)
                        .setUv(cached.vertices()[sourceIndex + 3], cached.vertices()[sourceIndex + 4])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight)
                        .setNormal(0, 0, 1);
            }
        }
    }

    /**
     * 通用顶点批量写入（Unsafe 直写，轨迹/伤害数字等共用，26.2 移植）。
     * <p>
     * 数据约定：每顶点 5 float（已变换 x,y,z + u,v）+ 每顶点 1 int（ARGB 颜色）。
     * 按目标格式布局写入：ITEM/NEW_ENTITY（36B，overlay=0、normal=0,0,1）与 BLOCK（32B，无 overlay）。
     * </p>
     *
     * @param consumer    顶点消费者
     * @param xyzuvData   每顶点 5 float（x,y,z,u,v，模型视图空间已变换）
     * @param colorData   每顶点 1 int（ARGB）
     * @param packedLight 打包光照
     * @param vertexCount 顶点数
     */
    public static void writeVertices(VertexConsumer consumer, float[] xyzuvData, int[] colorData, int packedLight, int vertexCount) {
        if (consumer instanceof BufferBuilder builder) {
            BufferBuilderAccessor accessor = (BufferBuilderAccessor) builder;
            int vertexSize = accessor.getFormat().getVertexSize();
            long totalBytes = (long) vertexCount * vertexSize;
            long pointer = accessor.getBuffer().reserve((int) totalBytes);
            boolean entityFormat = vertexSize == 36;
            Unsafe unsafe = UNSAFE;
            long offset = 0;
            for (int i = 0; i < vertexCount; i++) {
                int sourceIndex = i * 5;
                unsafe.putFloat(pointer + offset, xyzuvData[sourceIndex]);
                offset += 4;
                unsafe.putFloat(pointer + offset, xyzuvData[sourceIndex + 1]);
                offset += 4;
                unsafe.putFloat(pointer + offset, xyzuvData[sourceIndex + 2]);
                offset += 4;
                unsafe.putInt(pointer + offset, FastColor.ABGR32.fromArgb32(colorData[i]));
                offset += 4;
                unsafe.putFloat(pointer + offset, xyzuvData[sourceIndex + 3]);
                offset += 4;
                unsafe.putFloat(pointer + offset, xyzuvData[sourceIndex + 4]);
                offset += 4;
                if (entityFormat) {
                    unsafe.putInt(pointer + offset, OverlayTexture.NO_OVERLAY);
                    offset += 4;
                    unsafe.putInt(pointer + offset, packedLight);
                    offset += 4;
                    unsafe.putByte(pointer + offset, (byte) 0);
                    offset += 1;
                    unsafe.putByte(pointer + offset, (byte) 0);
                    offset += 1;
                    unsafe.putByte(pointer + offset, (byte) 127);
                    offset += 1;
                    offset += 1; // 对齐
                } else {
                    unsafe.putInt(pointer + offset, packedLight);
                    offset += 4;
                    unsafe.putByte(pointer + offset, (byte) 0);
                    offset += 1;
                    unsafe.putByte(pointer + offset, (byte) 0);
                    offset += 1;
                    unsafe.putByte(pointer + offset, (byte) 127);
                    offset += 1;
                    offset += 1; // BLOCK 32B = 28 数据 + Normal 3 + 对齐 1
                }
            }
            accessor.setVertices(accessor.getVertices() + vertexCount);
            accessor.setElementsToFill(0);
        } else {
            // 回退：逐顶点写入（非 BufferBuilder consumer）
            for (int i = 0; i < vertexCount; i++) {
                int sourceIndex = i * 5;
                consumer.addVertex(xyzuvData[sourceIndex], xyzuvData[sourceIndex + 1], xyzuvData[sourceIndex + 2],
                        colorData[i], xyzuvData[sourceIndex + 3], xyzuvData[sourceIndex + 4],
                        OverlayTexture.NO_OVERLAY, packedLight, 0, 0, 1);
            }
        }
    }

    /** ARGB 逐通道相乘（tint × base）。 */
    private static int multiplyColor(int tintColor, int color) {
        return FastColor.ARGB32.color(
                FastColor.ARGB32.alpha(tintColor) * FastColor.ARGB32.alpha(color) / 255,
                FastColor.ARGB32.red(tintColor) * FastColor.ARGB32.red(color) / 255,
                FastColor.ARGB32.green(tintColor) * FastColor.ARGB32.green(color) / 255,
                FastColor.ARGB32.blue(tintColor) * FastColor.ARGB32.blue(color) / 255);
    }

    private static CachedModel getCached(ModelResourceLocation key) {
        Minecraft minecraft = Minecraft.getInstance();
        ModelManager modelManager = minecraft.getModelManager();
        BakedModel model = modelManager.getModel(key);
        if (model == modelManager.getMissingModel()) {
            return null;
        }
        CachedModel cached = CACHE.get(key);
        if (cached == null || cached.model() != model) {
            cached = buildCached(model);
            CACHE.put(key, cached);
        }
        return cached;
    }

    /**
     * 预烘焙：收集 6 方向 + unculled 的全部 quads（BLOCK 打包 int[]，每顶点 8 个：
     * pos×3 + color(ABGR) + uv×2（0-16 像素）+ light + normal），
     * 提取为紧凑数组（uv 经 sprite.getU/getV 换算为 atlas 坐标，颜色转 ARGB）。
     */
    private static CachedModel buildCached(BakedModel model) {
        RandomSource random = RandomSource.create();
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            quads.addAll(model.getQuads(null, direction, random));
        }
        quads.addAll(model.getQuads(null, null, random));
        int count = quads.size();
        float[] vertices = new float[count * 20];
        int[] colors = new int[count * 4];
        int vertexIndex = 0;
        int colorIndex = 0;
        for (BakedQuad quad : quads) {
            int[] packed = quad.getVertices();
            for (int vertex = 0; vertex < 4; vertex++) {
                int base = vertex * 8;
                vertices[vertexIndex++] = Float.intBitsToFloat(packed[base]);
                vertices[vertexIndex++] = Float.intBitsToFloat(packed[base + 1]);
                vertices[vertexIndex++] = Float.intBitsToFloat(packed[base + 2]);
                // 1.21.1 烘焙 uv 已是 atlas 坐标（putBulkData 原样直传），不再经 sprite 换算
                vertices[vertexIndex++] = Float.intBitsToFloat(packed[base + 4]);
                vertices[vertexIndex++] = Float.intBitsToFloat(packed[base + 5]);
                // ABGR32 → ARGB（1.21.1 无 toArgb32，手动换位）
                int abgr = packed[base + 3];
                colors[colorIndex++] = (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF);
            }
        }
        return new CachedModel(model, vertices, colors);
    }

    /** Unsafe（sun.misc，反射取 theUnsafe 实例；Java 21 无 java.lang.foreign 正式 API）。 */
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
