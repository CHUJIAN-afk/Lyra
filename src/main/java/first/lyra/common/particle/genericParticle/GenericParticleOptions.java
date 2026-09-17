package first.lyra.common.particle.genericParticle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import first.lyra.register.LyraParticleRegister;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.mesdag.portlib.network.codec.PortStreamCodec;

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

    public static final PortStreamCodec<ByteBuf, GenericParticleOptions> STREAM_CODEC = PortStreamCodec.ofMember(
            (options, buffer) -> {
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

    public static final ParticleOptions.Deserializer<GenericParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public GenericParticleOptions fromCommand(ParticleType<GenericParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            String[] values = reader.readString().split(";");
            if (values.length != 6) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create("Expected 6 values");
            }
            return new GenericParticleOptions(
                    Integer.parseInt(values[0]),
                    Integer.parseInt(values[1]),
                    Integer.parseInt(values[2]),
                    Float.parseFloat(values[3]),
                    Float.parseFloat(values[4]),
                    Float.parseFloat(values[5])
            );
        }

        @Override
        public GenericParticleOptions fromNetwork(ParticleType<GenericParticleOptions> type, FriendlyByteBuf buffer) {
            return STREAM_CODEC.decode(buffer);
        }
    };

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        STREAM_CODEC.encode(buffer, this);
    }

    @Override
    public String writeToString() {
        return centerColor + ";" + edgeColor + ";" + lifetime + ";" + spinSpeed + ";" + friction + ";" + scale;
    }

    @Override
    public @NotNull ParticleType<GenericParticleOptions> getType() {
        return LyraParticleRegister.Generic.get();
    }
}
