package first.lyra.common;

import first.lyra.Lyra;
import first.lyra.common.armorSet.ArmorSet;
import first.lyra.common.attachment.DamageInfoData;
import first.lyra.common.attachment.InvincibleData;
import first.lyra.common.attachment.ParticlesData;
import first.lyra.register.LyraAttributeRegister;
import first.lyra.register.LyraItemRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@EventBusSubscriber(modid = Lyra.MODID)
public class Event {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void handler(LootTableLoadEvent event) {
        ResourceLocation location = event.getName();
        LootTable table = event.getTable();
        LyraItemRegistries.REGISTRIES.values().forEach(registries -> {
            Map<ResourceLocation, List<Function<LootTable, LootPool>>> tableData = registries.lootTableData;
            List<Function<LootTable, LootPool>> functions = tableData.get(location);
            if (functions != null) {
                for (Function<LootTable, LootPool> function : functions) {
                    function.apply(table);
                }
            }
        });
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
