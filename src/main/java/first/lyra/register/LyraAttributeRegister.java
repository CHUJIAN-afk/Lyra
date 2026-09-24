package first.lyra.register;

import first.lyra.Lyra;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class LyraAttributeRegister {

    private static final DeferredRegister<Attribute> Register = DeferredRegister.create(Registries.ATTRIBUTE, Lyra.MODID);

    public static final DeferredHolder<Attribute, Attribute> HealthRegen = register("health_regen", 0, -1000000, 1000000);
    public static final DeferredHolder<Attribute, Attribute> MinionMaxCount = register("minion_max_count", 1, 0, 1000);
    public static final DeferredHolder<Attribute, Attribute> SentryMaxCount = register("sentry_max_count", 1, 0, 1000);
    public static final DeferredHolder<Attribute, Attribute> SummonDamage = register("summon_damage", 1, 0, 1000000);
    public static final DeferredHolder<Attribute, Attribute> SummonKnockback = register("summon_knockback", 1, 0, 10);
    public static final DeferredHolder<Attribute, Attribute> SummonArmorPierce = register("summon_armor_pierce", 0, 0, 1000000);
    public static final DeferredHolder<Attribute, Attribute> SummonSearchRange = register("summon_search_range", 1, 0, 10);

    private static DeferredHolder<Attribute, Attribute> register(String name, double defaultValue, double min, double max) {
        return Register.register(name, () -> new RangedAttribute(Lyra.rl(name).toString(), defaultValue, min, max).setSyncable(true));
    }

    public static void register(IEventBus eventBus) {
        Register.register(eventBus);
    }
}
