package first.lyra.common;

import first.lyra.common.armorSet.ArmorSet;
import first.lyra.common.attachment.DamageInfoData;
import first.lyra.common.attachment.InvincibleData;
import first.lyra.common.attachment.ParticlesData;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraItemRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import org.mesdag.portlib.event.PortEventHandler;
import org.mesdag.portlib.event.PortEventPriority;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ForgeGameEvent {
    public static void init() {
        PortEventHandler.addListener(ForgeGameEvent::lootTable);
        PortEventHandler.addListener(ForgeGameEvent::playerTick);
        PortEventHandler.addListener(ForgeGameEvent::levelTick);
        PortEventHandler.addListener(ForgeGameEvent::equipmentChange);
        PortEventHandler.addListener(PortEventPriority.LOWEST, ForgeGameEvent::damage);
    }

    private static void lootTable(LootTableLoadEvent event) {
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

    private static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (!player.level().isClientSide()) {
            player.getData(LyraAttachmentRegister.TargetCache).tick((ServerPlayer) player);
        }
        player.getData(LyraAttachmentRegister.EntityData).tick(player);
    }

    private static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ParticlesData.tick(event);
        DamageInfoData.tick(event);
    }

    private static void equipmentChange(LivingEquipmentChangeEvent event) {
        ArmorSet.handler(event);
    }

    private static void damage(LivingDamageEvent event) {
        InvincibleData.handler(event);
        DamageInfoData.handler(event);
    }
}
