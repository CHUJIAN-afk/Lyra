package first.lyra.client;

import first.lyra.Lyra;
import first.lyra.client.render.model.bbmodel.BBModelManager;
import first.lyra.client.render.model.geo.GeoAnimationManager;
import first.lyra.client.render.model.geo.GeoModelManager;
import first.lyra.client.tooltip.TooltipHandler;
import first.lyra.common.damageInfo.DamageInfoStyleManager;
import first.lyra.common.particle.genericParticle.GenericParticleProvider;
import first.lyra.register.LyraParticleRegister;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = Lyra.MODID, value = Dist.CLIENT)
public class ClientEvent {

    @SubscribeEvent
    public static void handler(ItemTooltipEvent event) {
        TooltipHandler.handler(event);
    }

    @SubscribeEvent
    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(DamageInfoStyleManager.INSTANCE);
        event.registerReloadListener(GeoModelManager.INSTANCE);
        event.registerReloadListener(GeoAnimationManager.INSTANCE);
        event.registerReloadListener(BBModelManager.INSTANCE);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(LyraParticleRegister.Generic.get(), GenericParticleProvider::new);
    }
}
