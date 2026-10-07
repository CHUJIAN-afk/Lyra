package first.lyra.client.render.geo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

public class AttachmentEntityGeoRenderer<T extends AttachmentEntity> extends GeoObjectRenderer<T> {

    private final AbstractAttachmentEntityGeoRenderer<T> abstractAttachmentEntityGeoRenderer;

    public AttachmentEntityGeoRenderer(AbstractAttachmentEntityGeoRenderer<T> abstractAttachmentEntityGeoRenderer, GeoModel<T> model) {
        super(model);
        this.abstractAttachmentEntityGeoRenderer = abstractAttachmentEntityGeoRenderer;
    }

    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {
        return abstractAttachmentEntityGeoRenderer.getRenderColor(animatable, partialTick, packedLight);
    }

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.objectRenderTranslations = new Matrix4f(poseStack.last().pose());
    }
}
