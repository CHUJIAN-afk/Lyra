package first.lyra.register;

import first.lyra.Lyra;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.mesdag.portlib.registries.PortAttributeRegistration;
import org.mesdag.portlib.registries.PortRegisterHandler;
import org.mesdag.portlib.registries.PortRegistryEntry;

public class LyraAttributeRegister {

    private static final PortAttributeRegistration Register = PortRegisterHandler.attribute(Lyra.MODID);

    public static final PortRegistryEntry<Attribute, Attribute> HealthRegen = register("health_regen", 0, -1000000, 1000000);
    public static final PortRegistryEntry<Attribute, Attribute> MinionMaxCount = register("minion_max_count", 1, 0, 1000);
    public static final PortRegistryEntry<Attribute, Attribute> SentryMaxCount = register("sentry_max_count", 1, 0, 1000);
    public static final PortRegistryEntry<Attribute, Attribute> SummonDamage = register("summon_damage", 1, 0, 1000000);
    public static final PortRegistryEntry<Attribute, Attribute> SummonKnockback = register("summon_knockback", 1, 0, 10);
    public static final PortRegistryEntry<Attribute, Attribute> SummonArmorPierce = register("summon_armor_pierce", 0, 0, 1000000);
    public static final PortRegistryEntry<Attribute, Attribute> SummonSearchRange = register("summon_search_range", 1, 0, 10);

    private static PortRegistryEntry<Attribute, Attribute> register(String name, double defaultValue, double min, double max) {
        return Register.register(name, () -> new RangedAttribute(Lyra.rl(name).toString(), defaultValue, min, max), maker -> maker.setSyncable(true));
    }

    public static void register() {
    }
}
