package first.lyra.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;

/**
 * 颜色与透明度拦截的 VertexConsumer 包装器。
 * <p>
 * 合并原 {@code AlphaBufferSource.AlphaVertexConsumer}（透明度乘数）与 {@code TintedVertexConsumer}
 * （固定染色）的功能：通过 {@link #setColor(int, int, int, int)} 拦截最终颜色，
 * 通过 {@link #setAlpha(float)} 调整最终透明度（乘数），两者可独立或组合使用。
 * 不设置时透传，无额外开销。
 * </p>
 * <p>
 * 26.2: {@code VertexConsumer} 的 {@code putBakedQuad} 走 11 参 {@code addVertex} → 单参
 * {@code setColor(int)}，两个重载都必须应用拦截，否则染色/透明度失效。
 * </p>
 */
public final class ColorVertexConsumer implements VertexConsumer {

    private final VertexConsumer base;
    private boolean colorSet;
    private int r, g, b, a;
    private boolean alphaSet;
    private float alphaMultiplier = 1.0F;

    private ColorVertexConsumer(VertexConsumer base) {
        this.base = base;
    }

    // ===================== 控制方法 =====================

    /**
     * 拦截最终颜色（RGB 与 alpha 整体替换）。
     *
     * @return this
     */
    public ColorVertexConsumer setColorOverride(int r, int g, int b, int a) {
        this.colorSet = true;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        return this;
    }

    /**
     * 拦截最终透明度（乘数，[0,1]）。颜色保留，仅缩放 alpha 通道。
     *
     * @return this
     */
    public ColorVertexConsumer setAlpha(float alpha) {
        this.alphaSet = true;
        this.alphaMultiplier = alpha;
        return this;
    }

    // ===================== 工厂 =====================

    /** 包装顶点消费者，后续通过 setColor/setAlpha 设置拦截。 */
    public static ColorVertexConsumer wrap(VertexConsumer base) {
        return new ColorVertexConsumer(base);
    }

    /** 便捷工厂：拦截透明度（乘数）。 */
    public static ColorVertexConsumer wrapAlpha(VertexConsumer base, float alpha) {
        return new ColorVertexConsumer(base).setAlpha(alpha);
    }

    /** 便捷工厂：拦截颜色与透明度。 */
    public static ColorVertexConsumer wrapColor(VertexConsumer base, int r, int g, int b, int a) {
        return new ColorVertexConsumer(base).setColorOverride(r, g, b, a);
    }

    // ===================== VertexConsumer =====================

    @Override
    public @NotNull VertexConsumer addVertex(float x, float y, float z) {
        base.addVertex(x, y, z);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setColor(int r0, int g0, int b0, int a0) {
        base.setColor(apply(r0, g0, b0, a0));
        return this;
    }

    // 26.2: putBakedQuad 走 11 参 addVertex → setColor(int)，必须同样应用拦截
    @Override
    public @NotNull VertexConsumer setColor(int color) {
        base.setColor(apply((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF));
        return this;
    }

    @Override
    public @NotNull VertexConsumer setLineWidth(float width) {
        base.setLineWidth(width);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv(float u, float v) {
        base.setUv(u, v);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv1(int u, int v) {
        base.setUv1(u, v);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv2(int u, int v) {
        base.setUv2(u, v);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setNormal(float x, float y, float z) {
        base.setNormal(x, y, z);
        return this;
    }

    // ===================== 内部 =====================

    private int apply(int r0, int g0, int b0, int a0) {
        if (this.colorSet) {
            r0 = this.r;
            g0 = this.g;
            b0 = this.b;
            a0 = this.a;
        }
        if (this.alphaSet) {
            a0 = Math.round(a0 * this.alphaMultiplier);
        }
        return ARGB.color(a0, r0, g0, b0);
    }
}
