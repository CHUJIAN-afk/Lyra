package first.lyra.register;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import first.lyra.Lyra;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public class LyraAttributeRegister {

    private static final Registrate REGISTER = Lyra.REGISTRATE.get();

    public static final RegistryEntry<Attribute, Attribute> HealthRegen = register("health_regen", 0, -1000000, 1000000);
    public static final RegistryEntry<Attribute, Attribute> MinionMaxCount = register("minion_max_count", 1, 0, 1000);
    public static final RegistryEntry<Attribute, Attribute> SentryMaxCount = register("sentry_max_count", 1, 0, 1000);
    public static final RegistryEntry<Attribute, Attribute> SummonDamage = register("summon_damage", 1, 0, 1000000);
    public static final RegistryEntry<Attribute, Attribute> SummonKnockback = register("summon_knockback", 1, 0, 10);
    public static final RegistryEntry<Attribute, Attribute> SummonArmorPierce = register("summon_armor_pierce", 0, 0, 1000000);
    public static final RegistryEntry<Attribute, Attribute> SummonSearchRange = register("summon_search_range", 1, 0, 10);

    private static RegistryEntry<Attribute, Attribute> register(String name, double defaultValue, double min, double max) {
        return REGISTER.simple(name, Registries.ATTRIBUTE, () -> new RangedAttribute(Util.makeDescriptionId("attribute", Lyra.rl(name)), defaultValue, min, max).setSyncable(true));
    }

    public static void init() {
    }
}
