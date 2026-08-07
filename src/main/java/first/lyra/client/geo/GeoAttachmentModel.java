package first.lyra.client.geo;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Geo 附件模型定义，根据 ResourceLocation 自动推导 geo/texture/animation 资源路径。
 * <p>
 * 资源约定（与 GeckoLib 默认路径一致）：
 * <ul>
 *   <li>模型:   assets/{namespace}/geo/{path}.geo.json</li>
 *   <li>纹理:   assets/{namespace}/textures/item/entity/{path}.png</li>
 *   <li>动画:   assets/{namespace}/animations/{path}.animation.json</li>
 * </ul>
 * <p>
 * 通常通过 {@link GeoSideloader#create(ResourceLocation)} 间接获取，
 * 不需要单独持有此类的实例。
 */
public class GeoAttachmentModel extends GeoModel<DummyGeoAnimatable> {

    /**
     * 模型文件路径: geo/{path}.geo.json
     */
    private final ResourceLocation modelResource;

    /**
     * 纹理文件路径: textures/item/entity/{path}.png
     */
    private final ResourceLocation textureResource;

    /**
     * 动画文件路径: animations/{path}.animation.json
     */
    private final ResourceLocation animationResource;

    /**
     * 根据 ResourceLocation 推导三项资源路径。
     *
     * @param location 命名空间 + 路径，如 {@code lyra:test_boss}
     */
    public GeoAttachmentModel(ResourceLocation location) {
        String namespace = location.getNamespace();
        String path = location.getPath();
        this.modelResource = ResourceLocation.fromNamespaceAndPath(namespace, "geo/" + path + ".geo.json");
        this.textureResource = ResourceLocation.fromNamespaceAndPath(namespace, "textures/item/entity/" + path + ".png");
        this.animationResource = ResourceLocation.fromNamespaceAndPath(namespace, "animations/" + path + ".animation.json");
    }

    @Override
    public ResourceLocation getModelResource(DummyGeoAnimatable animatable) {
        return this.modelResource;
    }

    @Override
    public ResourceLocation getTextureResource(DummyGeoAnimatable animatable) {
        return this.textureResource;
    }

    @Override
    public ResourceLocation getAnimationResource(DummyGeoAnimatable animatable) {
        return this.animationResource;
    }
}
