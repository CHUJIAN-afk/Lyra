package first.lyra.client.render;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;

public final class LyraRenderTypes {

    /** items atlas + entity translucent 管线（depthWrite=true，无离屏目标）。 */
    public static final RenderType ENTITY_ATLAS_TRANSLUCENT = RenderType.create("lyra_entity_atlas_translucent",
            RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT)
                    .withTexture("Sampler0", TextureAtlas.LOCATION_ITEMS)
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup());

    private LyraRenderTypes() {
    }
}
