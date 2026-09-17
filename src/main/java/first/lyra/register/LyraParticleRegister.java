package first.lyra.register;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import first.lyra.Lyra;
import first.lyra.common.particle.genericParticle.GenericParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

public class LyraParticleRegister {

    private static final DeferredRegister<ParticleType<?>> Register = DeferredRegister.create(Registries.PARTICLE_TYPE, Lyra.MODID);

    public static final RegistryObject<ParticleType<GenericParticleOptions>> Generic = Register.register("generic", () -> new ParticleType<>(true, GenericParticleOptions.DESERIALIZER) {
        @Override
        public @NotNull Codec<GenericParticleOptions> codec() {
            return GenericParticleOptions.CODEC.codec();
        }
    });

    public static void register(IEventBus eventBus) {
        Register.register(eventBus);
    }
}
