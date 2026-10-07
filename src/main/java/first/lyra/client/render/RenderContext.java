package first.lyra.client.render;

import first.lyra.client.render.trail.TrailContext;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.PathNode;
import software.bernie.geckolib.core.object.Color;

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

    public boolean hasTrail() {
        return trail != null && trail.timer > 0;
    }

    @FunctionalInterface
    public interface ColorFunction<T extends AttachmentEntity> {
        int getColor(T entity, float progress, float partialTick);
    }

    @FunctionalInterface
    public interface FadeFunction {
        float getFade(float progress);
    }

    @FunctionalInterface
    public interface AlphaBoostFunction<T extends AttachmentEntity> {
        float getBoost(T entity, float progress);
    }

    @FunctionalInterface
    public interface BrightnessBoostFunction<T extends AttachmentEntity> {
        float getBoost(T entity, float progress);
    }
}
