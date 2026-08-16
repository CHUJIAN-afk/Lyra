package first.lyra.client.renderType;

import first.lyra.Lyra;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * 拖尾渲染类型。
 * <p>
 * 26.2: RenderType 不可继承、CompositeState 体系删除,改为 RenderSetup.builder(RenderPipelines) 工厂。
 * </p>
 */
public class TrailRenderType {

    private static final RenderType TRAIL = RenderType.create("lyra_trail",
            RenderSetup.builder(RenderPipelines.ITEM_TRANSLUCENT)
                    .withTexture("Sampler0", Lyra.id("textures/trail.png"))
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup());

    public static RenderType getTrail() {
        return TRAIL;
    }
}
