package first.lyra.register;

import first.lyra.Lyra;
import first.lyra.common.dataComponent.LyraRarity;
import first.lyra.common.dataComponent.MinionWeapon;
import org.mesdag.portlib.component.PortDataComponentType;
import org.mesdag.portlib.registries.PortDataComponentRegistration;
import org.mesdag.portlib.registries.PortRegisterHandler;
import org.mesdag.portlib.registries.PortRegistryEntry;

public class LyraDataComponentRegister {

    private static final PortDataComponentRegistration Register = PortRegisterHandler.dataComponent(Lyra.MODID);

    public static final PortRegistryEntry<PortDataComponentType<?>, PortDataComponentType<MinionWeapon>> MINION_WEAPON =
            Register.builder("minion_weapon", builder -> builder.persistent(MinionWeapon.CODEC).networkSynchronized(MinionWeapon.STREAM_CODEC));

    public static final PortRegistryEntry<PortDataComponentType<?>, PortDataComponentType<LyraRarity>> RARITY =
            Register.builder("rarity", builder -> builder.persistent(LyraRarity.CODEC).networkSynchronized(LyraRarity.STREAM_CODEC));

    public static void register() {}
}
