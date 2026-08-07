package first.lyra.register;

import com.mojang.serialization.MapCodec;
import first.lyra.Lyra;
import first.lyra.common.particle.genericParticle.GenericParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

public class LyraParticleRegister {

    private static final DeferredRegister<ParticleType<?>> Register = DeferredRegister.create(Registries.PARTICLE_TYPE, Lyra.MODID);

    public static final DeferredHolder<ParticleType<?>, ParticleType<GenericParticleOptions>> Generic = Register.register("generic", () -> new ParticleType<>(true) {
        @Override
        public @NotNull MapCodec<GenericParticleOptions> codec() {
            return GenericParticleOptions.CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, GenericParticleOptions> streamCodec() {
            return GenericParticleOptions.STREAM_CODEC;
        }
    });

    public static void register(IEventBus eventBus) {
        Register.register(eventBus);
    }
}