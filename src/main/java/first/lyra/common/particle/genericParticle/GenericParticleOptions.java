package first.lyra.common.particle.genericParticle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import first.lyra.register.LyraParticleRegister;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.mesdag.portlib.network.PortRegistryFriendlyByteBuf;
import org.mesdag.portlib.network.codec.PortStreamCodec;
import org.mesdag.portlib.wrapper.common.extensions.IPortFriendlyByteBufExtension;

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

    public static final PortStreamCodec<PortRegistryFriendlyByteBuf, GenericParticleOptions> STREAM_CODEC = PortStreamCodec.ofMember(
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

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        STREAM_CODEC.encode(IPortFriendlyByteBufExtension.of(buffer).wrap(), this);
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
