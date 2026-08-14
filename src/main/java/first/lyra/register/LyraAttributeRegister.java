package first.lyra.register;

import first.lyra.Lyra;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
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
        event.add(EntityTypes.PLAYER, LyraAttributeRegister.MinionMaxCount);
        event.add(EntityTypes.PLAYER, LyraAttributeRegister.SentryMaxCount);
        event.add(EntityTypes.PLAYER, LyraAttributeRegister.MinionDamage);
        event.add(EntityTypes.PLAYER, LyraAttributeRegister.MinionKnockback);
        event.add(EntityTypes.PLAYER, LyraAttributeRegister.MinionArmorPierce);
        event.add(EntityTypes.PLAYER, LyraAttributeRegister.MinionSearchRange);
    }

    private static final DeferredRegister<Attribute> Register = DeferredRegister.create(Registries.ATTRIBUTE, Lyra.MODID);

    public static final DeferredHolder<Attribute, Attribute> HealthRegen = register("health_regen", 0, -1000000, 1000000);
    public static final DeferredHolder<Attribute, Attribute> MinionMaxCount = register("minion_max_count", 1, 0, 1000);
    public static final DeferredHolder<Attribute, Attribute> SentryMaxCount = register("sentry__max_count", 1, 0, 1000);
    public static final DeferredHolder<Attribute, Attribute> MinionDamage = register("minion_damage", 1, 0, 1000000);
    public static final DeferredHolder<Attribute, Attribute> MinionKnockback = register("minion_knockback", 1, 0, 10);
    public static final DeferredHolder<Attribute, Attribute> MinionArmorPierce = register("minion_armor_pierce", 1, 0, 1000000);
    public static final DeferredHolder<Attribute, Attribute> MinionSearchRange = register("minion_search_range", 1, 0, 10);

    private static DeferredHolder<Attribute, Attribute> register(String name, double defaultValue, double min, double max) {
        return Register.register(name, () -> new RangedAttribute(Lyra.id(name).toString(), defaultValue, min, max).setSyncable(true));
    }

    public static void register(IEventBus eventBus) {
        Register.register(eventBus);
    }
}
