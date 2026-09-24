package first.lyra.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.PathNode;
import net.minecraft.client.renderer.MultiBufferSource;

@FunctionalInterface
public interface IAttachmentEntityRenderer<T extends AttachmentEntity> {
    void render(T entity, PoseStack poseStack, MultiBufferSource bufferSource, float partialTick, int packedLight, PathNode renderNode);
}
