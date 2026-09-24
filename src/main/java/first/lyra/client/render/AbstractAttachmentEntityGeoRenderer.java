package first.lyra.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import first.lyra.client.render.trail.ModelConfig;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.PathNode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

import java.util.List;
import java.util.function.Function;

public abstract class AbstractAttachmentEntityGeoRenderer<T extends AttachmentEntity> implements IAttachmentEntityRenderer<T>, GeoRenderer<T> {

    protected final AttachmentEntityGeoModel model;
    protected final AttachmentEntityGeoRenderer renderer;
    protected GeoRenderContext context;

    protected AbstractAttachmentEntityGeoRenderer(ResourceLocation location) {
        this(renderContext -> location);
    }

    protected AbstractAttachmentEntityGeoRenderer(Function<GeoRenderContext, ResourceLocation> resolver) {
        this.model = new AttachmentEntityGeoModel(resolver);
        this.renderer = new AttachmentEntityGeoRenderer(this.model);
    }

    public final class GeoRenderContext {
        public final T entity;
        public final PathNode visualNode;
        public final Color color;
        public final float partialTick;
        public final int packedLight;
        public final RenderContext<T> renderContext;

        public GeoRenderContext(T entity, PathNode visualNode, Color color, float partialTick, int packedLight, RenderContext<T> renderContext) {
            this.entity = entity;
            this.visualNode = visualNode;
            this.color = color;
            this.partialTick = partialTick;
            this.packedLight = packedLight;
            this.renderContext = renderContext;
        }
    }

    protected abstract GeoRenderContext createContext(T entity, PoseStack poseStack, MultiBufferSource bufferSource, float partialTick, int packedLight, PathNode visualNode);

    @Override
    public void render(T entity, PoseStack poseStack, MultiBufferSource bufferSource, float partialTick, int packedLight, PathNode visualNode) {
        context = createContext(entity, poseStack, bufferSource, partialTick, packedLight, visualNode);
        if (context != null) {
            model.resolve();
            if (context.renderContext.hasTrail()) {
                context.renderContext.trail.render(entity, poseStack, bufferSource, partialTick, visualNode, LyraRenderTypes.getTrail());
            }
            modelModify(poseStack, bufferSource);
        }
    }

    protected void modelModify(PoseStack poseStack, MultiBufferSource bufferSource) {
        ModelConfig<T> model = context.renderContext.model;
        PathNode visualNode = context.visualNode;
        T entity = context.entity;
        float partialTick = context.partialTick;
        int packedLight = context.packedLight;
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf()
                                  .mul(Axis.YN.rotationDegrees(visualNode.yaw()))
                                  .mul(Axis.XP.rotationDegrees(visualNode.pitch()))
                                  .mul(Axis.ZP.rotationDegrees(visualNode.roll()))
                                  .mul(Axis.YN.rotationDegrees(model.yawOffset))
                                  .mul(Axis.XP.rotationDegrees(model.pitchOffset))
                                  .mul(Axis.ZP.rotationDegrees(model.rollOffset)));
        poseStack.scale(model.scale, model.scale, model.scale);
        poseStack.translate(model.translateX, model.translateY, model.translateZ);
        RenderType renderType = getRenderType(entity, getTextureLocation(entity), bufferSource, partialTick);
        defaultRender(poseStack, entity, bufferSource, renderType, renderType != null ? bufferSource.getBuffer(renderType) : null, 0.0F, partialTick, packedLight);
        poseStack.popPose();
    }

    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {
        return context.color;
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
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

    public class AttachmentEntityGeoRenderer extends GeoObjectRenderer<T> {

        public AttachmentEntityGeoRenderer(GeoModel<T> model) {
            super(model);
        }

        @Override
        public Color getRenderColor(T animatable, float partialTick, int packedLight) {
            return AbstractAttachmentEntityGeoRenderer.this.getRenderColor(animatable, partialTick, packedLight);
        }

        @Override
        public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
            this.objectRenderTranslations = new Matrix4f(poseStack.last().pose());
        }
    }

    /**
     * Geo model using the virtual entity resource convention.
     * <p>
     * A base path such as {@code confluence:entity/summon/hornet_baby} resolves to
     * {@code geo/entity/summon/hornet_baby.geo.json},
     * {@code animations/entity/summon/hornet_baby.animation.json} and
     * {@code textures/entity/summon/hornet_baby.png}.
     * </p>
     */
    public class AttachmentEntityGeoModel extends GeoModel<T> {

        private final Function<GeoRenderContext, ResourceLocation> basePathResolver;
        private ResourceLocation cachedPath;
        private ResourceLocation model;
        private ResourceLocation texture;
        private ResourceLocation animation;

        public AttachmentEntityGeoModel(Function<GeoRenderContext, ResourceLocation> basePathResolver) {
            this.basePathResolver = basePathResolver;
        }

        /**
         * 解析当前基础路径对应的模型、贴图与动画资源，路径没变时沿用缓存。
         */
        private void resolve() {
            ResourceLocation basePath = basePathResolver.apply(context);
            if (!basePath.equals(cachedPath)) {
                cachedPath = basePath;
                String path = basePath.getPath();
                model = ResourceLocation.fromNamespaceAndPath(basePath.getNamespace(), "geo/" + path + ".geo.json");
                texture = ResourceLocation.fromNamespaceAndPath(basePath.getNamespace(), "textures/" + path + ".png");
                animation = ResourceLocation.fromNamespaceAndPath(basePath.getNamespace(), "animations/" + path + ".animation.json");
            }
        }

        @Override
        public RenderType getRenderType(T animatable, ResourceLocation texture) {
            return RenderType.entityTranslucentCull(texture);
        }

        @Override
        public ResourceLocation getModelResource(T animatable) {
            return model;
        }

        @Override
        public ResourceLocation getTextureResource(T animatable) {
            return texture;
        }

        @Override
        public ResourceLocation getAnimationResource(T animatable) {
            return animation;
        }
    }
}
