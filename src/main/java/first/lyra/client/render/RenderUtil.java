package first.lyra.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import first.lyra.mixin.BufferBuilderAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 独立模型渲染工具（静态方法调用，即时提交）。
 * <p>
 * 渲染经 standalone 注册机制（ModelEvent.RegisterStandalone）注册的独立模型（{@link QuadCollection}）。
 * </p>
 * <h2>提交路径</h2>
 * <p>
 * 即时提交：{@link #renderStandalone} 直接 submitSpecial（TRANSLUCENT_BLOCKS_AND_ITEMS 排序 phase），
 * 顶点写入用预烘焙数组 + MemorySegment 批量直写（BLOCK 格式，一次 reserve + 逐字段内联写入，
 * 无 JNI、无弃用 API）。管线 {@link LyraRenderTypes#ENTITY_ATLAS_TRANSLUCENT}
 * （items atlas + BLOCK 格式，无 ITEM_ENTITY_TARGET 离屏目标）。
 * </p>
 * <h2>纹理与 uv</h2>
 * <p>
 * 烘焙的 packedUV 是 atlas 坐标（FaceBakery 经 sprite.getU/getV 换算），BLOCK 格式管线
 * 直接采样 items atlas，无需换算。
 * </p>
 * <h2>性能设计</h2>
 * <p>
 * 模型烘焙后 quads 不变,只在资源重载后变化。因此顶点数据预提取为紧凑数组缓存于
 * {@link #CACHE}(key = StandaloneModelKey),每次调用仅做一次 ModelManager 查找 + 缓存查找
 * + 引用比较(cached.collection() != collection 时重建,自动覆盖资源重载),提交阶段零 record 访问。
 * </p>
 */
public final class RenderUtil {

    private static final Map<StandaloneModelKey<QuadCollection>, CachedModel> CACHE = new HashMap<>();

    /** 全亮光照常量（packed，渲染调用直接使用）。 */
    public static final int FULL_LIGHT = LightCoordsUtil.pack(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_SKY);

    /**
     * 缓存结果：烘焙后把 quad 顶点数据预提取为紧凑数组（消除提交期 record/switch/解包开销）。
     * <ul>
     *   <li>vertices：每顶点 5 float（x,y,z,u,v 局部坐标与 uv），4 顶点/quad 连续</li>
     *   <li>colors：每顶点 1 int（烘焙色 ARGB，提交期按 tintColor 直接使用或 multiply）</li>
     * </ul>
     * 法线不需要：BLOCK 格式管线（TRANSLUCENT_BLOCK）无法线元素，着色器无 per-face 漫反射。
     */
    record CachedModel(QuadCollection collection, float[] vertices, int[] colors) {}

    private RenderUtil() {
    }

    /**
     * 渲染独立模型（全参数版本，即时提交）。
     *
     * @param key         模型注册键
     * @param poseStack   姿态栈（模型视图空间）
     * @param collector   提交节点收集器
     * @param tintColor   整体染色 ARGB，-1 不染色
     * @param packedLight 打包光照（LightCoordsUtil.pack）
     */
    public static void renderStandalone(StandaloneModelKey<QuadCollection> key, PoseStack poseStack,
                                        SubmitNodeCollector collector, int tintColor, int packedLight) {
        CachedModel cached = getCached(key);
        if (cached == null) {
            return;
        }
        collector.submitSpecial(RenderPhaseKeys.TRANSLUCENT_BLOCKS_AND_ITEMS, new LyraCustomSubmit(
                poseStack.last().copy(), LyraRenderTypes.ENTITY_ATLAS_TRANSLUCENT,
                (pose, consumer) -> writeQuads(pose, consumer, cached, tintColor, packedLight)));
    }

    /**
     * 顶点写入：consumer 为 BufferBuilder 时走 MemorySegment 批量直写（BLOCK 格式，
     * 一次 reserve + 逐字段内联写入，无 JNI）；其他实现回退逐顶点 addVertex。
     */
    private static void writeQuads(PoseStack.Pose pose, VertexConsumer consumer, CachedModel cached, int tintColor, int packedLight) {
        if (consumer instanceof BufferBuilder builder) {
            writeQuadsBatch(pose, builder, cached, tintColor, packedLight);
        } else {
            writeQuadsSingle(pose, consumer, cached, tintColor, packedLight);
        }
    }

    /**
     * 批量直写（BLOCK 格式 28 字节/顶点：Position 12 + Color 4 + UV0 8 + UV2 4，平台字节序）。
     * 一次 reserve 全部顶点，MemorySegment 直写目标地址（java.lang.foreign 官方 off-heap 访问，
     * JIT 内联为 mov）；经 BufferBuilderAccessor（mixin 接口）同步内部计数。
     */
    private static void writeQuadsBatch(PoseStack.Pose pose, BufferBuilder builder, CachedModel cached, int tintColor, int packedLight) {
        Matrix4f poseMatrix = pose.pose();
        Vector3f position = new Vector3f();
        float[] vertices = cached.vertices();
        int[] colors = cached.colors();
        int vertexCount = colors.length;
        boolean tinted = tintColor != -1;
        BufferBuilderAccessor accessor = (BufferBuilderAccessor) builder;
        long totalBytes = (long) vertexCount * accessor.getFormat().getVertexSize();
        long pointer = accessor.getBuffer().reserve((int) totalBytes);
        MemorySegment segment = MemorySegment.ofAddress(pointer).reinterpret(totalBytes);
        long offset = 0;
        for (int i = 0; i < vertexCount; i++) {
            int sourceIndex = i * 5;
            position.set(vertices[sourceIndex], vertices[sourceIndex + 1], vertices[sourceIndex + 2]);
            poseMatrix.transformPosition(position);
            int color = colors[i];
            if (tinted) {
                color = ARGB.multiply(tintColor, color);
            }
            segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, position.x());
            offset += 4;
            segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, position.y());
            offset += 4;
            segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, position.z());
            offset += 4;
            segment.set(ValueLayout.JAVA_INT_UNALIGNED, offset, ARGB.toABGR(color));
            offset += 4;
            segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, vertices[sourceIndex + 3]);
            offset += 4;
            segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, vertices[sourceIndex + 4]);
            offset += 4;
            segment.set(ValueLayout.JAVA_INT_UNALIGNED, offset, packedLight);
            offset += 4;
        }
        accessor.setVertices(accessor.getVertices() + vertexCount);
        accessor.setElementsToFill(0);
    }

    /** 逐顶点回退路径（非 BufferBuilder consumer）。 */
    private static void writeQuadsSingle(PoseStack.Pose pose, VertexConsumer consumer, CachedModel cached, int tintColor, int packedLight) {
        Matrix4f poseMatrix = pose.pose();
        Vector3f position = new Vector3f();
        float[] vertices = cached.vertices();
        int[] colors = cached.colors();
        int vertexCount = colors.length;
        boolean tinted = tintColor != -1;
        for (int i = 0; i < vertexCount; i++) {
            int sourceIndex = i * 5;
            position.set(vertices[sourceIndex], vertices[sourceIndex + 1], vertices[sourceIndex + 2]);
            poseMatrix.transformPosition(position);
            int color = colors[i];
            if (tinted) {
                color = ARGB.multiply(tintColor, color);
            }
            consumer.addVertex(position.x(), position.y(), position.z(), color,
                    vertices[sourceIndex + 3], vertices[sourceIndex + 4],
                    OverlayTexture.NO_OVERLAY, packedLight, 0, 0, 1);
        }
    }

    private static CachedModel getCached(StandaloneModelKey<QuadCollection> key) {
        Minecraft minecraft = Minecraft.getInstance();
        QuadCollection collection = minecraft.getModelManager().getStandaloneModel(key);
        if (collection == null) {
            return null;
        }
        CachedModel cached = CACHE.get(key);
        if (cached == null || cached.collection() != collection) {
            cached = buildCached(collection);
            CACHE.put(key, cached);
        }
        return cached;
    }

    /**
     * 预烘焙：收集 6 方向 + unculled 的全部 quads（getQuads(null) 仅返回 unculled），
     * 提取顶点数据到紧凑数组。
     */
    private static CachedModel buildCached(QuadCollection collection) {
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            quads.addAll(collection.getQuads(direction));
        }
        quads.addAll(collection.getQuads(null));
        int count = quads.size();
        float[] vertices = new float[count * 20];
        int[] colors = new int[count * 4];
        int vertexIndex = 0;
        int colorIndex = 0;
        for (BakedQuad quad : quads) {
            for (int vertex = 0; vertex < 4; vertex++) {
                Vector3fc position = quad.position(vertex);
                vertices[vertexIndex++] = position.x();
                vertices[vertexIndex++] = position.y();
                vertices[vertexIndex++] = position.z();
                long packedUv = quad.packedUV(vertex);
                vertices[vertexIndex++] = UVPair.unpackU(packedUv);
                vertices[vertexIndex++] = UVPair.unpackV(packedUv);
                colors[colorIndex++] = quad.bakedColors().color(vertex);
            }
        }
        return new CachedModel(collection, vertices, colors);
    }
}
