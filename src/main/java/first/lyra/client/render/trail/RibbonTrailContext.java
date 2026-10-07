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

public class RibbonTrailContext<T extends AttachmentEntity> extends TrailContext<T> {

    public float upOffset = 0F;

    public float downOffset = 0F;

    public RenderContext.AlphaBoostFunction<T> tipAlphaBoost = (entity, progress) -> (1 - progress) * 20;

    public RenderContext.BrightnessBoostFunction<T> tipBrightnessBoost = (entity, progress) -> (1 - (progress * 0.5F));

    public RibbonTrailContext<T> upOffset(float upOffset) {
        this.upOffset = upOffset;
        return this;
    }

    public RibbonTrailContext<T> downOffset(float downOffset) {
        this.downOffset = downOffset;
        return this;
    }

    public RibbonTrailContext<T> tipAlphaBoost(RenderContext.AlphaBoostFunction<T> function) {
        this.tipAlphaBoost = function;
        return this;
    }

    public RibbonTrailContext<T> tipBrightnessBoost(RenderContext.BrightnessBoostFunction<T> function) {
        this.tipBrightnessBoost = function;
        return this;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, RenderContext<T> context) {
        List<InterpolatedNode> nodes = buildSmoothNodes(context.entity, context.visualNode, context.partialTick);
        int nodeCount = nodes.size();
        if (nodeCount > 1) {
            VertexConsumer buffer = bufferSource.getBuffer(LyraRenderTypes.getTrail());
            PoseStack.Pose pose = poseStack.last();
            Vec3 renderPos = context.visualNode.pos();
            Vector3f currTip = new Vector3f(), currBase = new Vector3f(), prevTip = new Vector3f(), prevBase = new Vector3f();
            for (int i = 0; i < nodeCount - 1; i++) {
                InterpolatedNode curr = nodes.get(i);
                InterpolatedNode prev = nodes.get(i + 1);
                float currProgress = (float) i / (nodeCount - 1);
                float prevProgress = (float) (i + 1) / (nodeCount - 1);

                currTip.set(0, 0, upOffset).rotate(curr.rot());
                currBase.set(0, 0, downOffset).rotate(curr.rot());
                prevTip.set(0, 0, upOffset).rotate(prev.rot());
                prevBase.set(0, 0, downOffset).rotate(prev.rot());

                int currColorRGB = colorFunction.getColor(context.entity, currProgress, context.partialTick);
                int prevColorRGB = colorFunction.getColor(context.entity, prevProgress, context.partialTick);
                float currBright = tipBrightnessBoost.getBoost(context.entity, currProgress);
                float prevBright = tipBrightnessBoost.getBoost(context.entity, prevProgress);
                float currAlphaBoost = tipAlphaBoost.getBoost(context.entity, currProgress);
                float prevAlphaBoost = tipAlphaBoost.getBoost(context.entity, prevProgress);
                int currTipColor = packColor(currColorRGB, Math.max(0F, 1F - currProgress) * 0.1F * currAlphaBoost, currBright);
                int currBaseColor = packColor(currColorRGB, Math.max(0F, 1F - currProgress * 2.5F) * 0.04F * currAlphaBoost, currBright);
                int prevTipColor = packColor(prevColorRGB, Math.max(0F, 1F - prevProgress) * 0.1F * prevAlphaBoost, prevBright);
                int prevBaseColor = packColor(prevColorRGB, Math.max(0F, 1F - prevProgress * 2.5F) * 0.04F * prevAlphaBoost, prevBright);

                float crx = (float) (curr.pos().x - renderPos.x), cry = (float) (curr.pos().y - renderPos.y), crz = (float) (curr.pos().z - renderPos.z);
                float prx = (float) (prev.pos().x - renderPos.x), pry = (float) (prev.pos().y - renderPos.y), prz = (float) (prev.pos().z - renderPos.z);

                buffer.addVertex(pose, crx + currTip.x, cry + currTip.y, crz + currTip.z).setColor(currTipColor).setUv(0F, 0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
                buffer.addVertex(pose, crx + currBase.x, cry + currBase.y, crz + currBase.z).setColor(currBaseColor).setUv(1F, 0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
                buffer.addVertex(pose, prx + prevBase.x, pry + prevBase.y, prz + prevBase.z).setColor(prevBaseColor).setUv(1F, 1F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
                buffer.addVertex(pose, prx + prevTip.x, pry + prevTip.y, prz + prevTip.z).setColor(prevTipColor).setUv(0F, 1F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
            }
        }
    }
}
