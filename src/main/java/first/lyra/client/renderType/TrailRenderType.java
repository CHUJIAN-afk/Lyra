package first.lyra.client.renderType;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import first.lyra.Lyra;
import net.minecraft.client.renderer.RenderType;

public class TrailRenderType extends RenderType {

    private TrailRenderType(String name, VertexFormat fmt, VertexFormat.Mode mode, int bufSize, boolean affectsCrumbling, boolean sort, Runnable setup, Runnable clear) {
        super(name, fmt, mode, bufSize, affectsCrumbling, sort, setup, clear);
    }

    private static final RenderType TRAIL = create("lyra_trail", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
            .setTextureState(new TextureStateShard(Lyra.rl("textures/trail.png"), false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .setOverlayState(OVERLAY)
            .createCompositeState(false));

    public static RenderType getTrail() {
        return TRAIL;
    }
}
