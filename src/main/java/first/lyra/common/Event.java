package first.lyra.common;

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
import org.mesdag.portlib.event.PortEventHandler;
import org.mesdag.portlib.event.PortEventPriority;
import org.mesdag.portlib.event.entity.PortEntityAttributeModificationEvent;
import org.mesdag.portlib.event.entity.living.PortLivingDamageEvent;
import org.mesdag.portlib.event.entity.living.PortLivingDeathEvent;
import org.mesdag.portlib.event.other.PortLootTableLoadEvent;
import org.mesdag.portlib.event.other.PortModifyDefaultComponentsEvent;
import org.mesdag.portlib.event.tick.PortEntityTickEvent;
import org.mesdag.portlib.event.tick.PortLevelTickEvent;
import org.mesdag.portlib.event.tick.PortPlayerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class Event {

    public static void init() {
        //PortEventHandler.addListener(PortEventPriority.LOWEST, false, PortModifyDefaultComponentsEvent.class, Event::onModifyDefaultComponents);
        PortEventHandler.addListener(PortEventPriority.LOWEST, false, PortLootTableLoadEvent.class, Event::onLootTableLoad);
        PortEventHandler.addListener(PortEventPriority.LOWEST, false, PortEntityAttributeModificationEvent.class, Event::onEntityAttributeModification);
        PortEventHandler.addListener(PortEventPriority.LOWEST, false, PortEntityTickEvent.Post.class, Event::onEntityTickPost);
        PortEventHandler.addListener(PortEventPriority.LOWEST, false, PortPlayerTickEvent.Post.class, Event::onPlayerTickPost);
        PortEventHandler.addListener(PortEventPriority.LOWEST, false, PortLevelTickEvent.Post.class, Event::onLevelTickPost);
        PortEventHandler.addListener(PortEventPriority.LOWEST, false, PortLivingDamageEvent.Pre.class, Event::onLivingDamagePre);
        PortEventHandler.addListener(PortEventPriority.LOWEST, false, PortLivingDamageEvent.Post.class, Event::onLivingDamagePost);
        PortEventHandler.addListener(PortEventPriority.LOWEST, false, PortLivingDeathEvent.class, Event::onLivingDeathPost);
    }

    public static void onModifyDefaultComponents(PortModifyDefaultComponentsEvent event) {
        Stream<Item> allItems = event.getAllItems();
        allItems.forEach(item -> {
            MinionWeapon weapon = item.getDefaultInstance().get(LyraDataComponentRegister.MINION_WEAPON.get());
            if (weapon != null) {
                MinionWeaponModifyEvent modifyEvent = new MinionWeaponModifyEvent(item, weapon);
                PortEventHandler.postEvent(modifyEvent);
                event.modify(item, builder -> builder.set(LyraDataComponentRegister.MINION_WEAPON.get(), modifyEvent.toMinionWeapon()));
            }
        });
    }

    public static void onLootTableLoad(PortLootTableLoadEvent event) {
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

    public static void onEntityAttributeModification(PortEntityAttributeModificationEvent event) {
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

    public static void onEntityTickPost(PortEntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide() && entity instanceof LivingEntity living) {
            living.getData(LyraAttachmentRegister.HealthData).tick();
            living.getData(LyraAttachmentRegister.InvincibleData).tick();
            living.getData(LyraAttachmentRegister.ImmunityData).tick();
        }
    }

    public static void onPlayerTickPost(PortPlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            player.getData(LyraAttachmentRegister.TargetCache).tick();
            player.getData(LyraAttachmentRegister.SummonMarkData).tick();
        }
        player.getData(LyraAttachmentRegister.EntityData).tick();
    }

    public static void onLevelTickPost(PortLevelTickEvent.Post event) {
        ParticlesData.tick(event);
    }

    public static void onLivingDamagePre(PortLivingDamageEvent.Pre event) {
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

    public static void onLivingDamagePost(PortLivingDamageEvent.Post event) {
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

    public static void onLivingDeathPost(PortLivingDeathEvent event) {
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
