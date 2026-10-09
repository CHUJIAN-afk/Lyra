package first.lyra.client;

import first.lyra.client.dynamicLight.DynamicLightDispatcher;
import first.lyra.client.render.AttachmentEntityRenderDispatcher;
import first.lyra.client.tooltip.TooltipHandler;
import first.lyra.common.particle.genericParticle.GenericParticleProvider;
import first.lyra.mixin.LevelRendererAccessor;
import first.lyra.register.LyraParticleRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.mesdag.portlib.client.PortDeltaTicker;
import org.mesdag.portlib.event.PortEventHandler;
import org.mesdag.portlib.event.PortEventPriority;
import org.mesdag.portlib.event.client.PortRegisterParticleProvidersEvent;
import org.mesdag.portlib.event.client.PortRenderLevelStageEvent;
import org.mesdag.portlib.event.entity.player.PortItemTooltipEvent;

import java.util.List;

public class ClientEvent {

    public static void init() {
        PortEventHandler.addListener(PortEventPriority.LOWEST, ClientEvent::onItemTooltip);
        PortEventHandler.addListener(ClientEvent::onRenderLevel);
        PortEventHandler.addListener(ClientEvent::onRegisterParticleProviders);
    }

    public static void onRenderLevel(PortRenderLevelStageEvent event) {
        if (event.getStage() == PortRenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            if (level != null) {
                MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
                PortDeltaTicker partialTick = event.getPartialTick();
                AttachmentEntityRenderDispatcher.render(level, event.getCamera(), event.getPoseStack(), bufferSource, partialTick.getGameTimeDeltaPartialTick(true));
            }
        } else if (event.getStage() == PortRenderLevelStageEvent.Stage.AFTER_LEVEL) {
            DynamicLightDispatcher.INSTANCE.update((LevelRendererAccessor) event.getLevelRenderer());
        }
    }

    public static void onItemTooltip(PortItemTooltipEvent event) {
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();
        List<Component> toolTip = event.getToolTip();
        toolTip.addAll(TooltipHandler.getMinionWeaponItemTooltip(itemStack, player));
        toolTip.addAll(TooltipHandler.getCustomTooltip(itemStack, player));
    }

    public static void onRegisterParticleProviders(PortRegisterParticleProvidersEvent event) {
        event.registerSpriteSet(LyraParticleRegister.Generic.get(), GenericParticleProvider::new);
    }
}
