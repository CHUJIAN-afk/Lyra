package first.lyra.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import first.lyra.Lyra;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber(modid = Lyra.MODID)
public class LyraRenderPipelines {

    @SubscribeEvent
    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(TRANSLUCENT);
        event.registerPipeline(TRANSLUCENT_NO_DEPTH);
    }

    public static final RenderPipeline TRANSLUCENT = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Lyra.id("pipeline/translucent"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
            .build();

    public static final RenderPipeline TRANSLUCENT_NO_DEPTH = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Lyra.id("pipeline/translucent_no_depth"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1f)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build();
}
