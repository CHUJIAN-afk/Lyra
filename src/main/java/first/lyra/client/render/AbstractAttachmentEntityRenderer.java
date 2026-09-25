package first.lyra.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import first.lyra.client.config.ClientConfig;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.PathNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import software.bernie.geckolib.util.Color;

/**
 * 附件实体渲染器基类。
 * <p>
 * 负责构建 {@link RenderContext}、写入颜色、绘制拖尾并施加模型变换，
 * 子类只需在 {@code createContext} 里链式配置上下文，并实现 {@link #render}（附加渲染）
 * 或 {@link #renderModel}（模型本体）。
 * </p>
 */
public abstract class AbstractAttachmentEntityRenderer<T extends AttachmentEntity> implements IAttachmentEntityRenderer<T> {

    protected RenderContext<T> context;

    /** 构建当帧渲染上下文：子类调用 super 后链式追加配置 */
    protected RenderContext<T> createContext(T entity, PathNode visualNode, float partialTick, int packedLight) {
        return new RenderContext<>(entity, visualNode, partialTick, packedLight);
    }

    @Override
    public void render(T entity, PoseStack poseStack, MultiBufferSource bufferSource, float partialTick, int packedLight, PathNode visualNode) {
        context = createContext(entity, visualNode, partialTick, packedLight);
        if (context == null) {
            return;
        }
        float alpha = getAlpha();
        context.color(Color.ofARGB(alpha * context.color.getAlphaFloat(), context.color.getRedFloat(), context.color.getGreenFloat(), context.color.getBlueFloat()));
        poseStack.pushPose();
        if (context.hasTrail()) {
            context.trail.render(poseStack, bufferSource, context);
        }
        modelModify(poseStack, bufferSource);
        poseStack.popPose();
    }

    /** 施加视觉节点朝向与模型偏移，随后依次渲染模型本体与附加内容 */
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

    /** 模型本体渲染（geo 渲染器在此提交模型），默认不渲染 */
    protected void renderModel(PoseStack poseStack, MultiBufferSource bufferSource) {
    }

    /** 附加渲染，默认不渲染 */
    protected void render(PoseStack poseStack, MultiBufferSource bufferSource) {
    }

    protected float getAlpha() {
        return ClientConfig.AlphaModify.isTrue() ? getAlphaModify() : 1.0F;
    }

    /** 第一人称下按距离淡出，避免贴脸时模型糊住视野 */
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
        return Math.clamp((float) ((distance - minDistance) / (maxDistance - minDistance)), 0.102F, 1.0F);
    }
}
