package first.lyra.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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

    /** 缓存结果,collection 引用用于资源重载后失效重建。 */
    private record CachedModel(QuadCollection collection, List<BakedQuad> quads) {}

    private RenderUtil() {
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
        collector.submitSpecial(RenderPhaseKeys.AFTER_TERRAIN, new LyraCustomSubmit(poseStack.last().copy(), LyraRenderTypes.ENTITY_ATLAS_TRANSLUCENT, (pose, consumer) -> writeQuads(pose, consumer, cached.quads(), tint)));
    }

    /** 全亮光照常量（消除每顶点 pack 调用）。 */
    private static final int FULL_LIGHT = LightCoordsUtil.pack(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_SKY);

    /**
     * 手写顶点写入（无分配）：复用 scratch Vector3f 变换位置与法线，11 参 addVertex 直写
     * （BufferBuilder 对 ENTITY/BLOCK 格式是快路径——一次 beginVertex 直写内存；
     * 勿经 VertexConsumer 包装器，否则降级 default 链式慢路径）。
     * 顶点色 = tint × quad 烘焙色（tint = -1 时仅烘焙色，跳过乘法）。
     */
    private static void writeQuads(PoseStack.Pose pose, VertexConsumer consumer, List<BakedQuad> quads, int tint) {
        Matrix4f matrix = pose.pose();
        Vector3f pos = new Vector3f();
        Vector3f normal = new Vector3f();
        for (BakedQuad quad : quads) {
            pose.transformNormal(quad.direction().getUnitVec3f(), normal);
            for (int vertex = 0; vertex < 4; vertex++) {
                long packedUv = quad.packedUV(vertex);
                int vertexColor = tint == -1 ? quad.bakedColors().color(vertex) : ARGB.multiply(tint, quad.bakedColors().color(vertex));
                matrix.transformPosition(quad.position(vertex), pos);
                consumer.addVertex(pos.x(), pos.y(), pos.z(), vertexColor,
                        UVPair.unpackU(packedUv), UVPair.unpackV(packedUv),
                        OverlayTexture.NO_OVERLAY, FULL_LIGHT,
                        normal.x(), normal.y(), normal.z());
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
            cached = new CachedModel(collection, collectQuads(collection));
            CACHE.put(key, cached);
        }
        return cached;
    }

    /** 收集 6 方向 + unculled 的全部 quads（QuadCollection.getQuads(null) 仅返回 unculled）。 */
    private static List<BakedQuad> collectQuads(QuadCollection collection) {
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            quads.addAll(collection.getQuads(direction));
        }
        quads.addAll(collection.getQuads(null));
        return quads;
    }
}
