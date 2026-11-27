package first.lyra.common.network;

import first.lyra.Lyra;
import first.lyra.common.particle.genericParticle.GenericParticleOptions;
import first.lyra.register.LyraParticleRegister;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

// 通用粒子可继承包内上一条通用粒子的参数与坐标。
public record BatchedParticlesPayload(List<Entry> entries) implements CustomPacketPayload {

    public static final Type<BatchedParticlesPayload> TYPE = new Type<>(Lyra.rl("batched_particles"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BatchedParticlesPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.entries.size());
                Entry previous = null;
                for (Entry entry : payload.entries) {
                    entry.encode(buf, previous);
                    if (entry.options instanceof GenericParticleOptions) {
                        previous = entry;
                    }
                }
            },
            buf -> {
                int count = buf.readVarInt();
                List<Entry> entries = new ArrayList<>(Math.min(count, 65536));
                Entry previous = null;
                for (int i = 0; i < count; i++) {
                    Entry entry = Entry.decode(buf, previous);
                    entries.add(entry);
                    if (entry.options instanceof GenericParticleOptions) {
                        previous = entry;
                    }
                }
                return new BatchedParticlesPayload(entries);
            }
    );

    public static void handleClient(BatchedParticlesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            for (Entry entry : payload.entries()) {
                level.addParticle(entry.options(), false, entry.x(), entry.y(), entry.z(), entry.vx(), entry.vy(), entry.vz());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.ofMember(
                (entry, buf) -> entry.encode(buf, null),
                buf -> decode(buf, null)
        );

        private void encode(RegistryFriendlyByteBuf buf, Entry previous) {
            ParticleType<?> type = options.getType();
            buf.writeVarInt(BuiltInRegistries.PARTICLE_TYPE.getId(type));
            int fields = 127;
            if (options instanceof GenericParticleOptions generic) {
                fields = (previous == null || !sameOptions(generic, (GenericParticleOptions) previous.options) ? 1 : 0)
                        | changed(x, previous == null ? 0.0 : previous.x, 2)
                        | changed(y, previous == null ? 0.0 : previous.y, 4)
                        | changed(z, previous == null ? 0.0 : previous.z, 8)
                        | changed(vx, previous == null ? 0.0 : previous.vx, 16)
                        | changed(vy, previous == null ? 0.0 : previous.vy, 32)
                        | changed(vz, previous == null ? 0.0 : previous.vz, 64);
                buf.writeByte(fields);
            }
            if ((fields & 1) != 0) {
                @SuppressWarnings("unchecked")
                StreamCodec<? super RegistryFriendlyByteBuf, ParticleOptions> codec = (StreamCodec<? super RegistryFriendlyByteBuf, ParticleOptions>) type.streamCodec();
                codec.encode(buf, options);
            }
            if ((fields & 2) != 0) {
                buf.writeDouble(x);
            }
            if ((fields & 4) != 0) {
                buf.writeDouble(y);
            }
            if ((fields & 8) != 0) {
                buf.writeDouble(z);
            }
            if ((fields & 16) != 0) {
                buf.writeDouble(vx);
            }
            if ((fields & 32) != 0) {
                buf.writeDouble(vy);
            }
            if ((fields & 64) != 0) {
                buf.writeDouble(vz);
            }
        }

        private static Entry decode(RegistryFriendlyByteBuf buf, Entry previous) {
            ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.byId(buf.readVarInt());
            assert type != null;
            int fields = type == LyraParticleRegister.Generic.get() ? buf.readUnsignedByte() : 127;
            ParticleOptions options = (fields & 1) != 0 ? type.streamCodec().decode(buf) : previous.options;
            double x = readCoordinate(buf, fields, 2, previous == null ? 0.0 : previous.x);
            double y = readCoordinate(buf, fields, 4, previous == null ? 0.0 : previous.y);
            double z = readCoordinate(buf, fields, 8, previous == null ? 0.0 : previous.z);
            double vx = readCoordinate(buf, fields, 16, previous == null ? 0.0 : previous.vx);
            double vy = readCoordinate(buf, fields, 32, previous == null ? 0.0 : previous.vy);
            double vz = readCoordinate(buf, fields, 64, previous == null ? 0.0 : previous.vz);
            return new Entry(options, x, y, z, vx, vy, vz);
        }

        private static int changed(double value, double previous, int mask) {
            return Double.doubleToRawLongBits(value) == Double.doubleToRawLongBits(previous) ? 0 : mask;
        }

        private static boolean sameOptions(GenericParticleOptions options, GenericParticleOptions previous) {
            return options == previous || (options.centerColor() == previous.centerColor()
                    && options.edgeColor() == previous.edgeColor()
                    && options.lifetime() == previous.lifetime()
                    && Float.floatToRawIntBits(options.spinSpeed()) == Float.floatToRawIntBits(previous.spinSpeed())
                    && Float.floatToRawIntBits(options.friction()) == Float.floatToRawIntBits(previous.friction())
                    && Float.floatToRawIntBits(options.scale()) == Float.floatToRawIntBits(previous.scale()));
        }

        private static double readCoordinate(RegistryFriendlyByteBuf buf, int fields, int mask, double previous) {
            return (fields & mask) != 0 ? buf.readDouble() : previous;
        }
    }
}
