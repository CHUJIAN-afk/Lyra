package first.lyra.common.particle.genericParticle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import first.lyra.register.LyraParticleRegister;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/**
 * 通用粒子选项，支持配置中心颜色、边缘颜色（RGB）、寿命、旋转速度、阻力、大小。
 */
public record GenericParticleOptions(int centerColor, int edgeColor, int lifetime, float spinSpeed, float friction, float scale) implements ParticleOptions {

    public static final MapCodec<GenericParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    com.mojang.serialization.Codec.INT.fieldOf("centerColor").forGetter(GenericParticleOptions::centerColor),
                    com.mojang.serialization.Codec.INT.fieldOf("edgeColor").forGetter(GenericParticleOptions::edgeColor),
                    com.mojang.serialization.Codec.INT.fieldOf("lifetime").forGetter(GenericParticleOptions::lifetime),
                    com.mojang.serialization.Codec.FLOAT.fieldOf("spinSpeed").forGetter(GenericParticleOptions::spinSpeed),
                    com.mojang.serialization.Codec.FLOAT.fieldOf("friction").forGetter(GenericParticleOptions::friction),
                    com.mojang.serialization.Codec.FLOAT.fieldOf("scale").forGetter(GenericParticleOptions::scale)
            ).apply(instance, GenericParticleOptions::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, GenericParticleOptions> STREAM_CODEC = StreamCodec.of(
            (buffer, options) -> {
                int flags = (options.centerColor == options.edgeColor ? 1 : 0)
                        | (options.centerColor >>> 24 != 0 ? 2 : 0)
                        | (options.edgeColor >>> 24 != 0 ? 4 : 0)
                        | (Float.floatToRawIntBits(options.spinSpeed) != 0 ? 8 : 0)
                        | (options.friction != 1.0F ? 16 : 0)
                        | (options.scale != 1.0F ? 32 : 0)
                        | (options.lifetime >>> 28 != 0 ? 64 : 0);
                buffer.writeByte(flags);
                if ((flags & 2) != 0) {
                    buffer.writeInt(options.centerColor);
                } else {
                    buffer.writeMedium(options.centerColor);
                }
                if ((flags & 1) == 0) {
                    if ((flags & 4) != 0) {
                        buffer.writeInt(options.edgeColor);
                    } else {
                        buffer.writeMedium(options.edgeColor);
                    }
                }
                if ((flags & 64) != 0) {
                    buffer.writeInt(options.lifetime);
                } else {
                    buffer.writeVarInt(options.lifetime);
                }
                if ((flags & 8) != 0) {
                    buffer.writeFloat(options.spinSpeed);
                }
                if ((flags & 16) != 0) {
                    buffer.writeFloat(options.friction);
                }
                if ((flags & 32) != 0) {
                    buffer.writeFloat(options.scale);
                }
            },
            buffer -> {
                int flags = buffer.readUnsignedByte();
                int center = (flags & 2) != 0 ? buffer.readInt() : buffer.readUnsignedMedium();
                int edge = (flags & 1) != 0 ? center : (flags & 4) != 0 ? buffer.readInt() : buffer.readUnsignedMedium();
                int lifetime = (flags & 64) != 0 ? buffer.readInt() : buffer.readVarInt();
                float spin = (flags & 8) != 0 ? buffer.readFloat() : 0.0F;
                float friction = (flags & 16) != 0 ? buffer.readFloat() : 1.0F;
                float scale = (flags & 32) != 0 ? buffer.readFloat() : 1.0F;
                return new GenericParticleOptions(center, edge, lifetime, spin, friction, scale);
            }
    );

    @Override
    public @NotNull ParticleType<GenericParticleOptions> getType() {
        return LyraParticleRegister.Generic.get();
    }
}
