package first.lyra.client.render.trail;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import first.lyra.client.render.LyraRenderTypes;
import first.lyra.client.render.RenderContext;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public class DropletTrailContext<T extends AttachmentEntity> extends ConeTrailContext<T> {

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, RenderContext<T> context) {
        List<InterpolatedNode> nodes = buildSmoothNodes(context.entity, context.visualNode, context.partialTick);
        int nodeCount = nodes.size();
        if (nodeCount > 1) {
            renderCone(poseStack, bufferSource, context, nodes);
            renderHead(poseStack, bufferSource, context, nodes.get(0));
        }
    }

    private void renderHead(PoseStack poseStack, MultiBufferSource bufferSource, RenderContext<T> context, InterpolatedNode headNode) {
        VertexConsumer buffer = bufferSource.getBuffer(LyraRenderTypes.getTrail());
        PoseStack.Pose pose = poseStack.last();
        float[] cosArr = getCosArray(resolution);
        float[] sinArr = getSinArray(resolution);

        float headFade = fadeOut.getFade(0);
        float headRadius = maxRadius * (minRadiusRatio + (1 - minRadiusRatio) * headFade);
        int headARGB = packColor(colorFunction.getColor(context.entity, 0, context.partialTick), headFade * (200F / 255F));

        Vec3 renderPos = context.visualNode.pos();
        float hrx = (float) (headNode.pos().x - renderPos.x), hry = (float) (headNode.pos().y - renderPos.y), hrz = (float) (headNode.pos().z - renderPos.z);
        int hemisphereSegments = Math.max(2, resolution / 2);
        Vector3f v1 = new Vector3f(), v2 = new Vector3f(), v3 = new Vector3f(), v4 = new Vector3f();

        for (int lat = 0; lat < hemisphereSegments; lat++) {
            float latAngle1 = (float) (Math.PI / 2 * lat / hemisphereSegments);
            float latAngle2 = (float) (Math.PI / 2 * (lat + 1) / hemisphereSegments);
            float r1 = (float) Math.cos(latAngle1) * headRadius;
            float r2 = (float) Math.cos(latAngle2) * headRadius;
            float h1 = (float) Math.sin(latAngle1) * headRadius;
            float h2 = (float) Math.sin(latAngle2) * headRadius;

            for (int lon = 0; lon < resolution; lon++) {
                float cos1 = cosArr[lon], sin1 = sinArr[lon], cos2 = cosArr[lon + 1], sin2 = sinArr[lon + 1];
                v1.set(cos1 * r1, sin1 * r1, h1).rotate(headNode.rot());
                v2.set(cos2 * r1, sin2 * r1, h1).rotate(headNode.rot());
                v3.set(cos2 * r2, sin2 * r2, h2).rotate(headNode.rot());
                v4.set(cos1 * r2, sin1 * r2, h2).rotate(headNode.rot());

                buffer.addVertex(pose, hrx + v1.x, hry + v1.y, hrz + v1.z).setColor(headARGB).setUv(0F, 0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
                buffer.addVertex(pose, hrx + v2.x, hry + v2.y, hrz + v2.z).setColor(headARGB).setUv(1F, 0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
                buffer.addVertex(pose, hrx + v3.x, hry + v3.y, hrz + v3.z).setColor(headARGB).setUv(1F, 1F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
                buffer.addVertex(pose, hrx + v4.x, hry + v4.y, hrz + v4.z).setColor(headARGB).setUv(0F, 1F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
            }
        }
    }
}
