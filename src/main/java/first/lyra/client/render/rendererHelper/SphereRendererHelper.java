package first.lyra.client.render.rendererHelper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import first.lyra.client.render.LyraRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class SphereRendererHelper {

    private float radius = 0.3f;
    private int layers = 4;
    private int sides = 12;
    private int colorRGB = 0xFFFFFFFF;
    private float alpha = 0.9f;
    private float innerRatio = 0.4f;

    private SphereRendererHelper() {
    }

    public static SphereRendererHelper builder() {
        return new SphereRendererHelper();
    }

    public SphereRendererHelper radius(float radius) {
        this.radius = radius;
        return this;
    }

    public SphereRendererHelper layers(int layers) {
        this.layers = Math.max(1, layers);
        return this;
    }

    public SphereRendererHelper sides(int sides) {
        this.sides = Math.max(3, sides);
        return this;
    }

    public SphereRendererHelper color(int argb) {
        this.colorRGB = argb;
        return this;
    }

    public SphereRendererHelper alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public SphereRendererHelper innerRatio(float ratio) {
        this.innerRatio = Mth.clamp(ratio, 0f, 1f);
        return this;
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer consumer = bufferSource.getBuffer(LyraRenderTypes.getTrail());
        Matrix4f pose = poseStack.last().pose();

        int baseR = FastColor.ARGB32.red(colorRGB);
        int baseG = FastColor.ARGB32.green(colorRGB);
        int baseB = FastColor.ARGB32.blue(colorRGB);
        int baseA = Mth.clamp(Math.round(alpha * 255), 0, 255);

        for (int layer = 0; layer < layers; layer++) {

            float layerRatio = layers == 1 ? 0f : (float) layer / (layers - 1);

            float radiusScale = mix(innerRatio, 1.0f, layerRatio);

            float layerAlpha = mix(1.0f, 0.15f, layerRatio);
            float r = radius * radiusScale;

            renderLayer(consumer, pose, r, layerAlpha, baseR, baseG, baseB, baseA, sides);
        }
    }

    private void renderLayer(VertexConsumer consumer, Matrix4f pose, float r,
                             float layerAlpha, int baseR, int baseG, int baseB, int baseA, int sides) {
        int a = Mth.clamp(Math.round(baseA * layerAlpha), 0, 255);
        int vertexColor = FastColor.ARGB32.color(a, baseR, baseG, baseB);

        int stacks = Math.max(2, sides / 2);

        for (int ring = 0; ring < stacks; ring++) {
            float phi0 = (float) ring / stacks * (float) Math.PI;
            float phi1 = (float) (ring + 1) / stacks * (float) Math.PI;
            float y0 = (float) Math.cos(phi0);
            float y1 = (float) Math.cos(phi1);
            float xz0 = (float) Math.sin(phi0);
            float xz1 = (float) Math.sin(phi1);

            for (int seg = 0; seg < sides; seg++) {
                float theta0 = (float) seg / sides * (float) (Math.PI * 2.0);
                float theta1 = (float) (seg + 1) / sides * (float) (Math.PI * 2.0);
                float ct0 = (float) Math.cos(theta0), st0 = (float) Math.sin(theta0);
                float ct1 = (float) Math.cos(theta1), st1 = (float) Math.sin(theta1);

                float u0 = (float) seg / sides;
                float u1 = (float) (seg + 1) / sides;
                float v0 = (float) ring / stacks;
                float v1 = (float) (ring + 1) / stacks;

                float nx0y0 = xz0 * ct0, ny0y0 = y0, nz0y0 = xz0 * st0;
                float nx1y0 = xz0 * ct1, ny1y0 = y0, nz1y0 = xz0 * st1;
                float nx1y1 = xz1 * ct1, ny1y1 = y1, nz1y1 = xz1 * st1;
                float nx0y1 = xz1 * ct0, ny0y1 = y1, nz0y1 = xz1 * st0;

                emitVertex(consumer, pose, nx0y0 * r, ny0y0 * r, nz0y0 * r, vertexColor, u0, v0, nx0y0, ny0y0, nz0y0);
                emitVertex(consumer, pose, nx1y0 * r, ny1y0 * r, nz1y0 * r, vertexColor, u1, v0, nx1y0, ny1y0, nz1y0);
                emitVertex(consumer, pose, nx1y1 * r, ny1y1 * r, nz1y1 * r, vertexColor, u1, v1, nx1y1, ny1y1, nz1y1);
                emitVertex(consumer, pose, nx0y1 * r, ny0y1 * r, nz0y1 * r, vertexColor, u0, v1, nx0y1, ny0y1, nz0y1);
            }
        }
    }

    private void emitVertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z, int color, float u, float v, float nx, float ny, float nz) {
        consumer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(nx, ny, nz);
    }

    private static float mix(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
