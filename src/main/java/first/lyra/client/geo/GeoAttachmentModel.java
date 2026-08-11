package first.lyra.client.geo;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/**
 * Geo 附件模型定义，根据 Identifier 自动推导 geo/texture/animation 资源路径。
 * <p>
 * 资源约定（与 GeckoLib 默认路径一致）：
 * <ul>
 *   <li>模型:   assets/{namespace}/geo/{path}.geo.json</li>
 *   <li>纹理:   assets/{namespace}/textures/item/entity/{path}.png</li>
 *   <li>动画:   assets/{namespace}/animations/{path}.animation.json</li>
 * </ul>
 * <p>
 * 通常通过 {@link GeoSideloader#create(Identifier)} 间接获取，
 * 不需要单独持有此类的实例。
 */
public class GeoAttachmentModel extends GeoModel<DummyGeoAnimatable> {

    /**
     * 模型文件路径: geo/{path}.geo.json
     */
    private final Identifier modelResource;

    /**
     * 纹理文件路径: textures/item/entity/{path}.png
     */
    private final Identifier textureResource;

    /**
     * 动画文件路径: animations/{path}.animation.json
     */
    private final Identifier animationResource;

    /**
     * 根据 Identifier 推导三项资源路径。
     *
     * @param location 命名空间 + 路径，如 {@code lyra:test_boss}
     */
    public GeoAttachmentModel(Identifier location) {
        String namespace = location.getNamespace();
        String path = location.getPath();
        this.modelResource = Identifier.fromNamespaceAndPath(namespace, "geo/" + path + ".geo.json");
        this.textureResource = Identifier.fromNamespaceAndPath(namespace, "textures/item/entity/" + path + ".png");
        this.animationResource = Identifier.fromNamespaceAndPath(namespace, "animations/" + path + ".animation.json");
    }

    @Override
    // 26.2: 参数从 animatable 改为 GeoRenderState
    public @NonNull Identifier getModelResource(@NonNull GeoRenderState renderState) {
        return this.modelResource;
    }

    @Override
    public @NonNull Identifier getTextureResource(@NonNull GeoRenderState renderState) {
        return this.textureResource;
    }

    @Override
    public @NonNull Identifier getAnimationResource(@NonNull DummyGeoAnimatable animatable) {
        return this.animationResource;
    }
}
