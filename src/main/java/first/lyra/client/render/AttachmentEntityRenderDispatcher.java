package first.lyra.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import first.lyra.common.entity.*;
import first.lyra.client.config.ClientConfig;
import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 附件实体渲染调度器，统一管理所有附件实体（仆从、射弹）的渲染。
 * <p>
 * 在 {@code SubmitCustomGeometryEvent} 中调用，遍历玩家的所有附件实体并调用对应的渲染器进行渲染。
 * </p>
 * <p>
 * 26.2: MultiBufferSource 移除,渲染走 {@link SubmitNodeCollector}。
 * </p>
 * <h2>第一人称透明度调整</h2>
 * <p>
 * 当玩家处于第一人称视角时，附件实体可能会遮挡玩家视野。为解决此问题，
 * 根据附件实体与玩家眼睛的距离动态调整透明度。
 * </p>
 */
public class AttachmentEntityRenderDispatcher {

    /**
     * 渲染器映射表，按实体类型存储对应的渲染器
     */
    private static final Map<AttachmentEntityType<?>, IAttachmentEntityRenderer<?>> renderers = new HashMap<>();

    /**
     * 渲染玩家的所有附件实体。
     *
     * @param players      全部玩家
     * @param camPos       摄像机世界坐标
     * @param poseStack    矩阵栈
     * @param collector    提交节点收集器
     * @param partialTick  部分 tick 插值进度
     */
    public static void render(List<AbstractClientPlayer> players, Vec3 camPos, PoseStack poseStack, SubmitNodeCollector collector, float partialTick) {
        for (AbstractClientPlayer player : players) {
            List<AttachmentEntity> entities = player.getData(LyraAttachmentRegister.EntityData)
                    .getRenderCache();
            boolean showHitboxes = Minecraft.getInstance().options.keyDebugShowHitboxes.isDown();
            int packedLight = LightCoordsUtil.FULL_BRIGHT;
            for (AttachmentEntity entity : entities) {
                entity.setOwner(player);
                poseStack.pushPose();
                PathNode renderNode = entity.getRenderNode(partialTick);
                Vec3 pos = renderNode.pos();
                poseStack.translate(pos.x() - camPos.x(), pos.y() - camPos.y(), pos.z() - camPos.z());
                // 渲染实体模型
                IAttachmentEntityRenderer<AttachmentEntity> renderer = getRenderer(entity);
                if (renderer != null) {
                    renderer.render(entity, poseStack, collector, partialTick, packedLight, renderNode);
                }
                if (ClientConfig.DebugMode.isTrue()) {
                    debugRender(poseStack, entity, showHitboxes, renderNode);
                }
                poseStack.popPose();
            }
        }
    }

    private static void debugRender(PoseStack poseStack, AttachmentEntity entity, boolean showHitboxes, PathNode renderNode) {
        // 调试渲染（26.2: renderLineBox 移除,改用 gizmos 体系）
        if (showHitboxes) {
            Matrix4f pose = poseStack.last().pose();
            try (Gizmos.TemporaryCollection ignored = Minecraft.getInstance().levelRenderer.collectPerFrameRenderThreadGizmos()) {
                lineBoxWorld(pose, -0.001, -0.001, -0.001, 0.001, 0.001, 0.001, ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 0.0F));
                poseStack.pushPose();
                poseStack.mulPose(Axis.YN.rotationDegrees(renderNode.yaw()));
                poseStack.mulPose(Axis.XP.rotationDegrees(renderNode.pitch()));
                poseStack.mulPose(Axis.ZP.rotationDegrees(renderNode.roll()));
                Matrix4f rotated = poseStack.last().pose();
                lineBoxWorld(rotated, -0.0001, -0.0001, 0, 0.0001, 0.0001, 2, ARGB.colorFromFloat(1.0F, 0.0F, 0.0F, 1.0F));
                lineBoxWorld(rotated, -0.0001, 0, -0.0001, 0.0001, 0.5, 0.0001, ARGB.colorFromFloat(1.0F, 0.0F, 0.0F, 1.0F));
                if (entity instanceof ICollideAttack<?> iCollideAttack) {
                    if (iCollideAttack.renderHitbox()) {
                        lineBoxWorld(pose, iCollideAttack.getHitbox(), ARGB.colorFromFloat(1.0F, 1.0F, 0.0F, 0.0F));
                    }
                }
                poseStack.popPose();
                if (entity instanceof IBlockCollision<?> iBlockCollision) {
                    lineBoxWorld(pose, iBlockCollision.getBlockCollisionBox(), ARGB.colorFromFloat(1.0F, 0.0F, 1.0F, 0.0F));
                }
            }
        }
    }

    /** 将局部 AABB 经矩阵变换后画 12 条边线（gizmos 世界坐标）。 */
    private static void lineBoxWorld(Matrix4f pose, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int argb) {
        lineBoxWorld(pose, new AABB(minX, minY, minZ, maxX, maxY, maxZ), argb);
    }

    private static void lineBoxWorld(Matrix4f pose, AABB box, int argb) {
        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;
        float[][] raw = {
                {minX, minY, minZ}, {maxX, minY, minZ}, {maxX, minY, maxZ}, {minX, minY, maxZ},
                {minX, maxY, minZ}, {maxX, maxY, minZ}, {maxX, maxY, maxZ}, {minX, maxY, maxZ}
        };
        Vector3f[] corners = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = new Vector3f(raw[i][0], raw[i][1], raw[i][2]);
            pose.transformPosition(corners[i]);
        }
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) {
            Gizmos.line(new Vec3(corners[e[0]].x(), corners[e[0]].y(), corners[e[0]].z()),
                    new Vec3(corners[e[1]].x(), corners[e[1]].y(), corners[e[1]].z()), argb);
        }
    }

    /**
     * 获取附件实体对应的渲染器。
     *
     * @param entity 附件实体实例
     * @return 对应的渲染器，若未注册则返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T extends AttachmentEntity> IAttachmentEntityRenderer<T> getRenderer(T entity) {
        AttachmentEntityType<T> type = (AttachmentEntityType<T>) entity.getType();
        return (IAttachmentEntityRenderer<T>) renderers.get(type);
    }

    /**
     * 注册附件实体类型的渲染器。
     * <p>
     * 每种实体类型只能注册一个渲染器，重复注册将被忽略。
     * </p>
     *
     * @param type     实体类型
     * @param renderer 渲染器实例
     */
    public static <T extends AttachmentEntity> void register(AttachmentEntityType<T> type, IAttachmentEntityRenderer<T> renderer) {
        if (!renderers.containsKey(type)) {
            renderers.put(type, renderer);
        }
    }
}
