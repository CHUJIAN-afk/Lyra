package first.lyra.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.neoforge.client.submit.RenderPhaseKey;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;

/**
 * Lyra 自定义几何提交入口。
 * <p>
 * 26.2 渲染顺序：feature 半透明 phase（translucentModels / translucentBlocksAndItems）
 * 在半透明方块与云（translucentTerrain）<b>之前</b>渲染，自定义半透明几何（拖尾、染色模型）
 * 若走这些 phase 会被后画的半透明方块/云错误覆盖（透过几何透视方块）。统一挂
 * afterTerrain phase（半透明方块之后渲染），层级正确；实体间遮挡顺序由
 * {@link LyraCustomFeatureRenderer#buildGroup} 内按距离排序保证。
 * </p>
 */
public final class LyraRenderPhases {

    /** afterTerrain phase（半透明方块/云之后渲染）。 */
    public static final RenderPhaseKey<SubmitNode> AFTER_TERRAIN = RenderPhaseKeys.AFTER_TERRAIN;

    private LyraRenderPhases() {
    }

    /** 提交自定义几何到 afterTerrain phase。拖尾/染色模型共用。 */
    public static void submitTranslucentCustom(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType,
                                               SubmitNodeCollector.CustomGeometryRenderer renderer) {
        collector.submitSpecial(AFTER_TERRAIN, new LyraCustomSubmit(poseStack.last().copy(), renderType, renderer));
    }
}
