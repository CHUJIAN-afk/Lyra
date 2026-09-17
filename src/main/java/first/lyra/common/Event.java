package first.lyra.common;

import first.lyra.Lyra;
import first.lyra.api.MinionWeaponModifyEvent;
import first.lyra.common.armorSet.ArmorSet;
import first.lyra.common.attachment.DamageInfoData;
import first.lyra.common.attachment.InvincibleData;
import first.lyra.common.attachment.ParticlesData;
import first.lyra.common.dataComponent.MinionWeapon;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraAttributeRegister;
import first.lyra.register.LyraDataComponentRegister;
import first.lyra.register.LyraItemRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@EventBusSubscriber(modid = Lyra.MODID)
public class Event {

    @SubscribeEvent
    public static void item(ModifyDefaultComponentsEvent event) {
        event.getAllItems().forEach(item -> {
            MinionWeapon weapon = item.getDefaultInstance().get(LyraDataComponentRegister.MINION_WEAPON);
            if (weapon != null) {
                MinionWeaponModifyEvent modifyEvent = new MinionWeaponModifyEvent(item, weapon);
                NeoForge.EVENT_BUS.post(modifyEvent);
                event.modify(item, builder -> builder.set(LyraDataComponentRegister.MINION_WEAPON.get(), modifyEvent.toMinionWeapon()));
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void handler(LootTableLoadEvent event) {
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
    public static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            player.level().getData(LyraAttachmentRegister.TargetCache).tick((ServerPlayer) player);
        }
        player.getData(LyraAttachmentRegister.EntityData).tick(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(LevelTickEvent.Post event) {
        ParticlesData.tick(event);
        DamageInfoData.tick(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void handler(LivingEquipmentChangeEvent event) {
        ArmorSet.handler(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void handler(EntityAttributeModificationEvent event) {
        LyraAttributeRegister.handler(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void handler(LivingDamageEvent.Post event) {
        InvincibleData.handler(event);
    }
}
