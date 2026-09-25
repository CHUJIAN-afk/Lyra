package first.lyra.client.render.geo;

import com.mojang.blaze3d.vertex.PoseStack;
import first.lyra.client.render.AbstractAttachmentEntityRenderer;
import first.lyra.client.render.RenderContext;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

import java.util.List;
import java.util.function.Function;

/**
 * Geo 附件实体渲染器：在通用渲染器基础上接入 geo 模型本体渲染。
 * <p>
 * 资源路径可直接给定，也可按上下文动态解析（多形态模型）；
 * 不需要 geo 模型的渲染器直接继承 {@link AbstractAttachmentEntityRenderer}。
 * </p>
 */
@SuppressWarnings("UnstableApiUsage")
public abstract class AbstractAttachmentEntityGeoRenderer<T extends AttachmentEntity> extends AbstractAttachmentEntityRenderer<T> implements GeoRenderer<T> {

    protected final AttachmentEntityGeoModel<T> model;
    protected final AttachmentEntityGeoRenderer<T> renderer;

    protected AbstractAttachmentEntityGeoRenderer(ResourceLocation location) {
        this(renderContext -> location);
    }

    protected AbstractAttachmentEntityGeoRenderer(Function<RenderContext<T>, ResourceLocation> resolver) {
        this.model = new AttachmentEntityGeoModel<>(resolver);
        this.renderer = new AttachmentEntityGeoRenderer<>(this, this.model);
    }

    @Override
    protected void renderModel(PoseStack poseStack, MultiBufferSource bufferSource) {
        T entity = context.entity;
        float partialTick = context.partialTick;
        model.resolve(context);
        ResourceLocation textureLocation = getTextureLocation(entity);
        if (textureLocation != null) {
            RenderType renderType = getRenderType(entity, textureLocation, bufferSource, partialTick);
            renderer.render(poseStack, entity, bufferSource, renderType, renderType != null ? bufferSource.getBuffer(renderType) : null, context.packedLight, partialTick);
        }
    }

    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {
        return context.color;
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucentCull(texture);
    }

    @Override
    public GeoModel<T> getGeoModel() {
        return renderer.getGeoModel();
    }

    @Override
    public T getAnimatable() {
        return renderer.getAnimatable();
    }

    @Override
    public List<GeoRenderLayer<T>> getRenderLayers() {
        return renderer.getRenderLayers();
    }

    @Override
    public void fireCompileRenderLayersEvent() {
        renderer.fireCompileRenderLayersEvent();
    }

    @Override
    public boolean firePreRenderEvent(PoseStack poseStack, BakedGeoModel bakedGeoModel, MultiBufferSource multiBufferSource, float partialTick, int packedLight) {
        return renderer.firePreRenderEvent(poseStack, bakedGeoModel, multiBufferSource, partialTick, packedLight);
    }

    @Override
    public void firePostRenderEvent(PoseStack poseStack, BakedGeoModel bakedGeoModel, MultiBufferSource multiBufferSource, float partialTick, int packedLight) {
        renderer.firePostRenderEvent(poseStack, bakedGeoModel, multiBufferSource, partialTick, packedLight);
    }

    @Override
    public void updateAnimatedTextureFrame(T animatable) {
        renderer.updateAnimatedTextureFrame(animatable);
    }
}
