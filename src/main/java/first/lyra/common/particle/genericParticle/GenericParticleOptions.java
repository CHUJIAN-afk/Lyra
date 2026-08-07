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
                buffer.writeInt(options.centerColor);
                buffer.writeInt(options.edgeColor);
                buffer.writeInt(options.lifetime);
                buffer.writeFloat(options.spinSpeed);
                buffer.writeFloat(options.friction);
                buffer.writeFloat(options.scale);
            },
            buffer -> new GenericParticleOptions(
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat()
            )
    );

    @Override
    public @NotNull ParticleType<GenericParticleOptions> getType() {
        return LyraParticleRegister.Generic.get();
    }
}
