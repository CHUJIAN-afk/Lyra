package first.lyra.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import first.lyra.common.attachmentEntity.PathNode;
import first.lyra.common.entity.IPathEntity;
import net.minecraft.world.phys.Vec3;

public class EntityPathRender {

    public static void translate(IPathEntity entity, PoseStack poseStack, float partialTick) {
        Vec3 offset = entity.getRenderNode(partialTick).pos().subtract(entity.getSyncEntity().getPosition(partialTick));
        poseStack.translate(offset.x, offset.y, offset.z);
    }

    public static void rotate(IPathEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.mulPose(entity.getRenderNode(partialTick).toQuaternion());
    }

    /** 在已有 yaw/pitch 变换后，绕模型局部 +Z 轴施加翻滚。 */
    public static void roll(IPathEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getRenderNode(partialTick).roll()));
    }

    /** LivingEntityRenderer 已施加 180° - yaw，模型正面朝向局部 -Z。 */
    public static void rotateLiving(IPathEntity entity, PoseStack poseStack, float partialTick) {
        PathNode node = entity.getRenderNode(partialTick);
        poseStack.mulPose(Axis.XN.rotationDegrees(node.pitch()));
        poseStack.mulPose(Axis.ZN.rotationDegrees(node.roll()));
    }
}
