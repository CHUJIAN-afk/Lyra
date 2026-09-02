package first.lyra.common.lootTable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class LootTableManager {

    private static final Map<ResourceLocation, List<Function<LootTable, LootPool>>> data = new HashMap<>();

    public static void register(ResourceLocation identifier, Function<LootTable, LootPool> function) {
        data.computeIfAbsent(identifier, k -> new ArrayList<>()).add(function);
    }

    public static void addToLootTable(ResourceLocation identifier, LootTable lootTable) {
        List<Function<LootTable, LootPool>> functions = data.get(identifier);
        if (functions != null) {
            for (Function<LootTable, LootPool> function : functions) {
                lootTable.addPool(function.apply(lootTable));
            }
        }
    }
}
