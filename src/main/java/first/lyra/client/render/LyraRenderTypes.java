package first.lyra.client.render;

import first.lyra.Lyra;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

public class LyraRenderTypes {

    @SuppressWarnings("deprecation")
    public static final RenderType ATTACHMENT_ENTITY_TRANSLUCENT = RenderTypes.itemTranslucent(TextureAtlas.LOCATION_ITEMS);

    public static final RenderType TRAIL = RenderType.create("lyra_trail", RenderSetup.builder(LyraRenderPipelines.TRANSLUCENT)
            .withTexture("Sampler0", Lyra.id("textures/trail.png"))
            .useLightmap()
            .useOverlay()
            .createRenderSetup());

    public static RenderType texture(Identifier texture, boolean alwaysVisible) {
        return RenderType.create(alwaysVisible ? "lyra_texture_translucent_no_depth" : "lyra_texture_translucent",
                                 RenderSetup.builder(alwaysVisible ? LyraRenderPipelines.TRANSLUCENT_NO_DEPTH : LyraRenderPipelines.TRANSLUCENT)
                                         .withTexture("Sampler0", texture)
                                         .useLightmap()
                                         .useOverlay()
                                         .sortOnUpload()
                                         .createRenderSetup());
    }
}
