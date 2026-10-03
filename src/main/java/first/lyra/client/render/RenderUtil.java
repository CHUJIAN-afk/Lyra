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

/**
 * 通用顶点与贴图渲染工具（26.2 行为对齐）。
 * <p>
 * 顶点统一以<b>完整实体顶点格式</b>（位置/颜色/UV/overlay/光照/法线）
 * 经 {@link VertexConsumer#addVertex(float, float, float, int, float, float, int, int, float, float, float)} 提交。
 * </p>
 */
public final class RenderUtil {

    /** 全亮光照常量（packed）。 */
    public static final int FULL_LIGHT = LightTexture.FULL_BRIGHT;

    private RenderUtil() {
    }

    /**
     * 渲染始终面向相机的贴图（1.21.1 对应 26.2 renderImage；召唤标记等使用）。
     *
     * @param texture      贴图路径
     * @param center       世界坐标中心
     * @param width        宽
     * @param height       高
     * @param bufferSource 渲染缓冲源
     * @param alwaysVisible true = 自发光变体（不受光照变暗）
     * @param tintColor    整体染色 ARGB
     */
    public static void renderImage(ResourceLocation texture, Vec3 center, float width, float height, MultiBufferSource bufferSource, boolean alwaysVisible, int tintColor) {
        Camera camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
        VertexConsumer consumer = bufferSource.getBuffer(LyraRenderTypes.texture(texture, alwaysVisible));
        Vec3 camPos = camera.getPosition();
        // 相机朝向四元数（原版实体名牌同款）：rotation × XN(180)
        Quaternionf rotation = new Quaternionf(camera.rotation()).mul(Axis.XN.rotationDegrees(180), new Quaternionf());
        Matrix4f matrix = new Matrix4f().rotate(rotation).setTranslation((float) (center.x - camPos.x), (float) (center.y - camPos.y), (float) (center.z - camPos.z));
        float halfWidth = width / 2f;
        float halfHeight = height / 2f;
        // 四边形顶点：x,y 偏移 + u,v（26.2 同序：u 随宽度、v 随高度）
        float[][] corners = {
                {-halfWidth, -halfHeight, 0f, 0f},
                {-halfWidth, halfHeight, 0f, 1f},
                {halfWidth, halfHeight, 1f, 1f},
                {halfWidth, -halfHeight, 1f, 0f}
        };
        Vector3f v = new Vector3f();
        for (float[] corner : corners) {
            matrix.transformPosition(corner[0], corner[1], 0f, v);
            consumer.addVertex(v.x, v.y, v.z, tintColor, corner[2], corner[3], OverlayTexture.NO_OVERLAY, FULL_LIGHT, 0f, 0f, 1f);
        }
    }
}
