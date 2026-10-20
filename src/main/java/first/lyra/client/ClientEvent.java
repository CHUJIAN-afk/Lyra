package first.lyra.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import first.lyra.Lyra;
import first.lyra.client.light.LyraLights;
import first.lyra.client.render.AttachmentEntityRenderDispatcher;
import first.lyra.client.render.WholeModelTransparency;
import first.lyra.client.tooltip.TooltipHandler;
import first.lyra.common.particle.genericParticle.GenericParticleProvider;
import first.lyra.register.LyraParticleRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.io.IOException;
import java.util.List;

@EventBusSubscriber(modid = Lyra.MODID, value = Dist.CLIENT)
public class ClientEvent {

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(LyraLights::initialize);
    }

    @SubscribeEvent
    public static void shaders(RegisterShadersEvent event) throws IOException {
        WholeModelTransparency.close();
        event.registerShader(new ShaderInstance(event.getResourceProvider(), Lyra.rl("model_transparency"), DefaultVertexFormat.PARTICLE), WholeModelTransparency::setShader);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderAttachments(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
                AttachmentEntityRenderDispatcher.render(minecraft.level, event.getCamera(), event.getPoseStack(), buffers, event.getPartialTick().getGameTimeDeltaPartialTick(false));
                buffers.endBatch();
            }
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            LyraLights.clear();
            WholeModelTransparency.close();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();
        List<Component> toolTip = event.getToolTip();
        toolTip.addAll(TooltipHandler.getMinionWeaponItemTooltip(itemStack, player));
        toolTip.addAll(TooltipHandler.getCustomTooltip(itemStack, player));
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(LyraParticleRegister.Generic.get(), GenericParticleProvider::new);
    }
}
