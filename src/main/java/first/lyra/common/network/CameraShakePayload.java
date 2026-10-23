package first.lyra.common.network;

import first.lyra.Lyra;
import first.lyra.client.render.CameraShake;
import first.lyra.utils.LyraStreamCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record CameraShakePayload(Vec3 pos, float range, float strength) implements CustomPacketPayload {

    public static final Type<CameraShakePayload> TYPE = new Type<>(Lyra.rl("camera_shake"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CameraShakePayload> STREAM_CODEC = StreamCodec.composite(
            LyraStreamCodecs.VEC_3, CameraShakePayload::pos,
            ByteBufCodecs.FLOAT, CameraShakePayload::range,
            ByteBufCodecs.FLOAT, CameraShakePayload::strength,
            CameraShakePayload::new
    );

    public static void handleClient(CameraShakePayload payload, IPayloadContext context) {
        float distance = (float) context.player().position().distanceTo(payload.pos);
        CameraShake.add(payload.strength * Math.max(0f, 1f - distance / payload.range));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
