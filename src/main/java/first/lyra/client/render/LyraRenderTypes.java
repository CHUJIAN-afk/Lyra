package first.lyra.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import first.lyra.Lyra;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * Lyra 渲染类型工厂（1.21.1，26.2 对齐）。
 * <p>
 * 模型管线：items atlas + entity translucent emissive（NEW_ENTITY 格式）。
 * 标记等 alwaysVisible 贴图使用无深度测试的半透明管线（可透视，不被方块遮挡）。
 * </p>
 */
public class LyraRenderTypes extends RenderType {

    public LyraRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    private static final Function<ResourceLocation, RenderType> NO_DEPTH_TEXTURE = Util.memoize(texture -> {
        CompositeState state = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setDepthTestState(NO_DEPTH_TEST)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(true);
        return create("lyra_texture_no_depth", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, state);
    });

    public static RenderType getTrail() {
        return RenderType.entityTranslucentEmissive(Lyra.rl("textures/trail.png"));
    }

    public static RenderType getModel() {
        return Sheets.translucentItemSheet();
    }

    /**
     * 通用贴图渲染类型（1.21.1 对应 26.2 LyraRenderTypes.texture）。
     *
     * @param texture       贴图路径
     * @param alwaysVisible true = 无深度测试变体（可透视，用于召唤标记等）
     */
    public static RenderType texture(ResourceLocation texture, boolean alwaysVisible) {
        return alwaysVisible ? NO_DEPTH_TEXTURE.apply(texture) : RenderType.entityTranslucent(texture);
    }
}
