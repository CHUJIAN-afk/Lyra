package first.lyra.client.render;

import first.lyra.client.render.trail.TrailContext;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.PathNode;
import software.bernie.geckolib.util.Color;

/**
 * 渲染上下文：包裹实体、视觉节点与当帧参数，以及拖尾/模型子上下文，链式构建。
 * <p>
 * 渲染器在 {@code createContext} 里配置本帧所需内容，后续渲染方法统一从 {@code context} 取值，
 * 颜色（含透明度）只在这里写入一次。
 * </p>
 */
public class RenderContext<T extends AttachmentEntity> {

    public final T entity;
    public final PathNode visualNode;
    public final float partialTick;

    public int packedLight;
    public Color color = Color.WHITE;
    public TrailContext<T> trail;
    public ModelContext model = new ModelContext();

    public RenderContext(T entity, PathNode visualNode, float partialTick, int packedLight) {
        this.entity = entity;
        this.visualNode = visualNode;
        this.partialTick = partialTick;
        this.packedLight = packedLight;
    }

    public RenderContext<T> packedLight(int packedLight) {
        this.packedLight = packedLight;
        return this;
    }

    public RenderContext<T> color(Color color) {
        this.color = color;
        return this;
    }

    public RenderContext<T> trail(TrailContext<T> trail) {
        this.trail = trail;
        return this;
    }

    public RenderContext<T> model(ModelContext model) {
        this.model = model;
        return this;
    }

    /** 是否有拖尾 */
    public boolean hasTrail() {
        return trail != null && trail.timer > 0;
    }

    /** 颜色计算函数 */
    @FunctionalInterface
    public interface ColorFunction<T extends AttachmentEntity> {
        int getColor(T entity, float progress, float partialTick);
    }

    /** 淡出函数 */
    @FunctionalInterface
    public interface FadeFunction {
        float getFade(float progress);
    }

    /** 透明度增强函数 */
    @FunctionalInterface
    public interface AlphaBoostFunction<T extends AttachmentEntity> {
        float getBoost(T entity, float progress);
    }

    /** 亮度增强函数 */
    @FunctionalInterface
    public interface BrightnessBoostFunction<T extends AttachmentEntity> {
        float getBoost(T entity, float progress);
    }
}
