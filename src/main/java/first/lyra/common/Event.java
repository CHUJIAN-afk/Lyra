package first.lyra.common;

import first.lyra.Lyra;
import first.lyra.api.MinionWeaponModifyEvent;
import first.lyra.common.attachment.*;
import first.lyra.common.attachmentEntity.AttachmentEntityDamageSource;
import first.lyra.common.dataComponent.MinionWeapon;
import first.lyra.common.summonMark.SummonMarkInstance;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraAttributeRegister;
import first.lyra.register.LyraDataComponentRegister;
import first.lyra.register.LyraItemRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

@EventBusSubscriber(modid = Lyra.MODID)
public class Event {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        Stream<Item> allItems = event.getAllItems();
        allItems.forEach(item -> {
            MinionWeapon weapon = item.getDefaultInstance().get(LyraDataComponentRegister.MINION_WEAPON);
            if (weapon != null) {
                MinionWeaponModifyEvent modifyEvent = new MinionWeaponModifyEvent(item, weapon);
                NeoForge.EVENT_BUS.post(modifyEvent);
                event.modify(item, builder -> builder.set(LyraDataComponentRegister.MINION_WEAPON.get(), modifyEvent.toMinionWeapon()));
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation location = event.getName();
        LootTable table = event.getTable();
        LyraItemRegistries.REGISTRIES.values().forEach(registries -> {
            Map<ResourceLocation, List<Function<LootTable, LootPool>>> tableData = registries.lootTableData;
            List<Function<LootTable, LootPool>> functions = tableData.get(location);
            if (functions != null) {
                for (Function<LootTable, LootPool> function : functions) {
                    table.addPool(function.apply(table));
                }
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide() && entity instanceof LivingEntity living) {
            living.getData(LyraAttachmentRegister.HealthData).tick();
            living.getData(LyraAttachmentRegister.InvincibleData).tick();
            living.getData(LyraAttachmentRegister.ImmunityData).tick();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            player.getData(LyraAttachmentRegister.TargetCache).tick();
            player.getData(LyraAttachmentRegister.SummonMarkData).tick();
        }
        player.getData(LyraAttachmentRegister.EntityData).tick();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        ParticlesData.tick(event);
        DamageInfoData.tick(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        if (!target.level().isClientSide() && source instanceof AttachmentEntityDamageSource damageSource && damageSource.getEntity() instanceof Player attacker && target != attacker) {
            SummonMarkTracker tracker = attacker.getData(LyraAttachmentRegister.SummonMarkData);
            if (tracker.isTarget(target)) {
                List<SummonMarkInstance> summonMarkInstances = tracker.getSummonMarkInstances();
                for (SummonMarkInstance instance : summonMarkInstances) {
                    event.setNewDamage(instance.getType().damagePre(tracker, instance, target, damageSource, event.getNewDamage()));
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        Level level = target.level();
        if (!level.isClientSide()) {
            if (target instanceof Player player && source.getEntity() instanceof LivingEntity attacker && target != attacker) {
                player.getData(LyraAttachmentRegister.TargetCache).record(attacker, 200);
            }
            if (source.getEntity() instanceof Player player && target != player) {
                player.getData(LyraAttachmentRegister.TargetCache).record(target, 200);
            }
        }
        if (!level.isClientSide() && source instanceof AttachmentEntityDamageSource damageSource) {
            Player owner = damageSource.getOwner();
            SummonMarkTracker tracker = owner.getData(LyraAttachmentRegister.SummonMarkData);
            if (tracker.isTarget(target)) {
                List<SummonMarkInstance> summonMarkInstances = tracker.getSummonMarkInstances();
                if (source.getEntity() instanceof LivingEntity attacker && target != attacker) {
                    for (SummonMarkInstance instance : summonMarkInstances) {
                        instance.getType().damagePost(tracker, instance, target, damageSource, event.getNewDamage());
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeathPost(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        Level level = target.level();
        if (!event.isCanceled() && !level.isClientSide() && source instanceof AttachmentEntityDamageSource damageSource) {
            Player owner = damageSource.getOwner();
            SummonMarkTracker tracker = owner.getData(LyraAttachmentRegister.SummonMarkData);
            if (tracker.isTarget(target)) {
                List<SummonMarkInstance> summonMarkInstances = tracker.getSummonMarkInstances();
                if (source.getEntity() instanceof LivingEntity attacker && target != attacker) {
                    for (SummonMarkInstance instance : summonMarkInstances) {
                        instance.getType().kill(tracker, instance, target, damageSource);
                    }
                }
            }
        }
    }
}
