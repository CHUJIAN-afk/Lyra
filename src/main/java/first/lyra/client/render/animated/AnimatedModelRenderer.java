package first.lyra.client.render.animated;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import first.lyra.client.render.ColorBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 静态动画模型渲染入口，无 GeckoLib 运行时依赖。
 * <p>
 * 用法：
 * <pre>{@code
 * AnimatedModelRenderer.request(Lyra.rl("laser_minigun"))
 *         .animation("shooting", ageTicks + partialTick)
 *         .hideBone("magazine")
 *         .render(poseStack, bufferSource);
 * }</pre>
 * 也提供旧式静态调用：
 * <pre>{@code
 * AnimatedModelRenderer.render(modelId, poseStack, bufferSource, "shooting", ageTicks);
 * }</pre>
 */
public final class AnimatedModelRenderer {

    private AnimatedModelRenderer() {
    }

    public static AnimatedRenderOptions request(ResourceLocation modelId) {
        return new AnimatedRenderOptions(modelId);
    }

    /** 无动画：渲染绑定姿态。 */
    public static boolean render(
            ResourceLocation modelId,
            PoseStack poseStack,
            MultiBufferSource bufferSource
    ) {
        return render(request(modelId), poseStack, bufferSource);
    }

    /** 播放指定动画，动画时间使用 tick 域。 */
    public static boolean render(
            ResourceLocation modelId,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            String animationName,
            float ageTicks
    ) {
        return render(request(modelId).animation(animationName, ageTicks), poseStack, bufferSource);
    }

    public static boolean render(
            AnimatedRenderOptions options,
            PoseStack poseStack,
            MultiBufferSource bufferSource
    ) {
        AnimatedGeoModel model = AnimatedGeoModelManager.INSTANCE.getModel(options.modelId());
        if (model == null) {
            return false;
        }

        model.reset();
        try {
            String animationName = options.animationName();
            if (animationName != null) {
                AnimatedClip clip = AnimatedClipManager.INSTANCE.getClip(options.animationId(), animationName);
                if (clip != null) {
                    clip.sample(options.ageTicks(), model);
                }
            }

            for (String boneName : options.hiddenBones()) {
                AnimatedBone bone = model.getBone(boneName);
                if (bone != null) {
                    bone.setHidden(true);
                }
            }

            ResourceLocation texture = options.texture() != null ? options.texture() : model.getTexture();
            RenderType renderType = options.renderType() != null
                    ? options.renderType()
                    : RenderType.entityTranslucent(texture);

            MultiBufferSource source = bufferSource;
            if (options.color() != -1 || options.alpha() < 1) {
                source = new ColorBufferSource(bufferSource)
                        .setColor(options.color())
                        .setAlpha(options.alpha());
            }

            poseStack.pushPose();
            try {
                poseStack.translate(options.translateX(), options.translateY(), options.translateZ());
                if (options.rotX() != 0) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(options.rotX()));
                }
                if (options.rotY() != 0) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(options.rotY()));
                }
                if (options.rotZ() != 0) {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(options.rotZ()));
                }
                if (options.scale() != 1) {
                    poseStack.scale(options.scale(), options.scale(), options.scale());
                }

                VertexConsumer consumer = source.getBuffer(renderType);
                model.render(poseStack, consumer, options.packedLight(), options.packedOverlay(), options.color() == -1 ? -1 : options.color());
                return true;
            } finally {
                poseStack.popPose();
            }
        } finally {
            model.reset();
        }
    }
}
