package first.lyra.register;

import first.lyra.Lyra;
import first.lyra.common.dataComponent.MinionWeapon;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class LyraDataComponentRegister {

    private static final DeferredRegister.DataComponents Register =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Lyra.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MinionWeapon>> MINION_WEAPON =
            Register.registerComponentType("minion_weapon", builder -> builder.persistent(MinionWeapon.CODEC).networkSynchronized(MinionWeapon.STREAM_CODEC));

    public static void register(IEventBus eventBus) {
        Register.register(eventBus);
    }
}
