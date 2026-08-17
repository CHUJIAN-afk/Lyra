package first.lyra.client.render;

import first.lyra.Lyra;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

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

    /**
     * items atlas + block translucent 管线（独立模型渲染）。
     * <p>
     * 用 TRANSLUCENT_BLOCK（BLOCK 顶点格式）而非 ITEM_TRANSLUCENT（ENTITY 格式）：
     * BufferBuilder 的 11 参 addVertex 对 BLOCK 格式是更短快路径（pos+color+uv+light
     * 直写，无 overlay/normal——8 次内存写 vs ENTITY 的 10 次 + putNormals 量化）。
     * block 着色器无 per-face 漫反射（光照在 lightmap 预计算），FULL_BRIGHT 全亮正确，
     * 法线不再需要。
     * </p>
     */
    public static final RenderType ENTITY_ATLAS_TRANSLUCENT = RenderType.create("lyra_entity_atlas_translucent",
            RenderSetup.builder(RenderPipelines.TRANSLUCENT_BLOCK)
                    .withTexture("Sampler0", TextureAtlas.LOCATION_ITEMS)
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup());

    /** 拖尾渲染类型（block translucent 管线 + trail 纹理，BLOCK 格式批量直写性能最优）。 */
    public static final RenderType TRAIL = RenderType.create("lyra_trail",
            RenderSetup.builder(RenderPipelines.TRANSLUCENT_BLOCK)
                    .withTexture("Sampler0", Lyra.id("textures/trail.png"))
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup());

    /** 单纹理 translucent 管线（block 格式，伤害数字等自定义几何批量直写）。 */
    public static RenderType textureTranslucent(Identifier texture) {
        return RenderType.create("lyra_texture_translucent",
                RenderSetup.builder(RenderPipelines.TRANSLUCENT_BLOCK)
                        .withTexture("Sampler0", texture)
                        .useLightmap()
                        .useOverlay()
                        .createRenderSetup());
    }
}
