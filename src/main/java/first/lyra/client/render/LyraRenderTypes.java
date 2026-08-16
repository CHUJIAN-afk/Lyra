package first.lyra.client.render;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;

@SuppressWarnings("deprecation")
public final class LyraRenderTypes {

    public static final RenderType ENTITY_ATLAS_TRANSLUCENT = RenderType.create("lyra_entity_atlas_translucent",
            RenderSetup.builder(RenderPipelines.ITEM_TRANSLUCENT)
                    .withTexture("Sampler0", TextureAtlas.LOCATION_ITEMS)
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup());

    private LyraRenderTypes() {
    }
}
