package first.lyra.client.render.geo;

import first.lyra.client.render.RenderContext;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import java.util.function.Function;

public class AttachmentEntityGeoModel<T extends AttachmentEntity> extends GeoModel<T> {

    private final Function<RenderContext<T>, ResourceLocation> basePathResolver;
    private ResourceLocation cachedPath;
    private ResourceLocation model;
    private ResourceLocation texture;
    private ResourceLocation animation;

    public AttachmentEntityGeoModel(Function<RenderContext<T>, ResourceLocation> basePathResolver) {
        this.basePathResolver = basePathResolver;
    }

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
