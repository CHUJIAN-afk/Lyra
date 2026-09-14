package first.lyra.register;

import first.lyra.Lyra;
import first.lyra.common.attachmentEntity.LivingAttachmentEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = Lyra.MODID)
public class LyraEntityRegister {

    private static final DeferredRegister<EntityType<?>> Register = DeferredRegister.create(Registries.ENTITY_TYPE, Lyra.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<LivingAttachmentEntity.HurtEntity>> HurtEntity = Register.register("hurt_entity", () -> EntityType.Builder.of(LivingAttachmentEntity.HurtEntity::new, MobCategory.MISC).sized(0.0F, 0.0F).noSummon().noSave().fireImmune().clientTrackingRange(10).updateInterval(1).build("hurt_entity"));

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(HurtEntity.get(), Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.GRAVITY, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).build());
    }

    public static void register(IEventBus eventBus) {
        Register.register(eventBus);
    }
}
