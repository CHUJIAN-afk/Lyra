package first.lyra.client.render.rendererHelper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import first.lyra.client.render.LyraRenderTypes;
import first.lyra.client.render.VertexAssembler;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 激光/光柱渲染器（链式 MinionWeaponItemBuilder）。
 * <p>
 * 在两点之间绘制多层同心圆柱壳，通过 <b>alpha 梯度叠加</b>实现体积雾感：
 * 内层窄而实（高 alpha，构成锐利核心），外层宽而散（低 alpha，构成泛光边缘）。
 * 多层标准半透明叠加即得到“实心核心 + 散开边缘”的视觉，类似旧版“内核+外晕”思路的泛化。
 * </p>
 * <p>
 * 使用原版 {@code item_translucent} 渲染类型（{@link LyraRenderTypes#TRAIL}），
 * 该类型在原版与光影（Iris/Oculus）环境下效果完全一致。
 * 所有视觉效果在 Java 侧预乘进顶点色，着色器只读取顶点色与位置变换。
 * </p>
 * <p>
 * 坐标约定：激光沿 -Z 方向铺设（z=0 为近端起点，z=-length 为远端终点），
 * 与现有渲染器的 {@code rotationOffset(180,0,0)} 翻转约定一致。半径在 XY 平面内展开。
 * </p>
 *
 * <pre>{@code
 * LaserRendererHelper.builder()
 *     .length(5.0f)
 *     .radius(0.15f, 0.05f)   // 近端半径, 远端半径
 *     .layers(4)
 *     .segments(12)
 *     .color(0xFF5599FF)
 *     .alpha(0.8f)
 *     .render(poseStack, bufferSource);
 * }</pre>
 */
public class LaserRendererHelper {

    // ===================== 参数 =====================
    private float length = 1.0f;
    private float radiusStart = 0.1f;   // 近端(z=0)半径
    private float radiusEnd = 0.1f;     // 远端(z=-length)半径
    private int layers = 4;
    private int segments = 12;
    private int colorRGB = 0xFFFFFFFF;
    private float alpha = 0.8f;
    private float innerRatio = 0.3f;    // 最内层相对半径(0~1)
    private boolean caps = true;        // 是否闭合两端端面（默认闭合）

    /** 全亮光照常量（消除每顶点 pack 调用）。 */
    private static final int FULL_LIGHT = LightCoordsUtil.pack(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_SKY);

    /** 位置预变换复用（避免每顶点分配）。 */
    private final Vector3f scratch = new Vector3f();

    /** 顶点批量组装器（回调末尾 MemorySegment 直写）。 */
    private final VertexAssembler assembler = new VertexAssembler();

    private LaserRendererHelper() {
    }

    /**
     * 创建 MinionWeaponItemBuilder 实例。
     */
    public static LaserRendererHelper builder() {
        return new LaserRendererHelper();
    }

    // -------------------- 链式参数 --------------------

    /**
     * 激光长度（沿 -Z 方向）。
     */
    public LaserRendererHelper length(float length) {
        this.length = length;
        return this;
    }

    /**
     * 两端半径：start 为近端(z=0)，end 为远端(z=-length)。
     */
    public LaserRendererHelper radius(float start, float end) {
        this.radiusStart = start;
        this.radiusEnd = end;
        return this;
    }

    /**
     * 同心壳层数，越多体积雾感越强（建议 3~8）。
     */
    public LaserRendererHelper layers(int layers) {
        this.layers = Math.max(1, layers);
        return this;
    }

    /**
     * 圆周分段数，越多越圆滑（建议 8~16）。
     */
    public LaserRendererHelper segments(int segments) {
        this.segments = Math.max(3, segments);
        return this;
    }

    /**
     * 颜色 ARGB（如 0xFF5599FF）。
     */
    public LaserRendererHelper color(int argb) {
        this.colorRGB = argb;
        return this;
    }

    /**
     * 整体基础透明度 0~1。
     */
    public LaserRendererHelper alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    /**
     * 最内层相对半径 0~1（内层越细，核心越锐利）。
     */
    public LaserRendererHelper innerRatio(float ratio) {
        this.innerRatio = Math.clamp(ratio, 0f, 1f);
        return this;
    }

    /**
     * 是否闭合两端端面（默认 true = 闭合）。
     * <p>
     * 默认补画近端(z=0)与远端(z=-length)两个实心圆盘（最外层半径 + 完整 alpha）；
     * 长激光如需开口管效果可传 false 关闭。
     * </p>
     */
    public LaserRendererHelper caps(boolean caps) {
        this.caps = caps;
        return this;
    }

    // -------------------- 渲染 --------------------
    // 26.2: MultiBufferSource 移除,渲染走 submitCustomGeometry（顶点组装后批量直写）
    public void render(PoseStack poseStack, SubmitNodeCollector collector) {
        collector.submitCustomGeometry(poseStack, LyraRenderTypes.TRAIL, (pose, consumer) -> {
            // 必须用回调的 pose 快照(提交时 copy),不能捕获 poseStack.last()——提交延迟执行,
            // poseStack 随后会被 popPose/mulPose 修改,捕获的矩阵会指向错误位置
            Matrix4f poseMatrix = pose.pose();
            this.assembler.clear();

            // 基础颜色分量：RGB 全层一致（保证圆柱连续，不压黑接缝），仅 alpha 按层渐变
            int baseR = ARGB.red(colorRGB);
            int baseG = ARGB.green(colorRGB);
            int baseB = ARGB.blue(colorRGB);
            int baseA = Math.clamp(Math.round(alpha * 255), 0, 255);

            for (int layer = 0; layer < layers; layer++) {
                // 层级比例: 0=最内层, 1=最外层
                float layerRatio = layers == 1 ? 0f : (float) layer / (layers - 1);
                // 该层半径系数: 内层(innerRatio) ~ 外层(1.0)
                float radiusScale = mix(innerRatio, 1.0f, layerRatio);
                // 层级 alpha：内层高（实核心），外层低（散边缘）—— alpha 梯度叠加出体积雾
                // 内层 1.0，外层 0.15，多层标准半透明叠加即得到“实心核心 + 散开泛光边缘”
                float layerAlpha = mix(1.0f, 0.15f, layerRatio);

                renderLayer(consumer, poseMatrix, radiusScale, layerAlpha, baseR, baseG, baseB, baseA);
            }
            if (caps) {
                renderCaps(consumer, poseMatrix, baseR, baseG, baseB, baseA);
            }
            // 跨几何体共享 buffer（同 RenderType 合并绘制）：顶点数对齐到 12 的倍数
            // （QUADS 4 顶点/四边形 与 TRIANGLES 3 顶点/三角形 的公倍数），
            // 末尾补退化顶点避免余数与下一个几何体组成错误图元
            int remainder = this.assembler.getVertexCount() % 12;
            if (remainder != 0) {
                this.assembler.duplicateLast(12 - remainder);
            }
            this.assembler.write(consumer, FULL_LIGHT);
        });
    }

    /**
     * 渲染单层圆柱壳。
     * <p>
     * 每层所有顶点共享同一颜色：RGB = 基础色（全层一致，圆柱连续无接缝断裂），
     * alpha = 基础 alpha × 层级 alpha（内层实、外层散）。体积雾感由多层 alpha 梯度叠加产生，
     * 不再用 per-segment 径向衰减（那会压黑接缝导致面片断裂）。
     * </p>
     */
    private void renderLayer(VertexConsumer consumer, Matrix4f pose, float radiusScale, float layerAlpha, int baseR, int baseG, int baseB, int baseA) {
        // 近端(z=0)半径, 远端(z=-length)半径
        float rNear = radiusStart * radiusScale;
        float rFar = radiusEnd * radiusScale;

        // 该层统一顶点色：RGB 不衰减，alpha 按层渐变
        int a = Math.clamp(Math.round(baseA * layerAlpha), 0, 255);
        int vertexColor = ARGB.color(a, baseR, baseG, baseB);

        for (int j = 0; j < segments; j++) {
            float angle1 = (float) (j) / segments * (float) (Math.PI * 2.0);
            float angle2 = (float) (j + 1) / segments * (float) (Math.PI * 2.0);
            float cos1 = (float) Math.cos(angle1), sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2), sin2 = (float) Math.sin(angle2);

            // 周向 u 仅作纹理坐标占位（薄着色器不依赖）
            float u = ((float) j + 0.5f) / segments;

            // 四个顶点构成一个纵向四边形(沿轴向): 近角1, 近角2, 远角2, 远角1
            emitVertex(consumer, pose, cos1 * rNear, sin1 * rNear, 0, vertexColor, u, 0f);
            emitVertex(consumer, pose, cos2 * rNear, sin2 * rNear, 0, vertexColor, u, 0f);
            emitVertex(consumer, pose, cos2 * rFar, sin2 * rFar, -length, vertexColor, u, 1f);
            emitVertex(consumer, pose, cos1 * rFar, sin1 * rFar, -length, vertexColor, u, 1f);
        }
    }

    /**
     * 渲染两端端面（闭合圆盘）。
     * <p>
     * 以最外层半径（radiusStart/radiusEnd）+ 完整 alpha 画实心圆盘：
     * 圆心一个顶点 + 每 segment 两个圆周顶点构成 fan。渲染类型为双面
     * （TRANSLUCENT_NO_CULL），绕序无需严格；近端反绕、远端顺绕保证双面视觉一致。
     * </p>
     */
    private void renderCaps(VertexConsumer consumer, Matrix4f pose, int baseR, int baseG, int baseB, int baseA) {
        int vertexColor = ARGB.color(baseA, baseR, baseG, baseB);

        // 近端面 (z=0)
        emitVertex(consumer, pose, 0, 0, 0, vertexColor, 0.5f, 0.5f);
        for (int j = 0; j < segments; j++) {
            float angle1 = (float) (j) / segments * (float) (Math.PI * 2.0);
            float angle2 = (float) (j + 1) / segments * (float) (Math.PI * 2.0);
            float cos1 = (float) Math.cos(angle1), sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2), sin2 = (float) Math.sin(angle2);
            emitVertex(consumer, pose, cos1 * radiusStart, sin1 * radiusStart, 0, vertexColor, 0.5f, 0f);
            emitVertex(consumer, pose, cos2 * radiusStart, sin2 * radiusStart, 0, vertexColor, 0.5f, 0f);
        }

        // 远端面 (z=-length)
        emitVertex(consumer, pose, 0, 0, -length, vertexColor, 0.5f, 0.5f);
        for (int j = 0; j < segments; j++) {
            float angle1 = (float) (j) / segments * (float) (Math.PI * 2.0);
            float angle2 = (float) (j + 1) / segments * (float) (Math.PI * 2.0);
            float cos1 = (float) Math.cos(angle1), sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2), sin2 = (float) Math.sin(angle2);
            emitVertex(consumer, pose, cos2 * radiusEnd, sin2 * radiusEnd, -length, vertexColor, 0.5f, 1f);
            emitVertex(consumer, pose, cos1 * radiusEnd, sin1 * radiusEnd, -length, vertexColor, 0.5f, 1f);
        }
    }

    /** 提交单个顶点：位置经矩阵预变换（复用 scratch），组装到连续缓冲（BLOCK 格式批量直写）。 */
    private void emitVertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z, int color, float u, float v) {
        scratch.set(x, y, z);
        pose.transformPosition(scratch);
        this.assembler.addVertex(scratch.x(), scratch.y(), scratch.z(), color, u, v);
    }

    private static float mix(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
