package first.lyra.client;

import first.lyra.client.render.model.bbmodel.BBModelManager;
import first.lyra.client.render.model.geo.GeoAnimationManager;
import first.lyra.client.render.model.geo.GeoModelManager;
import first.lyra.common.damageInfo.DamageInfoStyleManager;
import first.lyra.common.particle.genericParticle.GenericParticleProvider;
import first.lyra.register.LyraParticleRegister;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import org.mesdag.portlib.event.PortEventHandler;

public final class ClientModEvent {
    public static void init() {
        PortEventHandler.addListener(ClientModEvent::registerClientReloadListeners);
        PortEventHandler.addListener(ClientModEvent::registerParticleProviders);
    }

    private static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(DamageInfoStyleManager.INSTANCE);
        event.registerReloadListener(GeoModelManager.INSTANCE);
        event.registerReloadListener(GeoAnimationManager.INSTANCE);
        event.registerReloadListener(BBModelManager.INSTANCE);
    }

    private static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(LyraParticleRegister.Generic.get(), GenericParticleProvider::new);
    }
}
