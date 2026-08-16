package first.lyra.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import first.lyra.common.entity.*;
import first.lyra.client.config.ClientConfig;
import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final int FULL_LIGHT = LightCoordsUtil.pack(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_SKY);
    /**
     * 渲染器映射表，按实体类型存储对应的渲染器
     */
    private static final Map<AttachmentEntityType<AttachmentEntity>, IAttachmentEntityRenderer<AttachmentEntity>> renderers = new HashMap<>();

    /**
     * 渲染玩家的所有附件实体。
     *
     * @param players     全部玩家
     * @param camPos      摄像机世界坐标
     * @param poseStack   矩阵栈
     * @param collector   提交节点收集器
     * @param partialTick 部分 tick 插值进度
     */
    public static void render(List<AbstractClientPlayer> players, Vec3 camPos, PoseStack poseStack, SubmitNodeCollector collector, float partialTick) {
        for (AbstractClientPlayer player : players) {
            Set<Map.Entry<AttachmentEntityType<?>, List<AttachmentEntity>>> entries = player.getData(LyraAttachmentRegister.EntityData).getRenderCache().entrySet();
            for (Map.Entry<AttachmentEntityType<?>, List<AttachmentEntity>> entry : entries) {
                IAttachmentEntityRenderer<AttachmentEntity> renderer = renderers.get(entry.getKey());
                List<AttachmentEntity> entities = entry.getValue();
                if (renderer != null && !entities.isEmpty()) {
                    for (AttachmentEntity entity : entities) {
                        entity.setOwner(player);
                        poseStack.pushPose();
                        PathNode renderNode = entity.getRenderNode(partialTick);
                        Vec3 pos = renderNode.pos();
                        poseStack.translate(pos.x() - camPos.x(), pos.y() - camPos.y(), pos.z() - camPos.z());
                        renderer.render(entity, poseStack, collector, partialTick, FULL_LIGHT, renderNode);
                        debugRender(entity, poseStack, renderNode);
                        poseStack.popPose();
                    }
                }
            }
        }
    }

    /**
     * 调试渲染（对齐原版 EntityHitboxDebugRenderer 的 gizmos 方式，全部世界坐标）。
     * <ul>
     *   <li>位置点 + 视线箭头（原版蓝色，从实体当前位置沿朝向 2 格——虚拟实体无眼睛）</li>
     *   <li>实体碰撞箱：OBB 有向包围盒——局部 box 按欧拉角旋转（与模型一致的
     *       qYaw·qPitch·qRoll）后画 12 条边线，白色（原版一致）</li>
     *   <li>方块碰撞箱：白色 cuboid，仅平移不旋转（原版一致）</li>
     * </ul>
     */
    private static void debugRender(AttachmentEntity entity, PoseStack poseStack, PathNode renderNode) {
        if (ClientConfig.DebugMode.isFalse()) {
            return;
        }
        try (Gizmos.TemporaryCollection ignored = Minecraft.getInstance().levelRenderer.collectPerFrameRenderThreadGizmos()) {
            Vec3 pos = renderNode.pos();
            // 位置点 + 视线（原版：蓝色 arrow，从当前位置沿朝向 2 格）
            Gizmos.point(pos, -1, 2.0F);
            float yaw = (float) Math.toRadians(renderNode.yaw());
            float pitch = (float) Math.toRadians(renderNode.pitch());
            Gizmos.arrow(pos, pos.add(new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch)).scale(2.0)), -16776961);
            // 实体碰撞箱：OBB（欧拉角旋转后的 12 条边线，白色——原版一致）
            if (entity instanceof ICollideAttack<?> iCollideAttack && iCollideAttack.renderHitbox()) {
                AABB box = iCollideAttack.getHitbox();
                Quaternionf rotation = new Quaternionf(Axis.YN.rotationDegrees(renderNode.yaw()))
                        .mul(Axis.XP.rotationDegrees(renderNode.pitch()))
                        .mul(Axis.ZP.rotationDegrees(renderNode.roll()));
                Vec3 pos1 = renderNode.pos();
                float[][] raw = {
                        {(float) box.minX, (float) box.minY, (float) box.minZ}, {(float) box.maxX, (float) box.minY, (float) box.minZ},
                        {(float) box.maxX, (float) box.minY, (float) box.maxZ}, {(float) box.minX, (float) box.minY, (float) box.maxZ},
                        {(float) box.minX, (float) box.maxY, (float) box.minZ}, {(float) box.maxX, (float) box.maxY, (float) box.minZ},
                        {(float) box.maxX, (float) box.maxY, (float) box.maxZ}, {(float) box.minX, (float) box.maxY, (float) box.maxZ}
                };
                Vec3[] corners = new Vec3[8];
                for (int i = 0; i < 8; i++) {
                    Vector3f v = new Vector3f(raw[i][0], raw[i][1], raw[i][2]).rotate(rotation);
                    corners[i] = new Vec3(v.x() + pos1.x, v.y() + pos1.y, v.z() + pos1.z);
                }
                int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
                for (int[] e : edges) {
                    Gizmos.line(corners[e[0]], corners[e[1]], -1);
                }
            }
            // 方块碰撞箱（不旋转，仅平移，白色——原版一致）
            if (entity instanceof IBlockCollision<?> iBlockCollision) {
                Gizmos.cuboid(iBlockCollision.getBlockCollisionBox().move(pos.x, pos.y, pos.z), GizmoStyle.stroke(-1));
            }
        }
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
    @SuppressWarnings("unchecked")
    public static <T extends AttachmentEntity> void register(AttachmentEntityType<T> type, IAttachmentEntityRenderer<T> renderer) {
        if (!renderers.containsKey(type)) {
            renderers.put((AttachmentEntityType<AttachmentEntity>) type, (IAttachmentEntityRenderer<AttachmentEntity>) renderer);
        }
    }
}
