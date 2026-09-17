package first.lyra.register;

import first.lyra.Lyra;
import first.lyra.common.network.BatchedDamageInfoPayload;
import first.lyra.common.network.BatchedParticlesPayload;

public class LyraNetworkPacketRegister {

    public static void register() {
        Lyra.NETWORK_HANDLER.registerInGameS2C(
                BatchedParticlesPayload.class,
                BatchedParticlesPayload.ID,
                BatchedParticlesPayload.STREAM_CODEC
        );
        Lyra.NETWORK_HANDLER.registerInGameS2C(
                BatchedDamageInfoPayload.class,
                BatchedDamageInfoPayload.ID,
                BatchedDamageInfoPayload.STREAM_CODEC
        );
    }
}
