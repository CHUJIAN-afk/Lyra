package first.lyra.client.render.geo;

import first.lyra.client.render.RenderContext;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.animatable.GeoAnimatable;

import java.util.function.Function;

/**
 * Geo model using the virtual entity resource convention.
 * <p>
 * A base path such as {@code confluence:entity/summon/hornet_baby} resolves to
 * {@code geo/entity/summon/hornet_baby.geo.json},
 * {@code animations/entity/summon/hornet_baby.animation.json} and
 * {@code textures/entity/summon/hornet_baby.png}.
 * </p>
 */
public class AttachmentEntityGeoModel<T extends AttachmentEntity> extends GeoModel<GeoAnimatable> {

    private final Function<RenderContext<T>, ResourceLocation> basePathResolver;
    private ResourceLocation cachedPath;
    private ResourceLocation model;
    private ResourceLocation texture;
    private ResourceLocation animation;

    public AttachmentEntityGeoModel(Function<RenderContext<T>, ResourceLocation> basePathResolver) {
        this.basePathResolver = basePathResolver;
    }

    /**
     * 解析当前基础路径对应的模型、贴图与动画资源，路径没变时沿用缓存。
     */
    public void resolve(RenderContext<T> context) {
        ResourceLocation basePath = basePathResolver.apply(context);
        if (basePath != null && !basePath.equals(cachedPath)) {
            cachedPath = basePath;
            String path = basePath.getPath();
            model = ResourceLocation.fromNamespaceAndPath(basePath.getNamespace(), "geo/" + path + ".geo.json");
            texture = ResourceLocation.fromNamespaceAndPath(basePath.getNamespace(), "textures/" + path + ".png");
            animation = ResourceLocation.fromNamespaceAndPath(basePath.getNamespace(), "animations/" + path + ".animation.json");
        }
    }

    @Override
    public RenderType getRenderType(GeoAnimatable animatable, ResourceLocation texture) {
        return RenderType.entityTranslucentCull(texture);
    }

    @Override
    public ResourceLocation getModelResource(GeoAnimatable animatable) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(GeoAnimatable animatable) {
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(GeoAnimatable animatable) {
        return animation;
    }
}
