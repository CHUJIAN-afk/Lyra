package first.lyra.register;

import first.lyra.Lyra;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class LyraAttributeRegister {

    public static void handler(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            event.add(type, LyraAttributeRegister.HealthRegen);
        }
        event.add(EntityType.PLAYER, LyraAttributeRegister.MinionMaxCount);
        event.add(EntityType.PLAYER, LyraAttributeRegister.SentryMaxCount);
        event.add(EntityType.PLAYER, LyraAttributeRegister.SummonDamage);
        event.add(EntityType.PLAYER, LyraAttributeRegister.SummonKnockback);
        event.add(EntityType.PLAYER, LyraAttributeRegister.SummonArmorPierce);
        event.add(EntityType.PLAYER, LyraAttributeRegister.SummonSearchRange);
    }

    private static final DeferredRegister<Attribute> Register = DeferredRegister.create(Registries.ATTRIBUTE, Lyra.MODID);

    public static final DeferredHolder<Attribute, Attribute> HealthRegen = register("health_regen", 0, -1000000, 1000000);
    public static final DeferredHolder<Attribute, Attribute> MinionMaxCount = register("minion_max_count", 1, 0, 1000);
    public static final DeferredHolder<Attribute, Attribute> SentryMaxCount = register("sentry_max_count", 1, 0, 1000);
    public static final DeferredHolder<Attribute, Attribute> SummonDamage = register("summon_damage", 1, 0, 1000000);
    public static final DeferredHolder<Attribute, Attribute> SummonKnockback = register("summon_knockback", 1, 0, 10);
    public static final DeferredHolder<Attribute, Attribute> SummonArmorPierce = register("summon_armor_pierce", 1, 0, 1000000);
    public static final DeferredHolder<Attribute, Attribute> SummonSearchRange = register("summon_search_range", 1, 0, 10);

    private static DeferredHolder<Attribute, Attribute> register(String name, double defaultValue, double min, double max) {
        return Register.register(name, () -> new RangedAttribute(Lyra.rl(name).toString(), defaultValue, min, max).setSyncable(true));
    }

    public static void register(IEventBus eventBus) {
        Register.register(eventBus);
    }
}
