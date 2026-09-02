package first.lyra.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import first.lyra.mixin.BufferBuilderAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 独立模型渲染工具（静态方法调用，全参数签名，26.2 行为对齐）。
 * <p>
 * 渲染经 {@code ModelEvent.RegisterAdditional} 注册的独立模型（{@link ModelResourceLocation}）。
 * 模型<b>不手动缓存</b>：每次渲染时经 {@link ModelManager#getModel} 获取烘焙模型（烘焙 manager），
 * 遍历其全部 quads（6 方向 + unculled）即时写入。
 * 顶点写入使用<b>完整实体顶点格式</b>（位置/颜色/UV/overlay/光照/法线），
 * 法线取 quad 朝向并经姿态 normal 矩阵变换（不忽略法线、不写死 0,0,1）。
 * consumer 为 BufferBuilder 时一次 reserve + Unsafe 直写（跳过逐顶点 addVertex 检查）；
 * 其他实现回退逐顶点 addVertex。
 * </p>
 */
@SuppressWarnings("deprecation")
public final class RenderUtil {

    /** 全亮光照常量（packed）。 */
    public static final int FULL_LIGHT = LightTexture.FULL_BRIGHT;

    private RenderUtil() {
    }

    /**
     * 渲染独立模型（全参数版本，与 26.2 同构；跨版本仅需替换 key/bufferSource 类型）。
     *
     * @param key          模型注册键（ModelResourceLocation）
     * @param poseStack    姿态栈（模型视图空间）
     * @param bufferSource 渲染缓冲源
     * @param tintColor    整体染色 ARGB，-1 不染色
     * @param packedLight  打包光照
     */
    public static void renderStandalone(ModelResourceLocation key, PoseStack poseStack, MultiBufferSource bufferSource, int tintColor, int packedLight) {
        Minecraft minecraft = Minecraft.getInstance();
        ModelManager modelManager = minecraft.getModelManager();
        // 不手动缓存：每次从模型烘焙 manager 获取（资源重载后自动生效）
        BakedModel model = modelManager.getModel(key);
        if (model != modelManager.getMissingModel()) {
            VertexConsumer consumer = bufferSource.getBuffer(LyraRenderTypes.getModel());
            writeModel(poseStack.last(), consumer, model, tintColor, packedLight);
        }
    }

    /**
     * 渲染始终面向相机的贴图（1.21.1 对应 26.2 renderImage；召唤标记等使用）。
     *
     * @param texture      贴图路径
     * @param center       世界坐标中心
     * @param width        宽
     * @param height       高
     * @param bufferSource 渲染缓冲源
     * @param alwaysVisible true = 自发光变体（不受光照变暗）
     * @param tintColor    整体染色 ARGB
     */
    public static void renderImage(ResourceLocation texture, Vec3 center, float width, float height, MultiBufferSource bufferSource, boolean alwaysVisible, int tintColor) {
        Camera camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
        VertexConsumer consumer = bufferSource.getBuffer(LyraRenderTypes.texture(texture, alwaysVisible));
        Vec3 camPos = camera.getPosition();
        // 相机朝向四元数（原版实体名牌同款）：rotation × XN(180)
        Quaternionf rotation = new Quaternionf(camera.rotation()).mul(Axis.XN.rotationDegrees(180), new Quaternionf());
        Matrix4f matrix = new Matrix4f().rotate(rotation).setTranslation((float) (center.x - camPos.x), (float) (center.y - camPos.y), (float) (center.z - camPos.z));
        float halfWidth = width / 2f;
        float halfHeight = height / 2f;
        // 每顶点 5 float（已变换 x,y,z + u,v）+ 颜色
        float[] xyzuvData = new float[4 * 5];
        int[] colorData = new int[4];
        Vector3f v = new Vector3f();
        int vertexIndex = 0;
        // 四边形顶点：x,y,z 偏移 + u,v（26.2 同序：u 随宽度、v 随高度）
        float[][] corners = {
                {-halfWidth, -halfHeight, 0f, 0f, 0f},
                {-halfWidth, halfHeight, 0f, 0f, 1f},
                {halfWidth, halfHeight, 0f, 1f, 1f},
                {halfWidth, -halfHeight, 0f, 1f, 0f}
        };
        for (float[] corner : corners) {
            matrix.transformPosition(corner[0], corner[1], corner[2], v);
            int dataIndex = vertexIndex * 5;
            xyzuvData[dataIndex] = v.x();
            xyzuvData[dataIndex + 1] = v.y();
            xyzuvData[dataIndex + 2] = v.z();
            xyzuvData[dataIndex + 3] = corner[3];
            xyzuvData[dataIndex + 4] = corner[4];
            colorData[vertexIndex] = tintColor;
            vertexIndex++;
        }
        writeVertices(consumer, xyzuvData, colorData, FULL_LIGHT, 4);
    }

    /**
     * 模型写入：遍历模型全部 quads（6 方向 + unculled），
     * 每 quad 以朝向为法线（经姿态 normal 矩阵变换），完整实体顶点格式写入。
     */
    private static void writeModel(PoseStack.Pose pose, VertexConsumer consumer, BakedModel model, int tintColor, int packedLight) {
        RandomSource random = RandomSource.create();
        for (Direction direction : Direction.values()) {
            writeQuads(pose, consumer, model.getQuads(null, direction, random), tintColor, packedLight);
        }
        writeQuads(pose, consumer, model.getQuads(null, null, random), tintColor, packedLight);
    }

    /**
     * 顶点写入：consumer 为 BufferBuilder 时走 Unsafe 批量直写（ENTITY 格式，含真实法线）；
     * 其他实现回退逐顶点 addVertex。
     */
    private static void writeQuads(PoseStack.Pose pose, VertexConsumer consumer, List<BakedQuad> quads, int tintColor, int packedLight) {
        if (quads.isEmpty()) {
            return;
        }
        if (consumer instanceof BufferBuilder builder) {
            writeQuadsBatch(pose, builder, quads, tintColor, packedLight);
        } else {
            writeQuadsSingle(pose, consumer, quads, tintColor, packedLight);
        }
    }

    /**
     * 批量直写（BufferBuilder 快路径）。格式按目标自适应：
     * ENTITY 36B（Position 12 + Color 4 + UV0 8 + UV1 overlay 4 + UV2 light 4 + Normal 3 + 对齐 1）
     * 与 BLOCK 32B（无 UV1 overlay）。
     * 法线取每 quad 自身朝向经姿态 normal 矩阵变换，量化写入 byte 三元组。
     */
    private static void writeQuadsBatch(PoseStack.Pose pose, BufferBuilder builder, List<BakedQuad> quads, int tintColor, int packedLight) {
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
            // quad 自身朝向经姿态 normal 矩阵变换 → 真实法线（与 26.2 一致）
            Direction quadDirection = quad.getDirection();
            pose.transformNormal(quadDirection.step(), normal);
            byte nx = normalByte(normal.x);
            byte ny = normalByte(normal.y);
            byte nz = normalByte(normal.z);
            int[] packed = quad.getVertices();
            for (int vertex = 0; vertex < 4; vertex++) {
                int base = vertex * 8;
                position.set(
                        Float.intBitsToFloat(packed[base]),
                        Float.intBitsToFloat(packed[base + 1]),
                        Float.intBitsToFloat(packed[base + 2]));
                pose.pose().transformPosition(position);
                int color = packed[base + 3];
                // ABGR32 → ARGB
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
                offset += 1; // 对齐
            }
        }
        accessor.setVertices(accessor.getVertices() + vertexCount);
        accessor.setElementsToFill(0);
    }

    /** 逐顶点回退路径（非 BufferBuilder consumer），法线取每 quad 自身朝向变换。 */
    private static void writeQuadsSingle(PoseStack.Pose pose, VertexConsumer consumer, List<BakedQuad> quads, int tintColor, int packedLight) {
        boolean tinted = tintColor != -1;
        Vector3f normal = new Vector3f();
        for (BakedQuad quad : quads) {
            Direction quadDirection = quad.getDirection();
            pose.transformNormal(quadDirection.step(), normal);
            int[] packed = quad.getVertices();
            for (int vertex = 0; vertex < 4; vertex++) {
                int base = vertex * 8;
                int color = packed[base + 3];
                // ABGR32 → ARGB
                color = (color & 0xFF00FF00) | ((color & 0xFF) << 16) | ((color >> 16) & 0xFF);
                if (tinted) {
                    color = multiplyColor(tintColor, color);
                }
                consumer.addVertex(pose.pose(), Float.intBitsToFloat(packed[base]), Float.intBitsToFloat(packed[base + 1]), Float.intBitsToFloat(packed[base + 2]))
                        .setColor(color)
                        .setUv(Float.intBitsToFloat(packed[base + 4]), Float.intBitsToFloat(packed[base + 5]))
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight)
                        .setNormal(normal.x(), normal.y(), normal.z());
            }
        }
    }

    /** 法线量化（float [-1,1] → byte，与原版 putNormals 同款）。 */
    private static byte normalByte(float value) {
        return (byte) ((int) (Mth.clamp(value, -1.0F, 1.0F) * 127.0F) & 0xFF);
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
                // BLOCK 32B = 28 数据 + Normal 3 + 对齐 1
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
        return FastColor.ARGB32.color(FastColor.ARGB32.alpha(tintColor) * FastColor.ARGB32.alpha(color) / 255, FastColor.ARGB32.red(tintColor) * FastColor.ARGB32.red(color) / 255, FastColor.ARGB32.green(tintColor) * FastColor.ARGB32.green(color) / 255, FastColor.ARGB32.blue(tintColor) * FastColor.ARGB32.blue(color) / 255);
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
