package first.lyra.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

public record TintedVertexConsumer(VertexConsumer base, int r, int g, int b, int a) implements VertexConsumer {
    @Override
    public @NotNull VertexConsumer addVertex(float x, float y, float z) {
        base.addVertex(x, y, z);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setColor(int r0, int g0, int b0, int a0) {
        base.setColor(this.r, this.g, this.b, this.a);
        return this;
    }

    // 26.2: VertexConsumer 接口新增抽象方法;putBakedQuad 走 11 参 addVertex → setColor(int),
    // 必须同样应用固定染色,否则染色失效
    @Override
    public @NotNull VertexConsumer setColor(int color) {
        base.setColor(this.r, this.g, this.b, this.a);
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
}
