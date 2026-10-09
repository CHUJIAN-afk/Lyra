package first.lyra.register;

import first.lyra.Lyra;
import first.lyra.common.network.BatchedParticlesPayload;

public class LyraNetworkPacketRegister {

    public static void register() {
        Lyra.NETWORK_HANDLER.registerInGameS2C(
                BatchedParticlesPayload.class,
                BatchedParticlesPayload.ID,
                BatchedParticlesPayload.STREAM_CODEC
        );
    }
}
