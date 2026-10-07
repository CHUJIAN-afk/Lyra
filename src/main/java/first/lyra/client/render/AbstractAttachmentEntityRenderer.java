package first.lyra.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import first.lyra.client.config.ClientConfig;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.PathNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import software.bernie.geckolib.core.object.Color;

public abstract class AbstractAttachmentEntityRenderer<T extends AttachmentEntity> implements IAttachmentEntityRenderer<T> {

    protected RenderContext<T> context;

    protected RenderContext<T> createContext(T entity, PathNode visualNode, float partialTick, int packedLight) {
        return new RenderContext<>(entity, visualNode, partialTick, packedLight);
    }

    @Override
    public void render(T entity, PoseStack poseStack, MultiBufferSource bufferSource, float partialTick, int packedLight, PathNode visualNode) {
        context = createContext(entity, visualNode, partialTick, packedLight);
        if (context != null) {
            float alpha = getAlpha();
            context.color(Color.ofRGBA(context.color.getRedFloat(), context.color.getGreenFloat(), context.color.getBlueFloat(), alpha * context.color.getAlphaFloat()));
            poseStack.pushPose();
            if (context.hasTrail()) {
                context.trail.render(poseStack, bufferSource, context);
            }
            modelModify(poseStack, bufferSource);
            poseStack.popPose();
        }
    }

    protected void modelModify(PoseStack poseStack, MultiBufferSource bufferSource) {
        PathNode visualNode = context.visualNode;
        ModelContext model = context.model;
        Quaternionf rotation = new Quaternionf()
                .mul(Axis.YN.rotationDegrees(visualNode.yaw()))
                .mul(Axis.XP.rotationDegrees(visualNode.pitch()))
                .mul(Axis.ZP.rotationDegrees(visualNode.roll()))
                .mul(Axis.YN.rotationDegrees(model.yawOffset))
                .mul(Axis.XP.rotationDegrees(model.pitchOffset))
                .mul(Axis.ZP.rotationDegrees(model.rollOffset));
        poseStack.pushPose();
        poseStack.mulPose(rotation);
        poseStack.scale(model.scaleX, model.scaleY, model.scaleZ);
        poseStack.translate(model.translateX, model.translateY, model.translateZ);
        renderModel(poseStack, bufferSource);
        render(poseStack, bufferSource);
        poseStack.popPose();
    }

    protected void renderModel(PoseStack poseStack, MultiBufferSource bufferSource) {
    }

    protected void render(PoseStack poseStack, MultiBufferSource bufferSource) {
    }

    protected float getAlpha() {
        return ClientConfig.AlphaModify.get() ? getAlphaModify() : 1.0F;
    }

    protected float getAlphaModify() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return 1.0F;
        }
        Vec3 entityPos = context.visualNode.pos();
        Vec3 eyePos = player.getEyePosition(context.partialTick);
        double distance = entityPos.distanceTo(eyePos);
        float minDistance = 0.5F * context.model.alphaDistanceFactor;
        float maxDistance = 4.0F * context.model.alphaDistanceFactor;
        if (distance <= minDistance) {
            return 0.0F;
        }
        if (distance >= maxDistance) {
            return 1.0F;
        }
        return Mth.clamp((float) ((distance - minDistance) / (maxDistance - minDistance)), 0.102F, 1.0F);
    }
}
