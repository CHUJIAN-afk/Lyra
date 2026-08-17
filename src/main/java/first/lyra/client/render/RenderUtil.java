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
 * 独立模型渲染工具（静态方法调用）。
 * <p>
 * 渲染经 standalone 注册机制（ModelEvent.RegisterStandalone）注册的独立模型（{@link QuadCollection}）。
 * </p>
 * <h2>提交路径（26.2 高性能路径）</h2>
 * <p>
 * 手写顶点提交：{@link LyraRenderTypes#ENTITY_ATLAS_TRANSLUCENT}（items atlas + entity 管线，
 * 无 item sheet 的 ITEM_ENTITY_TARGET 离屏目标）+ 复用 scratch Vector3f 逐顶点写入
 * （无 {@code putBakedQuad} 的每顶点分配）。一次提交全部 quads（单 renderType 单 draw）。
 * 经 {@code submitSpecial(RenderPhaseKeys.AFTER_TERRAIN, ...)} 挂 afterTerrain phase
 * （半透明方块/云之后渲染，层级正确）。
 * </p>
 * <h2>性能设计</h2>
 * <p>
 * 模型烘焙后 quads 不变,只在资源重载后变化。因此扁平 quads 列表缓存于 {@link #CACHE}
 * (key = StandaloneModelKey),每次调用仅做一次 ModelManager 查找 + 缓存查找 + 引用比较
 * (cached.collection() != collection 时重建,自动覆盖资源重载),提交阶段零分组开销。
 * </p>
 */
public class RenderUtil {

    private static final Map<StandaloneModelKey<QuadCollection>, CachedModel> CACHE = new HashMap<>();
    /** 全亮光照常量（消除每顶点 pack 调用）。 */
    private static final int FULL_LIGHT = LightCoordsUtil.pack(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_SKY);

    private RenderUtil() {
    }

    //TODO 只保留全参数的渲染方法
    public static void renderStandalone(StandaloneModelKey<QuadCollection> key, PoseStack poseStack, SubmitNodeCollector collector, int ARGB, int packedLight) {
        renderStandalone(key, poseStack, collector, -1);
    }

    /**
     * 渲染独立模型（不染色）。
     */
    public static void renderStandalone(StandaloneModelKey<QuadCollection> key, PoseStack poseStack, SubmitNodeCollector collector) {
        renderStandalone(key, poseStack, collector, -1);
    }

    /**
     * 渲染独立模型并整体染色。
     * <p>
     * tintLayers（submitItem 的 tintIndex 机制）仅对带 tintIndex 的 quad 生效，item/generated
     * 自动 quad 无 tintIndex → 必须手写顶点色染色（ARGB.multiply(tint, bakedColor)）。
     * 回调的 pose 为提交时快照,必须使用而非外部捕获的 poseStack 引用
     * (提交延迟执行,poseStack 随后会被 popPose/mulPose 修改)。
     * </p>
     */
    public static void renderStandalone(StandaloneModelKey<QuadCollection> key, PoseStack poseStack, SubmitNodeCollector collector, int r, int g, int b, int a) {
        renderStandalone(key, poseStack, collector, ARGB.color(a, r, g, b));
    }

    private static void renderStandalone(StandaloneModelKey<QuadCollection> key, PoseStack poseStack, SubmitNodeCollector collector, int tint) {
        CachedModel cached = getCached(key);
        if (cached == null) {
            return;
        }
        collector.submitSpecial(RenderPhaseKeys.TRANSLUCENT_BLOCKS_AND_ITEMS, new LyraCustomSubmit(poseStack.last().copy(), LyraRenderTypes.ENTITY_ATLAS_TRANSLUCENT, (pose, consumer) -> writeQuads(pose, consumer, cached, tint)));
    }

    /**
     * 顶点写入：consumer 为 BufferBuilder 时走批量快路径（一次 reserve + 内存组装 + 一次
     * memCopy，JNI 边界从每顶点 8 次降为每批 1 次）；其他实现回退逐顶点 addVertex
     * （BLOCK 格式下 blockFormat 快路径，忽略 overlay/normal）。
     * 顶点色 = tint × 烘焙色（tint = -1 时跳过乘法）。
     */
    private static void writeQuads(PoseStack.Pose pose, VertexConsumer consumer, CachedModel cached, int tint) {
        if (consumer instanceof BufferBuilder builder) {
            writeQuadsBatch(pose, builder, cached, tint);
        } else {
            writeQuadsSingle(pose, consumer, cached, tint);
        }
    }

    /**
     * 批量写入（零 JNI、无弃用 API）：一次 reserve 全部顶点，MemorySegment 直写目标地址
     * （java.lang.foreign 官方 off-heap 访问，JIT 内联为 mov，无 memCopy/memPut 的 JNI 边界，
     * 替代弃用的 sun.misc.Unsafe.put*）。
     * BLOCK 格式（28 字节/顶点：Position 12 + Color 4 + UV0 8 + UV2 4，平台字节序）。
     * 模拟 beginVertex：经 BufferBuilderAccessor（mixin 接口）同步内部计数，
     * 保证 build() 正确产出 MeshData。
     */
    private static void writeQuadsBatch(PoseStack.Pose pose, BufferBuilder builder, CachedModel cached, int tint) {
        Matrix4f matrix = pose.pose();
        Vector3f pos = new Vector3f();
        float[] vertices = cached.vertices();
        int[] colors = cached.colors();
        int vertexCount = colors.length;
        boolean tinted = tint != -1;
        BufferBuilderAccessor accessor = (BufferBuilderAccessor) builder;
        long pointer = accessor.getBuffer().reserve(vertexCount * accessor.getFormat().getVertexSize());
        // ofAddress(long) 为 zero-length segment，reinterpret 指定实际大小（信任模式）
        MemorySegment segment = MemorySegment.ofAddress(pointer).reinterpret((long) vertexCount * accessor.getFormat().getVertexSize());
        long off = 0;
        for (int i = 0; i < vertexCount; i++) {
            int idx = i * 5;
            pos.set(vertices[idx], vertices[idx + 1], vertices[idx + 2]);
            matrix.transformPosition(pos);
            int vertexColor = colors[i];
            if (tinted) {
                vertexColor = ARGB.multiply(tint, vertexColor);
            }
            segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, off, pos.x());
            off += 4;
            segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, off, pos.y());
            off += 4;
            segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, off, pos.z());
            off += 4;
            segment.set(ValueLayout.JAVA_INT_UNALIGNED, off, ARGB.toABGR(vertexColor));
            off += 4;
            segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, off, vertices[idx + 3]);
            off += 4;
            segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, off, vertices[idx + 4]);
            off += 4;
            segment.set(ValueLayout.JAVA_INT_UNALIGNED, off, FULL_LIGHT);
            off += 4;
        }
        accessor.setVertices(accessor.getVertices() + vertexCount);
        accessor.setElementsToFill(0);
    }

    /** 逐顶点回退路径（非 BufferBuilder consumer）。 */
    private static void writeQuadsSingle(PoseStack.Pose pose, VertexConsumer consumer, CachedModel cached, int tint) {
        Matrix4f matrix = pose.pose();
        Vector3f pos = new Vector3f();
        float[] vertices = cached.vertices();
        int[] colors = cached.colors();
        int quadCount = colors.length / 4;
        boolean tinted = tint != -1;
        for (int q = 0; q < quadCount; q++) {
            int vb = q * 20;
            int cb = q * 4;
            for (int v = 0; v < 4; v++) {
                int idx = vb + v * 5;
                pos.set(vertices[idx], vertices[idx + 1], vertices[idx + 2]);
                matrix.transformPosition(pos);
                int vertexColor = colors[cb + v];
                if (tinted) {
                    vertexColor = ARGB.multiply(tint, vertexColor);
                }
                consumer.addVertex(pos.x(), pos.y(), pos.z(), vertexColor,
                        vertices[idx + 3], vertices[idx + 4],
                        OverlayTexture.NO_OVERLAY, FULL_LIGHT,
                        0, 0, 1);
            }
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
        int vi = 0;
        int ci = 0;
        for (BakedQuad quad : quads) {
            for (int vertex = 0; vertex < 4; vertex++) {
                Vector3fc p = quad.position(vertex);
                vertices[vi++] = p.x();
                vertices[vi++] = p.y();
                vertices[vi++] = p.z();
                long packedUv = quad.packedUV(vertex);
                vertices[vi++] = UVPair.unpackU(packedUv);
                vertices[vi++] = UVPair.unpackV(packedUv);
                colors[ci++] = quad.bakedColors().color(vertex);
            }
        }
        return new CachedModel(collection, vertices, colors);
    }

    /**
     * 缓存结果：烘焙后把 quad 顶点数据预提取为紧凑数组（消除渲染期 record/switch/解包开销）。
     * <ul>
     *   <li>vertices：每顶点 5 float（x,y,z,u,v 局部坐标与 uv），4 顶点/quad 连续</li>
     *   <li>colors：每顶点 1 int（烘焙色 ARGB，渲染期按 tint 直接使用或 multiply）</li>
     * </ul>
     * 法线不需要：BLOCK 格式管线（TRANSLUCENT_BLOCK）无法线元素，着色器无 per-face 漫反射。
     */
    private record CachedModel(QuadCollection collection, float[] vertices, int[] colors) {}
}
