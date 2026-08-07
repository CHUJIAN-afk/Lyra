package first.lyra.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

/**
 * 带透明度调整的 MultiBufferSource 包装器。
 * <p>
 * 所有通过此缓冲源获取的 VertexConsumer 都会应用指定的透明度。
 * </p>
 */
public class AlphaBufferSource implements MultiBufferSource {

    private final MultiBufferSource inner;
    private float alpha = 1.0f;

    public AlphaBufferSource(MultiBufferSource inner) {
        this.inner = inner;
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

    @Override
    public @NotNull VertexConsumer getBuffer(@NotNull RenderType renderType) {
        VertexConsumer innerConsumer = inner.getBuffer(renderType);
        if (alpha >= 1.0f) {
            return innerConsumer;
        }
        // 返回一个包装的 VertexConsumer，在设置颜色时应用透明度
        return new AlphaVertexConsumer(innerConsumer, alpha);
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

        @Override
        public @NotNull VertexConsumer misc(@NotNull VertexFormatElement element, int @NotNull ... rawData) {
            inner.misc(element, rawData);
            return this;
        }
    }
}