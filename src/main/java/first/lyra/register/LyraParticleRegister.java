package first.lyra.register;

import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import first.lyra.Lyra;
import first.lyra.common.particle.genericParticle.GenericParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public class LyraParticleRegister {

    private static final Registrate REGISTER = Lyra.REGISTRATE.get();

    public static final RegistryEntry<ParticleType<?>, ParticleType<GenericParticleOptions>> Generic =
            REGISTER.simple("generic", Registries.PARTICLE_TYPE, () -> new ParticleType<GenericParticleOptions>(true) {
                @Override
                public @NotNull MapCodec<GenericParticleOptions> codec() {
                    return GenericParticleOptions.CODEC;
                }

                @Override
                public @NotNull StreamCodec<RegistryFriendlyByteBuf, GenericParticleOptions> streamCodec() {
                    return GenericParticleOptions.STREAM_CODEC;
                }
            });

    public static void init() {
    }
}
