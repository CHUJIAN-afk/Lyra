package first.lyra.register;

import first.lyra.Lyra;
import first.lyra.common.network.BatchedParticlesPayload;
import first.lyra.common.network.CameraShakePayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Lyra.MODID)
public class LyraNetworkPacketRegister {

    @SubscribeEvent
    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Lyra.MODID).executesOn(HandlerThread.MAIN);
        registrar.versioned("2").playToClient(BatchedParticlesPayload.TYPE, BatchedParticlesPayload.STREAM_CODEC, BatchedParticlesPayload::handleClient);
        registrar.playToClient(CameraShakePayload.TYPE, CameraShakePayload.STREAM_CODEC, CameraShakePayload::handleClient);
    }
}
