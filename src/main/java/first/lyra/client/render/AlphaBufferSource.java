package first.lyra.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

/**
 * 透明度调整工具（26.2 重构）。
 * <p>
 * 26.2: MultiBufferSource 移除,不再存在缓冲源包装器。
 * 改为包装 {@link VertexConsumer}：在 submitCustomGeometry 的渲染回调内调用 {@link #wrap}。
 * </p>
 */
public class AlphaBufferSource {

    private float alpha = 1.0f;

    public AlphaBufferSource() {
    }

    /**
     * 设置全局透明度。
     *
     * @param alpha 透明度 [0, 1]
     */
    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    /**
     * 获取当前透明度。
     */
    public float getAlpha() {
        return alpha;
    }

    /**
     * 包装顶点消费者，应用当前透明度。
     */
    public @NotNull VertexConsumer wrap(VertexConsumer inner) {
        return wrap(inner, alpha);
    }

    /**
     * 包装顶点消费者，应用指定透明度。
     */
    public static @NotNull VertexConsumer wrap(VertexConsumer inner, float alpha) {
        if (alpha >= 1.0f) {
            return inner;
        }
        return new AlphaVertexConsumer(inner, alpha);
    }

    /**
     * 带透明度调整的 VertexConsumer 包装器。
     */
    private record AlphaVertexConsumer(VertexConsumer inner, float alpha) implements VertexConsumer {

        @Override
        public @NotNull VertexConsumer addVertex(float x, float y, float z) {
            inner.addVertex(x, y, z);
            return this;
        }

        @Override
        public @NotNull VertexConsumer setColor(int r, int g, int b, int a) {
            // 应用透明度
            return inner.setColor(r, g, b, (int) (a * alpha));
        }

        // 26.2: VertexConsumer 接口新增抽象方法
        @Override
        public @NotNull VertexConsumer setColor(int color) {
            return inner.setColor(color);
        }

        @Override
        public @NotNull VertexConsumer setLineWidth(float width) {
            return inner.setLineWidth(width);
        }

        @Override
        public @NotNull VertexConsumer setUv(float u, float v) {
            inner.setUv(u, v);
            return this;
        }

        @Override
        public @NotNull VertexConsumer setUv1(int u, int v) {
            inner.setUv1(u, v);
            return this;
        }

        @Override
        public @NotNull VertexConsumer setUv2(int u, int v) {
            inner.setUv2(u, v);
            return this;
        }

        @Override
        public @NotNull VertexConsumer setNormal(float x, float y, float z) {
            inner.setNormal(x, y, z);
            return this;
        }
    }
}
