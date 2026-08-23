package first.lyra.client;

import first.lyra.Lyra;
import first.lyra.client.render.LyraCustomFeatureRenderer;
import first.lyra.client.tooltip.TooltipHandler;
import first.lyra.common.particle.genericParticle.GenericParticleProvider;
import first.lyra.common.damageInfo.DamageInfoStyleManager;
import first.lyra.register.LyraParticleRegister;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterFeatureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = Lyra.MODID, value = Dist.CLIENT)
public class ClientEvent {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void handler(ItemTooltipEvent event) {
        TooltipHandler.handler(event);
    }

    @SubscribeEvent
    public static void registerClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(Lyra.id("damage_info_style"), DamageInfoStyleManager.INSTANCE);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(LyraParticleRegister.Generic.get(), GenericParticleProvider::new);
    }

    @SubscribeEvent
    public static void registerFeatureRenderers(RegisterFeatureRenderersEvent event) {
        event.register(LyraCustomFeatureRenderer.TYPE, new LyraCustomFeatureRenderer());
    }
}