package first.lyra.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class RenderUtil {

    public static final int FULL_LIGHT = LightTexture.FULL_BRIGHT;

    private RenderUtil() {
    }

    public static void renderImage(ResourceLocation texture, Vec3 center, float width, float height, MultiBufferSource bufferSource, boolean alwaysVisible, int tintColor) {
        Camera camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
        VertexConsumer consumer = bufferSource.getBuffer(LyraRenderTypes.texture(texture, alwaysVisible));
        Vec3 camPos = camera.getPosition();

        Quaternionf rotation = new Quaternionf(camera.rotation()).mul(Axis.XN.rotationDegrees(180), new Quaternionf());
        Matrix4f matrix = new Matrix4f().rotate(rotation).setTranslation((float) (center.x - camPos.x), (float) (center.y - camPos.y), (float) (center.z - camPos.z));
        float halfWidth = width / 2f;
        float halfHeight = height / 2f;

        float[][] corners = {
                {-halfWidth, -halfHeight, 0f, 0f},
                {-halfWidth, halfHeight, 0f, 1f},
                {halfWidth, halfHeight, 1f, 1f},
                {halfWidth, -halfHeight, 1f, 0f}
        };
        Vector3f v = new Vector3f();
        for (float[] corner : corners) {
            matrix.transformPosition(corner[0], corner[1], 0f, v);
            consumer.addVertex(v.x, v.y, v.z).setColor(tintColor).setUv(corner[2], corner[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_LIGHT).setNormal(0f, 0f, 1f).endVertex();
        }
    }
}
