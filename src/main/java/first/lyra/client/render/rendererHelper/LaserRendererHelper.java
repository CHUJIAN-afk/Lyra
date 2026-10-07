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

public class LaserRendererHelper {

    private float length = 1.0f;
    private float radiusStart = 0.1f;
    private float radiusEnd = 0.1f;
    private int layers = 4;
    private int segments = 12;
    private int colorRGB = 0xFFFFFFFF;
    private float alpha = 0.8f;
    private float innerRatio = 0.3f;

    private LaserRendererHelper() {
    }

    public static LaserRendererHelper builder() {
        return new LaserRendererHelper();
    }

    public LaserRendererHelper length(float length) {
        this.length = length;
        return this;
    }

    public LaserRendererHelper radius(float start, float end) {
        this.radiusStart = start;
        this.radiusEnd = end;
        return this;
    }

    public LaserRendererHelper layers(int layers) {
        this.layers = Math.max(1, layers);
        return this;
    }

    public LaserRendererHelper segments(int segments) {
        this.segments = Math.max(3, segments);
        return this;
    }

    public LaserRendererHelper color(int argb) {
        this.colorRGB = argb;
        return this;
    }

    public LaserRendererHelper alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public LaserRendererHelper innerRatio(float ratio) {
        this.innerRatio = Math.max(0f, Math.min(1f, ratio));
        return this;
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer consumer = bufferSource.getBuffer(LyraRenderTypes.getTrail());
        Matrix4f pose = poseStack.last()
                .pose();

        int baseR = FastColor.ARGB32.red(colorRGB);
        int baseG = FastColor.ARGB32.green(colorRGB);
        int baseB = FastColor.ARGB32.blue(colorRGB);
        int baseA = Mth.clamp(Math.round(alpha * 255), 0, 255);

        for (int layer = 0; layer < layers; layer++) {

            float layerRatio = layers == 1 ? 0f : (float) layer / (layers - 1);

            float radiusScale = mix(innerRatio, 1.0f, layerRatio);

            float layerAlpha = mix(1.0f, 0.15f, layerRatio);

            renderLayer(consumer, pose, radiusScale, layerAlpha, baseR, baseG, baseB, baseA);
        }
    }

    private void renderLayer(VertexConsumer consumer, Matrix4f pose, float radiusScale, float layerAlpha, int baseR, int baseG, int baseB, int baseA) {

        float rNear = radiusStart * radiusScale;
        float rFar = radiusEnd * radiusScale;

        int a = Math.max(0, Math.min(255, Math.round(baseA * layerAlpha)));
        int vertexColor = FastColor.ARGB32.color(a, baseR, baseG, baseB);

        for (int j = 0; j < segments; j++) {
            float angle1 = (float) (j) / segments * (float) (Math.PI * 2.0);
            float angle2 = (float) (j + 1) / segments * (float) (Math.PI * 2.0);
            float cos1 = (float) Math.cos(angle1), sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2), sin2 = (float) Math.sin(angle2);

            float u = ((float) j + 0.5f) / segments;

            emitVertex(consumer, pose, cos1 * rNear, sin1 * rNear, 0, vertexColor, u, 0f);
            emitVertex(consumer, pose, cos2 * rNear, sin2 * rNear, 0, vertexColor, u, 0f);
            emitVertex(consumer, pose, cos2 * rFar, sin2 * rFar, -length, vertexColor, u, 1f);
            emitVertex(consumer, pose, cos1 * rFar, sin1 * rFar, -length, vertexColor, u, 1f);
        }
    }

    private void emitVertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z, int color, float u, float v) {
        consumer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0, 0, 1);
    }

    private static float mix(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
