package first.lyra.register;

import first.lyra.Lyra;
import first.lyra.common.particle.genericParticle.GenericParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import org.mesdag.portlib.registries.PortParticleTypeRegistration;
import org.mesdag.portlib.registries.PortRegisterHandler;
import org.mesdag.portlib.registries.PortRegistryEntry;

public class LyraParticleRegister {

    private static final PortParticleTypeRegistration Register = PortRegisterHandler.particleType(Lyra.MODID);

    public static final PortRegistryEntry<ParticleType<?>, ParticleType<GenericParticleOptions>> Generic =
            Register.register("generic", true, GenericParticleOptions.CODEC, GenericParticleOptions.STREAM_CODEC);

    public static void register(IEventBus eventBus) {
    }
}
