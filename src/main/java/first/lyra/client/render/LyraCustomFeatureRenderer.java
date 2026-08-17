package first.lyra.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * {@link LyraCustomSubmit} 的渲染器：遍历同批次的提交，逐调用顶点写入回调。
 * <p>
 * 实体间遮挡由渲染类型的深度测试负责（深度写开启），提交顺序即写入顺序。
 * 单例由 FeatureRenderDispatcher 经 RegisterFeatureRenderersEvent 创建。
 * </p>
 */
public class LyraCustomFeatureRenderer extends RenderTypeFeatureRenderer<LyraCustomSubmit> {
    public static final FeatureRendererType<LyraCustomSubmit> TYPE = FeatureRendererType.create("Lyra Custom");

    @Override
    protected void buildGroup(@NonNull FeatureFrameContext context, List<LyraCustomSubmit> submits) {
        for (LyraCustomSubmit submit : submits) {
            VertexConsumer builder = this.getVertexBuilder(submit.renderType());
            submit.renderer().render(submit.pose(), builder);
        }
    }
}
