package first.lyra.client.render.trail;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import first.lyra.client.render.LyraCustomSubmit;
import first.lyra.client.render.RenderContext;
import first.lyra.client.render.RenderUtil;
import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.entity.PathNode;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 拖尾渲染配置基类。
 * <p>
 * 使用模板方法模式，将渲染逻辑封装在配置类中，子类实现具体渲染。
 * 基类提供共享能力：平滑节点构建、圆周缓存、颜色打包、样板化的渲染上下文与四边形发射。
 * </p>
 * <p>
 * 优化：绕过 PoseStack，直接构造 Matrix4f 并使用 10 参数 addVertex 快速路径，
 * 每四边形从 24 次方法调用降至 4 次。
 * </p>
 *
 * @param <T>    实体类型
 * @param <SELF> 配置类自身类型（用于链式调用）
 */
public abstract class TrailConfig<T extends AttachmentEntity, SELF extends TrailConfig<T, SELF>> {

    // ===================== 基础参数 =====================

    /** 拖尾计时器值，>0 时显示拖尾 */
    public int timer = 0;

    /** 历史节点数量，默认 4 */
    public int historyLength = 4;

    /** 每节点插值分段数，默认 8 */
    public int segmentsPerNode = 8;

    /** 拖尾起始索引，默认 0 */
    public int startIndex = 0;

    /** 基础颜色 RGB */
    public int colorRGB = 0xFF0000;

    /** 颜色函数 */
    public RenderContext.ColorFunction<T> colorFunction = (entity, progress, partialTick) -> colorRGB;

    /** 淡出函数 */
    public RenderContext.FadeFunction fadeOut = progress -> (float) Math.pow(Math.max(0.0f, 1.0f - progress), 1.5);

    /** timeShift 时间缩放魔法数（owner.tickCount + partialTick）× 此值 */
    private static final float TIME_SHIFT_SCALE = 0.015f;

    // ===================== 圆周缓存 =====================

    /** 圆形顶点缓存 */
    protected static final Map<Integer, float[]> COS_CACHE = new HashMap<>();
    protected static final Map<Integer, float[]> SIN_CACHE = new HashMap<>();

    protected static float[] getCosArray(int resolution) {
        return COS_CACHE.computeIfAbsent(resolution, r -> {
            float[] arr = new float[r + 1];
            float delta = (float) (2.0 * Math.PI / r);
            for (int i = 0; i <= r; i++) {
                arr[i] = (float) Math.cos(i * delta);
            }
            return arr;
        });
    }

    protected static float[] getSinArray(int resolution) {
        return SIN_CACHE.computeIfAbsent(resolution, r -> {
            float[] arr = new float[r + 1];
            float delta = (float) (2.0 * Math.PI / r);
            for (int i = 0; i <= r; i++) {
                arr[i] = (float) Math.sin(i * delta);
            }
            return arr;
        });
    }

    // ===================== 链式配置方法 =====================

    @SuppressWarnings("unchecked")
    protected SELF self() {
        return (SELF) this;
    }

    public SELF timer(int timer) {
        this.timer = timer;
        return self();
    }

    public SELF historyLength(int length) {
        this.historyLength = length;
        return self();
    }

    public SELF segmentsPerNode(int segments) {
        this.segmentsPerNode = segments;
        return self();
    }

    public SELF startIndex(int index) {
        this.startIndex = index;
        return self();
    }

    public SELF colorRGB(int color) {
        this.colorRGB = color;
        return self();
    }

    public SELF colorFunction(RenderContext.ColorFunction<T> function) {
        this.colorFunction = function;
        return self();
    }

    public SELF fadeOut(RenderContext.FadeFunction function) {
        this.fadeOut = function;
        return self();
    }

    // ===================== 渲染入口 =====================

    /**
     * 渲染拖尾。模板方法：提交自定义几何后委托 {@link #renderBody}。
     * <p>
     * 26.2: MultiBufferSource 移除,渲染走 {@link SubmitNodeCollector#submitCustomGeometry}。
     * </p>
     */
    public final void render(T entity, PoseStack poseStack, SubmitNodeCollector collector, float partialTick, PathNode visualNode, RenderType renderType) {
        render(entity, poseStack, collector, partialTick, visualNode, renderType, 1.0f);
    }

    /**
     * 渲染拖尾（带透明度包装，1.0 = 不透明）。
     */
    public final void render(T entity, PoseStack poseStack, SubmitNodeCollector collector, float partialTick, PathNode visualNode, RenderType renderType, float alpha) {
        RenderSetup<T> setup = beginRender(entity, poseStack, collector, partialTick, visualNode, renderType);
        if (setup == null) {
            return;
        }
        // 挂 afterTerrain phase：顶点先组装到连续缓冲（emitQuad），回调末尾一次性
        // MemorySegment 批量直写（BLOCK 格式，RenderUtil.writeVertices）
        collector.submitSpecial(RenderPhaseKeys.AFTER_TERRAIN, new LyraCustomSubmit(poseStack.last().copy(), renderType, (pose, buffer) -> {
            this.currentAlpha = alpha;
            this.vertexCount = 0;
            setup.consumer = buffer;
            renderBody(setup);
            RenderUtil.writeVertices(buffer, this.xyzuvData, this.colorData, RenderUtil.FULL_LIGHT, this.vertexCount);
            this.currentAlpha = 1.0F;
        }));
    }

    /** 子类实现的具体渲染逻辑（顶点写入 setup.consumer）。 */
    protected abstract void renderBody(RenderSetup<T> setup);

    /**
     * 渲染样板：一次性算好子类所需的全部上下文。
     * <p>
     * 子类 {@code renderBody} 中直接用 {@link RenderSetup#consumer} 写顶点。
     * 直接从 PoseStack 取出 Matrix4f，绕过后续所有 PoseStack 操作。
     * </p>
     */
    protected final RenderSetup<T> beginRender(T entity, PoseStack poseStack, SubmitNodeCollector collector, float partialTick, PathNode visualNode, RenderType renderType) {
        List<InterpolatedNode> smoothNodes = buildSmoothNodes(entity, visualNode, partialTick);
        if (smoothNodes.size() < 2) {
            return null;
        }
        Matrix4f matrix = new Matrix4f(poseStack.last().pose());
        Vec3 renderPos = visualNode.pos();
        return new RenderSetup<>(entity, collector, poseStack, renderType, matrix, partialTick, renderPos, smoothNodes);
    }

    /**
     * 渲染上下文（渲染样板产物），供子类直接取用。
     */
    protected static final class RenderSetup<T extends AttachmentEntity> {
        public final T entity;
        public final SubmitNodeCollector collector;
        public final PoseStack poseStack;
        public final RenderType renderType;
        public final Matrix4f matrix;
        public final float partialTick;
        public final Vec3 renderPos;
        public final List<InterpolatedNode> smoothNodes;
        /** 当前提交的顶点消费者（由 submitCustomGeometry 回调写入） */
        public VertexConsumer consumer;

        RenderSetup(T entity, SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType, Matrix4f matrix, float partialTick, Vec3 renderPos, List<InterpolatedNode> smoothNodes) {
            this.entity = entity;
            this.collector = collector;
            this.poseStack = poseStack;
            this.renderType = renderType;
            this.matrix = matrix;
            this.partialTick = partialTick;
            this.renderPos = renderPos;
            this.smoothNodes = smoothNodes;
        }

        /** 节点数（含头尾） */
        public int nodeCount() {
            return smoothNodes.size();
        }
    }

    // ===================== 颜色工具 =====================

    /**
     * 将 RGB 颜色与 alpha 打包为 ARGB 顶点色。
     */
    protected static int packColor(int rgb, float alpha) {
        int a = clampByte(alpha * 255f);
        return ARGB.color(a, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    /**
     * 将 RGB 颜色与 alpha、亮度增强打包为 ARGB 顶点色。
     */
    protected static int packColor(int rgb, float alpha, float brightness) {
        int a = clampByte(alpha * 255f);
        int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * brightness));
        int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * brightness));
        int b = Math.min(255, Math.round((rgb & 0xFF) * brightness));
        return ARGB.color(a, r, g, b);
    }

    // ===================== 平滑节点构建 =====================

    protected List<InterpolatedNode> buildSmoothNodes(T entity, PathNode visualNode, float partialTick) {
        ArrayList<PathNode> history = new ArrayList<>(entity.getHistoryNodes());
        history.set(0, visualNode);
        int actualLength = Math.min(history.size(), historyLength);
        if (actualLength < 2) {
            return List.of();
        }
        PathNode[] nodes = new PathNode[actualLength];
        for (int i = 0; i < actualLength; i++) {
            nodes[i] = history.get(i).lerp(history.get(Math.max(0, i - 1)), partialTick);
        }
        int endIndex = nodes.length - 1;
        int startIdx = Math.clamp(startIndex, 0, endIndex - 1);

        List<InterpolatedNode> result = new ArrayList<>((endIndex - startIdx) * segmentsPerNode + 1);
        Quaternionf tempQuat = new Quaternionf();

        for (int i = startIdx; i < endIndex; i++) {
            PathNode p0 = nodes[Math.max(i - 1, startIdx)];
            PathNode p1 = nodes[i];
            PathNode p2 = nodes[i + 1];
            PathNode p3 = nodes[Math.min(i + 2, endIndex)];

            Quaternionf q1 = eulerToQuaternion(p1.yaw(), p1.pitch(), p1.roll());
            Quaternionf q2 = eulerToQuaternion(p2.yaw(), p2.pitch(), p2.roll());

            for (int j = 0; j < segmentsPerNode; j++) {
                float t = ((float) j / segmentsPerNode);
                result.add(catmullRomInterpolate(p0, p1, p2, p3, q1, q2, t, tempQuat));
            }
        }

        PathNode lastNode = nodes[endIndex];
        result.add(new InterpolatedNode(lastNode.pos(), eulerToQuaternion(lastNode.yaw(), lastNode.pitch(), lastNode.roll())));

        return result;
    }

    protected InterpolatedNode catmullRomInterpolate(PathNode p0, PathNode p1, PathNode p2, PathNode p3,
                                                     Quaternionf q1, Quaternionf q2, float t, Quaternionf tempQuat) {
        float t2 = t * t, t3 = t2 * t;
        float f0 = -0.5f * t3 + t2 - 0.5f * t;
        float f1 = 1.5f * t3 - 2.5f * t2 + 1.0f;
        float f2 = -1.5f * t3 + 2.0f * t2 + 0.5f * t;
        float f3 = 0.5f * t3 - 0.5f * t2;

        Vec3 pos = new Vec3(
                p0.pos().x * f0 + p1.pos().x * f1 + p2.pos().x * f2 + p3.pos().x * f3,
                p0.pos().y * f0 + p1.pos().y * f1 + p2.pos().y * f2 + p3.pos().y * f3,
                p0.pos().z * f0 + p1.pos().z * f1 + p2.pos().z * f2 + p3.pos().z * f3
        );

        tempQuat.set(q1).slerp(q2, t);
        return new InterpolatedNode(pos, new Quaternionf(tempQuat));
    }

    protected Quaternionf eulerToQuaternion(float yaw, float pitch, float roll) {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-yaw))
                .rotateX((float) Math.toRadians(pitch))
                .rotateZ((float) Math.toRadians(roll));
    }

    // ===================== 四边形发射（组装到连续缓冲，回调末尾批量直写） =====================

    /**
     * 发射一个四边形（float 坐标版本）：顶点变换后组装到 {@link #xyzuvData}/{@link #colorData}，
     * 由 render 回调末尾经 {@link RenderUtil#writeVertices} 一次性 MemorySegment 批量直写
     * （BLOCK 格式）。consumer 参数保留（子类调用兼容，写入阶段不使用）。
     * alpha（当前透明度）在组装时乘入颜色。
     */
    protected void emitQuad(VertexConsumer consumer, Matrix4f matrix,
                            float x1, float y1, float z1, int c1,
                            float x2, float y2, float z2, int c2,
                            float x3, float y3, float z3, int c3,
                            float x4, float y4, float z4, int c4) {
        ensureVertexCapacity(this.vertexCount + 8);
        Vector3f v = new Vector3f();
        // 正向 + 反向绕序各一遍（管线剔除背面，几何层手动双面）
        matrix.transformPosition(x1, y1, z1, v);
        appendVertex(v.x(), v.y(), v.z(), c1, 0, 0);
        matrix.transformPosition(x2, y2, z2, v);
        appendVertex(v.x(), v.y(), v.z(), c2, 1, 0);
        matrix.transformPosition(x3, y3, z3, v);
        appendVertex(v.x(), v.y(), v.z(), c3, 1, 1);
        matrix.transformPosition(x4, y4, z4, v);
        appendVertex(v.x(), v.y(), v.z(), c4, 0, 1);
        matrix.transformPosition(x4, y4, z4, v);
        appendVertex(v.x(), v.y(), v.z(), c4, 0, 1);
        matrix.transformPosition(x3, y3, z3, v);
        appendVertex(v.x(), v.y(), v.z(), c3, 1, 1);
        matrix.transformPosition(x2, y2, z2, v);
        appendVertex(v.x(), v.y(), v.z(), c2, 1, 0);
        matrix.transformPosition(x1, y1, z1, v);
        appendVertex(v.x(), v.y(), v.z(), c1, 0, 0);
    }

    /** 顶点组装缓冲（每顶点 5 float：x,y,z,u,v），回调末尾批量直写。 */
    private float[] xyzuvData = new float[1024 * 5];
    /** 颜色缓冲（每顶点 1 int ARGB，alpha 已乘入）。 */
    private int[] colorData = new int[1024];
    private int vertexCount;
    private float currentAlpha = 1.0F;

    private void appendVertex(float x, float y, float z, int color, float u, float v) {
        int vertexIndex = this.vertexCount * 5;
        this.xyzuvData[vertexIndex] = x;
        this.xyzuvData[vertexIndex + 1] = y;
        this.xyzuvData[vertexIndex + 2] = z;
        this.xyzuvData[vertexIndex + 3] = u;
        this.xyzuvData[vertexIndex + 4] = v;
        this.colorData[this.vertexCount] = ARGB.multiplyAlpha(color, this.currentAlpha);
        this.vertexCount++;
    }

    private void ensureVertexCapacity(int requiredVertexCount) {
        if (requiredVertexCount * 5 > this.xyzuvData.length) {
            int newVertexCapacity = Math.max(requiredVertexCount, this.xyzuvData.length / 5 * 2);
            float[] newXyzuvData = new float[newVertexCapacity * 5];
            System.arraycopy(this.xyzuvData, 0, newXyzuvData, 0, this.vertexCount * 5);
            this.xyzuvData = newXyzuvData;
            int[] newColorData = new int[newVertexCapacity];
            System.arraycopy(this.colorData, 0, newColorData, 0, this.vertexCount);
            this.colorData = newColorData;
        }
    }

    // ===================== 插值节点记录 =====================

    public record InterpolatedNode(Vec3 pos, Quaternionf rot) {
    }

    // ===================== 工具 =====================

    private static int clampByte(float v) {
        return Math.clamp(Math.round(v), 0, 255);
    }
}
