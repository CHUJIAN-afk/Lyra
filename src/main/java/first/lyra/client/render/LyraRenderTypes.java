package first.lyra.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import first.lyra.Lyra;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;

/**
 * Lyra 渲染类型工厂（1.21.1，26.2 对齐）。
 * <p>
 * 模型管线：items atlas + entity translucent emissive（NEW_ENTITY 格式，26.2 行为对齐：
 * 半透明正确混合、不写深度 COLOR_WRITE、无 sortOnUpload、FULL_BRIGHT 全亮）。
 * </p>
 */
public class LyraRenderTypes extends RenderType {

    private LyraRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    private static final RenderType TRAIL = create("lyra_trail", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_TRANSLUCENT_SHADER)
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
