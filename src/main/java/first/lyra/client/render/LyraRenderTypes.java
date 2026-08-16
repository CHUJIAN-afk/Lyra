package first.lyra.client.render;

import first.lyra.Lyra;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;

/**
 * Lyra 渲染类型工厂（模型 + 拖尾统一入口）。
 * <p>
 * 26.2: RenderType 不可继承、CompositeState 体系删除,改为 RenderSetup.builder(RenderPipelines) 工厂。
 * </p>
 */
@SuppressWarnings("deprecation")
public final class LyraRenderTypes {

    private LyraRenderTypes() {
    }

    /** items atlas + item translucent 管线（独立模型渲染，深度/半透明已校验）。 */
    public static final RenderType ENTITY_ATLAS_TRANSLUCENT = RenderType.create("lyra_entity_atlas_translucent",
            RenderSetup.builder(RenderPipelines.ITEM_TRANSLUCENT)
                    .withTexture("Sampler0", TextureAtlas.LOCATION_ITEMS)
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup());

    /** 拖尾渲染类型（item translucent 管线 + trail 纹理）。 */
    public static final RenderType TRAIL = RenderType.create("lyra_trail",
            RenderSetup.builder(RenderPipelines.ITEM_TRANSLUCENT)
                    .withTexture("Sampler0", Lyra.id("textures/trail.png"))
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup());
}
