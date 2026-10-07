package first.lyra.common.network;

import first.lyra.Lyra;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.mesdag.portlib.network.IPortPacket;
import org.mesdag.portlib.network.PortRegistryFriendlyByteBuf;
import org.mesdag.portlib.network.codec.PortByteBufCodecs;
import org.mesdag.portlib.network.codec.PortStreamCodec;

import java.util.List;

public record BatchedParticlesPayload(List<Entry> entries) implements IPortPacket.S2C {

    public static final ResourceLocation ID = Lyra.rl("batched_particles");

    public static final PortStreamCodec<PortRegistryFriendlyByteBuf, BatchedParticlesPayload> STREAM_CODEC = PortStreamCodec.composite(
            Entry.STREAM_CODEC.apply(PortByteBufCodecs.list()),
            BatchedParticlesPayload::entries,
            BatchedParticlesPayload::new
    );

    @Override
    public void work(Player player) {
        Level level = player.level();
        for (Entry entry : entries) {
            level.addParticle(entry.options(), false, entry.x(), entry.y(), entry.z(), entry.vx(), entry.vy(), entry.vz());
        }
    }

    @Override
    public ResourceLocation identifier() {
        return ID;
    }

    public record Entry(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz) {

        public static final PortStreamCodec<PortRegistryFriendlyByteBuf, Entry> STREAM_CODEC = PortStreamCodec.ofMember(
                (entry, buf) -> {
                    ParticleType<?> type = entry.options.getType();
                    buf.writeResourceLocation(BuiltInRegistries.PARTICLE_TYPE.getKey(type));
                    entry.options.writeToNetwork(buf);
                    buf.writeDouble(entry.x);
                    buf.writeDouble(entry.y);
                    buf.writeDouble(entry.z);
                    buf.writeDouble(entry.vx);
                    buf.writeDouble(entry.vy);
                    buf.writeDouble(entry.vz);
                },
                buf -> {
                    ResourceLocation id = buf.readResourceLocation();
                    ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(id);
                    assert type != null;
                    @SuppressWarnings("unchecked") ParticleOptions options1 = ((ParticleType<ParticleOptions>) type).getDeserializer().fromNetwork((ParticleType<ParticleOptions>) type, buf);
                    double x1 = buf.readDouble();
                    double y1 = buf.readDouble();
                    double z1 = buf.readDouble();
                    double vx1 = buf.readDouble();
                    double vy1 = buf.readDouble();
                    double vz1 = buf.readDouble();
                    return new Entry(options1, x1, y1, z1, vx1, vy1, vz1);
                }
        );
    }
}
