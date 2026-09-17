package first.lyra.common.network;

import first.lyra.Lyra;
import first.lyra.common.particle.genericParticle.GenericParticleOptions;
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

/**
 * 批量粒子网络包（服务端 → 客户端）。
 * <p>
 * 单 tick 内服务端累积的所有粒子记录一次性下发，避免每个粒子单独发送
 * {@code ClientboundLevelParticlesPacket}。客户端收到后逐条调用
 * {@link net.minecraft.world.level.Level#addParticle} 生成粒子，视觉效果与原版一致。
 * </p>
 * <p>
 * 粒子类型序列化复刻原版 {@code ClientboundLevelParticlesPacket}：先写
 * {@link net.minecraft.core.registries.BuiltInRegistries#PARTICLE_TYPE} 的注册表 id，
 * 再用该类型自有的 {@link ParticleType#streamCodec()} 编码具体参数，
 * 兼容原版与自定义粒子（如 {@link GenericParticleOptions}）。
 * </p>
 *
 * @param entries 粒子记录列表
 */
public record BatchedParticlesPayload(List<Entry> entries) implements IPortPacket.S2C {

    public static final ResourceLocation ID = Lyra.rl("batched_particles");

    public static final PortStreamCodec<PortRegistryFriendlyByteBuf, BatchedParticlesPayload> STREAM_CODEC = PortStreamCodec.composite(
            Entry.STREAM_CODEC.apply(PortByteBufCodecs.list()),
            BatchedParticlesPayload::entries,
            BatchedParticlesPayload::new
    );

    /**
     * 客户端处理：逐条调用 {@link Level#addParticle} 生成粒子，复刻原版视觉效果。
     */
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

    /**
     * 单条粒子记录：类型 + 位置 + 速度。
     * <p>
     * 序列化复刻原版：写 {@link net.minecraft.core.registries.BuiltInRegistries#PARTICLE_TYPE} 注册表 id，
     * 再用 {@link ParticleType#streamCodec()} 编码具体参数。位置/速度用三个 double 存储。
     * </p>
     */
    public record Entry(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz) {

        @SuppressWarnings("unchecked")
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
                    ParticleOptions options1 = ((ParticleType<ParticleOptions>) type).getDeserializer().fromNetwork((ParticleType<ParticleOptions>) type, buf);
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
