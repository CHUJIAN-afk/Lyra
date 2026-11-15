package first.lyra.client;

import first.lyra.client.render.IrisCompat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import first.lyra.Lyra;
import first.lyra.client.light.LyraLights;
import first.lyra.client.render.AfterimageManager;
import first.lyra.client.render.AttachmentEntityRenderDispatcher;
import first.lyra.client.render.CameraShake;
import first.lyra.client.render.WholeModelTransparency;
import first.lyra.client.render.EffectPipeline;
import first.lyra.client.tooltip.LyraTooltipHandler;
import first.lyra.common.particle.genericParticle.GenericParticleProvider;
import first.lyra.register.LyraParticleRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.io.IOException;

@EventBusSubscriber(modid = Lyra.MODID, value = Dist.CLIENT)
public class ClientEvent {

    @SubscribeEvent
    public static void shaders(RegisterShadersEvent event) throws IOException {
        AfterimageManager.close();
        WholeModelTransparency.close();
        LyraLights.clear();
        EffectPipeline.close();
        event.registerShader(new ShaderInstance(event.getResourceProvider(), Lyra.rl("model_transparency"), DefaultVertexFormat.PARTICLE), WholeModelTransparency::setShader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), Lyra.rl("dynamic_light"), DefaultVertexFormat.POSITION), LyraLights::setShader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), Lyra.rl("bloom_extract"), DefaultVertexFormat.POSITION), EffectPipeline::setExtractShader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), Lyra.rl("bloom_downsample"), DefaultVertexFormat.POSITION), EffectPipeline::setDownsampleShader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), Lyra.rl("bloom_upsample"), DefaultVertexFormat.POSITION), EffectPipeline::setUpsampleShader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), Lyra.rl("bloom_composite"), DefaultVertexFormat.POSITION), EffectPipeline::setCompositeShader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), Lyra.rl("effect_texture"), DefaultVertexFormat.POSITION_TEX_COLOR), EffectPipeline::setTextureShader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(), Lyra.rl("effect_composite"), DefaultVertexFormat.POSITION), EffectPipeline::setResolveShader);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beginFrame(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY && !IrisCompat.isShadowPass()) {
            LyraLights.beginFrame();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void finishFrame(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL && !IrisCompat.isShadowPass()) {
            LyraLights.render(event.getCamera(), event.getModelViewMatrix(), event.getProjectionMatrix());
            WholeModelTransparency.prepareLighting();
            WholeModelTransparency.finishFrame();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderAttachments(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES && !IrisCompat.isShadowPass()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
                float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
                AfterimageManager.render(minecraft.level, event.getCamera(), event.getPoseStack(), partialTick);
                AttachmentEntityRenderDispatcher.render(minecraft.level, event.getCamera(), event.getPoseStack(), buffers, partialTick);
                buffers.endBatch();
            }
        }
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        CameraShake.tick();
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        CameraShake.apply(event);
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            CameraShake.clear();
            LyraLights.clear();
            AfterimageManager.close();
            WholeModelTransparency.close();
            EffectPipeline.close();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        LyraTooltipHandler.handler(event);
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(LyraParticleRegister.Generic.get(), GenericParticleProvider::new);
    }
}
