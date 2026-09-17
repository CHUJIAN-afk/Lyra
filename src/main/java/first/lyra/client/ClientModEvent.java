package first.lyra.client;

import first.lyra.Lyra;
import first.lyra.client.render.model.bbmodel.BBModelManager;
import first.lyra.client.render.model.geo.GeoAnimationManager;
import first.lyra.client.render.model.geo.GeoModelManager;
import first.lyra.common.damageInfo.DamageInfoStyleManager;
import first.lyra.common.particle.genericParticle.GenericParticleProvider;
import first.lyra.register.LyraParticleRegister;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Lyra.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientModEvent {

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
