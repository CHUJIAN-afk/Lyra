package first.lyra.register;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import first.lyra.Lyra;
import first.lyra.common.dataComponent.LyraRarity;
import first.lyra.common.dataComponent.MinionWeapon;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

public class LyraDataComponentRegister {

    private static final Registrate REGISTER = Lyra.REGISTRATE.get();

    public static final RegistryEntry<DataComponentType<?>, DataComponentType<MinionWeapon>> MINION_WEAPON =
            REGISTER.simple("minion_weapon", Registries.DATA_COMPONENT_TYPE, () -> DataComponentType.<MinionWeapon>builder()
                    .persistent(MinionWeapon.CODEC)
                    .networkSynchronized(MinionWeapon.STREAM_CODEC)
                    .build());

    public static final RegistryEntry<DataComponentType<?>, DataComponentType<LyraRarity>> RARITY =
            REGISTER.simple("rarity", Registries.DATA_COMPONENT_TYPE, () -> DataComponentType.<LyraRarity>builder()
                    .persistent(LyraRarity.CODEC)
                    .networkSynchronized(LyraRarity.STREAM_CODEC)
                    .build());

    public static void init() {
    }
}
