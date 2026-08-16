package first.lyra.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.BatchableSubmit;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NonNull;

/**
 * 自定义几何提交节点（TranslucentSubmit + BatchableSubmit，挂 afterTerrain phase）。
 * <p>
 * 经 {@code collector.submitSpecial(RenderPhaseKeys.AFTER_TERRAIN, ...)} 提交，
 * 在半透明方块/云之后渲染（层级正确）。实体间遮挡由渲染类型的深度测试负责。
 * {@code distanceToCameraSq} 为 TranslucentSubmit 接口要求（保留挂排序 phase 的能力）。
 * </p>
 * <p>
 * {@code batchKey = renderType}：SimpleFeatureRenderPhase 按 key 合并同 renderType 的提交为
 * 一个 group → 一个 draw（GPU draw call）。跨实体同 renderType 的拖尾/模型合并为单个 draw，
 * 与原版 BatchableSubmit（Model/Item/Custom 的 batchKey 均为 renderType）行为一致，
 * draw call 数从「实体数量」降为「renderType 数量」。
 * </p>
 *
 * @param pose       提交时 PoseStack 快照（延迟执行，不可用外部可变引用）
 * @param renderType 渲染类型
 * @param renderer   顶点写入回调
 */
public record LyraCustomSubmit(PoseStack.Pose pose, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) implements TranslucentSubmit, BatchableSubmit {

    @Override
    public @NonNull Object batchKey() {
        return this.renderType;
    }

    @Override
    public float distanceToCameraSq() {
        return TranslucentSubmit.computeDistanceToCameraSq(this.pose.pose());
    }

    @Override
    public @NonNull FeatureRendererType<LyraCustomSubmit> featureType() {
        return LyraCustomFeatureRenderer.TYPE;
    }
}
