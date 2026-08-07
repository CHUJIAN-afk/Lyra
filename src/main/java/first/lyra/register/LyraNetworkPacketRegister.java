package first.lyra.register;

import first.lyra.Lyra;
import first.lyra.common.network.BatchedDamageInfoPayload;
import first.lyra.common.network.BatchedParticlesPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

@EventBusSubscriber(modid = Lyra.MODID)
public class LyraNetworkPacketRegister {

    @SubscribeEvent
    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(Lyra.MODID)
                .executesOn(HandlerThread.MAIN)
                .playToClient(BatchedParticlesPayload.TYPE, BatchedParticlesPayload.STREAM_CODEC, BatchedParticlesPayload::handleClient)
                .playToClient(BatchedDamageInfoPayload.TYPE, BatchedDamageInfoPayload.STREAM_CODEC, BatchedDamageInfoPayload::handleClient);
    }
}
