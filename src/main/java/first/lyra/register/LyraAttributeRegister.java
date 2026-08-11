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
        event.add(EntityType.PLAYER, LyraAttributeRegister.ServantMaxCount);
        event.add(EntityType.PLAYER, LyraAttributeRegister.SentryServantMaxCount);
        event.add(EntityType.PLAYER, LyraAttributeRegister.ServantDamage);
        event.add(EntityType.PLAYER, LyraAttributeRegister.ServantKnockback);
        event.add(EntityType.PLAYER, LyraAttributeRegister.ServantArmorPierce);
        event.add(EntityType.PLAYER, LyraAttributeRegister.ServantSearchRange);
    }

    private static final DeferredRegister<Attribute> Register = DeferredRegister.create(Registries.ATTRIBUTE, Lyra.MODID);

    public static final DeferredHolder<Attribute, Attribute> HealthRegen = register("health_regen", 0, -1000000, 1000000);
    public static final DeferredHolder<Attribute, Attribute> ServantMaxCount = register("servant_max_count", 1, 0, 1000);
    public static final DeferredHolder<Attribute, Attribute> SentryServantMaxCount = register("sentry_servant_max_count", 1, 0, 1000);
    public static final DeferredHolder<Attribute, Attribute> ServantDamage = register("servant_damage", 1, 0, 1000000);
    public static final DeferredHolder<Attribute, Attribute> ServantKnockback = register("servant_knockback", 1, 0, 10);
    public static final DeferredHolder<Attribute, Attribute> ServantArmorPierce = register("servant_armor_pierce", 1, 0, 1000000);
    public static final DeferredHolder<Attribute, Attribute> ServantSearchRange = register("servant_search_range", 1, 0, 10);

    private static DeferredHolder<Attribute, Attribute> register(String name, double defaultValue, double min, double max) {
        return Register.register(name, () -> new RangedAttribute(Lyra.id(name).toString(), defaultValue, min, max).setSyncable(true));
    }

    public static void register(IEventBus eventBus) {
        Register.register(eventBus);
    }
}
