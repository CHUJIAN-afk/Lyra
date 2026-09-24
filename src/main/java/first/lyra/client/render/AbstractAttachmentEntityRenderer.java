package first.lyra.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import first.lyra.client.config.ClientConfig;
import first.lyra.client.render.trail.ModelConfig;
import first.lyra.client.render.trail.TrailConfig;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.PathNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import software.bernie.geckolib.renderer.GeoRenderer;

public abstract class AbstractAttachmentEntityRenderer<T extends AttachmentEntity> implements IAttachmentEntityRenderer<T> {

    protected abstract RenderContext<T> createContext(T entity, float partialTick);

    protected abstract void render(T entity, PoseStack poseStack, MultiBufferSource bufferSource, PathNode visualNode, RenderContext<T> context, float partialTick, int packedLight, float alpha);

    @Override
    public void render(T entity, PoseStack poseStack, MultiBufferSource bufferSource, float partialTick, int packedLight, PathNode visualNode) {
        RenderContext<T> context = createContext(entity, partialTick);
        if (context != null) {
            poseStack.pushPose();
            float alpha = getAlpha(context, visualNode, partialTick);
            if (context.hasTrail()) {
                context.trail.render(entity, poseStack, bufferSource, partialTick, visualNode, LyraRenderTypes.getTrail());
            }
            modelModify(entity, poseStack, bufferSource, visualNode, context, partialTick, packedLight, alpha);
            poseStack.popPose();
        }
    }

    protected float getAlpha(RenderContext<T> context, PathNode visualNode, float partialTick) {
        return ClientConfig.AlphaModify.isTrue() ? getAlphaModify(context, visualNode, partialTick) : 1.0f;
    }

    protected void modelModify(T entity, PoseStack poseStack, MultiBufferSource bufferSource, PathNode visualNode, RenderContext<T> context, float partialTick, int packedLight, float alpha) {
        ModelConfig<T> model = context.model;
        float yawDeg = visualNode.yaw();
        float pitchDeg = visualNode.pitch();
        float rollDeg = visualNode.roll();
        Quaternionf qYaw = Axis.YN.rotationDegrees(yawDeg);
        Quaternionf qPitch = Axis.XP.rotationDegrees(pitchDeg);
        Quaternionf qRoll = Axis.ZP.rotationDegrees(rollDeg);
        Quaternionf qYawOff = Axis.YN.rotationDegrees(model.yawOffset);
        Quaternionf qPitchOff = Axis.XP.rotationDegrees(model.pitchOffset);
        Quaternionf qRollOff = Axis.ZP.rotationDegrees(model.rollOffset);
        Quaternionf rotation = new Quaternionf(qYaw)
                .mul(qPitch)
                .mul(qRoll)
                .mul(qYawOff)
                .mul(qPitchOff)
                .mul(qRollOff);
        float s = model.scale;
        Matrix4f transform = new Matrix4f()
                .rotate(rotation)
                .scale(s, s, s)
                .translate(model.translateX, model.translateY, model.translateZ);
        poseStack.pushPose();
        poseStack.last().pose().mul(transform);
        poseStack.last().normal().mul(new Matrix3f().rotation(rotation));
        render(entity, poseStack, bufferSource, visualNode, context, partialTick, packedLight, alpha);
        poseStack.popPose();
    }

    protected float getAlphaModify(RenderContext<T> config, PathNode visualNode, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return 1.0f;
        }

        Vec3 entityPos = visualNode.pos();
        Vec3 eyePos = player.getEyePosition(partialTick);
        double distance = entityPos.distanceTo(eyePos);

        float minDistance = 0.5f * config.model.alphaDistanceFactor;
        float maxDistance = 4.0f * config.model.alphaDistanceFactor;

        if (distance <= minDistance) {
            return 0.0f;
        }
        if (distance >= maxDistance) {
            return 1.0f;
        }

        float alpha = (float) ((distance - minDistance) / (maxDistance - minDistance));
        return Math.clamp(alpha, 0.102f, 1.0f);
    }
}
