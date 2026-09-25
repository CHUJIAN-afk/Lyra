package first.lyra.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import first.lyra.client.config.ClientConfig;
import first.lyra.client.dynamicLight.DynamicLightDispatcher;
import first.lyra.common.attachmentEntity.*;
import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttachmentEntityRenderDispatcher {

    private static final Map<AttachmentEntityType<?>, IAttachmentEntityRenderer<?>> renderers = new HashMap<>();

    public static void render(ClientLevel level, Camera camera, PoseStack poseStack, MultiBufferSource bufferSource, float partialTick) {
        List<AbstractClientPlayer> players = level.players();
        for (AbstractClientPlayer player : players) {
            List<AttachmentEntity> entities = player.getData(LyraAttachmentRegister.EntityData).getRenderCache();
            Vec3 cameraPos = camera.getPosition();
            int playerLight = getLightCoords(level, player.getLightProbePosition(partialTick));
            boolean showHitboxes = Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes();
            VertexConsumer debugConsumer = showHitboxes ? bufferSource.getBuffer(RenderType.lines()) : null;
            for (AttachmentEntity entity : entities) {
                poseStack.pushPose();
                PathNode renderNode = entity.getRenderNode(partialTick);
                Vec3 pos = renderNode.pos();
                poseStack.translate(pos.x() - cameraPos.x(), pos.y() - cameraPos.y(), pos.z() - cameraPos.z());
                int lightCoords = Math.max(playerLight, getLightCoords(level, pos));
                // 渲染实体模型
                IAttachmentEntityRenderer<AttachmentEntity> renderer = getRenderer(entity);
                if (renderer != null) {
                    renderer.render(entity, poseStack, bufferSource, partialTick, lightCoords, renderNode);
                }
                if (ClientConfig.DebugMode.isTrue()) {
                    debugRender(poseStack, entity, showHitboxes, renderNode, debugConsumer);
                }
                poseStack.popPose();
            }
        }
    }

    private static int getLightCoords(Level level, Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        int sky = level.getBrightness(LightLayer.SKY, blockPos);
        int block = Math.max(level.getBrightness(LightLayer.BLOCK, blockPos), level.getBlockState(blockPos).getLightEmission(level, blockPos));
        int packed = LightTexture.pack(block, sky);
        return DynamicLightDispatcher.getDynamicLight(pos, packed);
    }

    private static void debugRender(PoseStack poseStack, AttachmentEntity entity, boolean showHitboxes, PathNode renderNode, VertexConsumer debugConsumer) {
        if (showHitboxes) {
            LevelRenderer.renderLineBox(poseStack, debugConsumer, -0.001, -0.001, -0.001, 0.001, 0.001, 0.001, 1.0F, 1.0F, 0.0F, 1.0F);
            poseStack.pushPose();
            poseStack.mulPose(Axis.YN.rotationDegrees(renderNode.yaw()));
            poseStack.mulPose(Axis.XP.rotationDegrees(renderNode.pitch()));
            poseStack.mulPose(Axis.ZP.rotationDegrees(renderNode.roll()));
            LevelRenderer.renderLineBox(poseStack, debugConsumer, -0.0001, -0.0001, 0, 0.0001, 0.0001, 2, 0, 0, 1, 1.0F);
            LevelRenderer.renderLineBox(poseStack, debugConsumer, -0.0001, 0, -0.0001, 0.0001, 0.5, 0.0001, 0, 0, 1, 1.0F);
            if (entity instanceof IEntityCollision<?> iCollideAttack) {
                if (iCollideAttack.renderHitbox()) {
                    LevelRenderer.renderLineBox(poseStack, debugConsumer, iCollideAttack.getHitbox(), 1.0F, 0.0F, 0.0F, 1.0F);
                }
            }
            poseStack.popPose();
            if (entity instanceof IBlockCollision<?> iBlockCollision) {
                LevelRenderer.renderLineBox(poseStack, debugConsumer, iBlockCollision.getBlockCollisionBox(), 0.0F, 1.0F, 0.0F, 1.0F);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends AttachmentEntity> IAttachmentEntityRenderer<T> getRenderer(T entity) {
        AttachmentEntityType<T> type = (AttachmentEntityType<T>) entity.getType();
        return (IAttachmentEntityRenderer<T>) renderers.get(type);
    }

    public static <T extends AttachmentEntity> void register(AttachmentEntityType<T> type, IAttachmentEntityRenderer<T> renderer) {
        if (!renderers.containsKey(type)) {
            renderers.put(type, renderer);
        }
    }
}
