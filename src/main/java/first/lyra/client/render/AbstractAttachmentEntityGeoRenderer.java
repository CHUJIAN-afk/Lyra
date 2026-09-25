package first.lyra.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import first.lyra.client.render.trail.TrailConfig;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.PathNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
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

@SuppressWarnings("UnstableApiUsage")
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

    protected GeoRenderContext createContext(T entity, PoseStack poseStack, MultiBufferSource bufferSource, float partialTick, int packedLight, PathNode visualNode) {
        Color color = Color.ofARGB(getAlpha(visualNode.pos(), entity.getOwner().getEyePosition(partialTick)), 1, 1, 1);
        return new GeoRenderContext(entity, visualNode, color, partialTick, packedLight, null, null);
    }

    protected void render(PoseStack poseStack, MultiBufferSource bufferSource) {
    }

    @Override
    public void render(T entity, PoseStack poseStack, MultiBufferSource bufferSource, float partialTick, int packedLight, PathNode visualNode) {
        context = createContext(entity, poseStack, bufferSource, partialTick, packedLight, visualNode);
        if (context != null) {
            model.resolve();
            if (context.trail != null && context.trail.timer > 0) {
                context.trail.render(entity, poseStack, bufferSource, partialTick, visualNode, LyraRenderTypes.getTrail());
            }
            modelModify(poseStack, bufferSource);
        }
    }

    protected float getAlpha(Vec3 visualPos, Vec3 eyePos) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player != null && minecraft.options.getCameraType().isFirstPerson()) {
            double distance = visualPos.distanceTo(eyePos);
            float minDistance = 0.5f;
            float maxDistance = 4.0f;
            if (distance <= minDistance) {
                return 0.0f;
            }
            if (distance >= maxDistance) {
                return 1.0f;
            }
            float alpha = (float) ((distance - minDistance) / (maxDistance - minDistance));
            return Math.clamp(alpha, 0.102f, 1.0f);
        }
        return 1.0f;
    }

    protected void modelModify(PoseStack poseStack, MultiBufferSource bufferSource) {
        GeoRenderContext.ModelContext modelContext = context.modelContext;
        if (modelContext != null) {
            PathNode visualNode = context.visualNode;
            T entity = context.entity;
            float partialTick = context.partialTick;
            int packedLight = context.packedLight;
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf()
                                      .mul(Axis.YN.rotationDegrees(visualNode.yaw()))
                                      .mul(Axis.XP.rotationDegrees(visualNode.pitch()))
                                      .mul(Axis.ZP.rotationDegrees(visualNode.roll()))
                                      .mul(Axis.YN.rotationDegrees(modelContext.yawOffset))
                                      .mul(Axis.XP.rotationDegrees(modelContext.pitchOffset))
                                      .mul(Axis.ZP.rotationDegrees(modelContext.rollOffset)));
            poseStack.scale(modelContext.scaleX, modelContext.scaleY, modelContext.scaleZ);
            poseStack.translate(modelContext.translateX, modelContext.translateY, modelContext.translateZ);
            RenderType renderType = getRenderType(entity, getTextureLocation(entity), bufferSource, partialTick);
            renderer.render(poseStack, entity, bufferSource, renderType, renderType != null ? bufferSource.getBuffer(renderType) : null, packedLight, partialTick);
            render(poseStack, bufferSource);
            poseStack.popPose();
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

    public final class GeoRenderContext {
        public final @NotNull T entity;
        public final @NotNull PathNode visualNode;
        public @NotNull Color color;
        public final float partialTick;
        public int packedLight;
        public @Nullable TrailConfig<T, ?> trail;
        public @Nullable ModelContext modelContext;

        public GeoRenderContext(@NotNull T entity, @NotNull PathNode visualNode, @NotNull Color color, float partialTick, int packedLight, @Nullable TrailConfig<T, ?> trail, @Nullable ModelContext modelContext) {
            this.entity = entity;
            this.visualNode = visualNode;
            this.color = color;
            this.partialTick = partialTick;
            this.packedLight = packedLight;
            this.trail = trail;
            this.modelContext = modelContext;
        }

        public static final class ModelContext {
            public float scaleX = 1.0F;
            public float scaleY = 1.0F;
            public float scaleZ = 1.0F;
            public float translateX = 0.0F;
            public float translateY = 0.0F;
            public float translateZ = 0.0F;
            public float yawOffset = 0.0F;
            public float pitchOffset = 0.0F;
            public float rollOffset = 0.0F;

            public ModelContext scale(float scale) {
                this.scaleX = scale;
                this.scaleY = scale;
                this.scaleZ = scale;
                return this;
            }

            public ModelContext scale(float scaleX, float scaleY, float scaleZ) {
                this.scaleX = scaleX;
                this.scaleY = scaleY;
                this.scaleZ = scaleZ;
                return this;
            }

            public ModelContext translateOffset(float x, float y, float z) {
                this.translateX = x;
                this.translateY = y;
                this.translateZ = z;
                return this;
            }

            public ModelContext rotationOffset(float yaw, float pitch, float roll) {
                this.yawOffset = yaw;
                this.pitchOffset = pitch;
                this.rollOffset = roll;
                return this;
            }
        }
    }
}
