package first.lyra.client.render;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;

/**
 * 独立模型渲染管线（items atlas + entity 管线）。
 * <p>
 * 26.2: 物品 sheet 管线（RenderTypes.itemTranslucent）带 ITEM_ENTITY_TARGET 离屏目标
 * （为附魔 glint 的透明合成服务）。独立模型（无 foil）不需要离屏目标，
 * 改用 entity 管线 + items atlas 纹理直接画主目标，消除离屏渲染开销
 * （Java 模型路径性能的关键差异）。atlas 为普通 2D 纹理，quad 的 packedUV
 * 即 atlas 内 0-1 坐标，entity 着色器直接采样。
 * </p>
 * <p>
 * 用 ENTITY_TRANSLUCENT（默认 depthWrite=true，带深度测试）：实体间遮挡由深度测试
 * 负责（近实体挡远实体），不依赖手动排序——手动距离排序在实体接近时闪烁、
 * 且只代表渲染位置不代表模型大小，不可靠。
 * </p>
 */
public final class LyraItemRenderTypes {

    /** items atlas + entity translucent 管线（depthWrite=true，无离屏目标）。 */
    public static final RenderType ENTITY_ATLAS_TRANSLUCENT = RenderType.create("lyra_entity_atlas_translucent",
            RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT)
                    .withTexture("Sampler0", TextureAtlas.LOCATION_ITEMS)
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup());

    private LyraItemRenderTypes() {
    }
}
