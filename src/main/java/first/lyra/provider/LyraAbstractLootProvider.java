package first.lyra.provider;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.providers.ProviderType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class LyraAbstractLootProvider {

    private final Map<LootContextParamSet, Map<ResourceKey<LootTable>, List<LootPool.Builder>>> lootTables = new HashMap<>();

    public abstract void loot();

    public void add(LootContextParamSet lootContextParamSet, ResourceKey<LootTable> lootTable, LootPool.Builder builder) {
        lootTables.computeIfAbsent(lootContextParamSet, k -> new HashMap<>()).computeIfAbsent(lootTable,k -> new ArrayList<>()).add(builder);
    }

    public void init(Registrate registrate) {
        registrate.addDataGenerator(ProviderType.LOOT, provider -> {
            lootTables.forEach((lootContextParamSet, resourceKeyBuilderMap) -> provider.addLootAction(lootContextParamSet, consumer -> {
                resourceKeyBuilderMap.forEach((tableResourceKey, value) -> {
                    for (LootPool.Builder builder : value) {
                        consumer.accept(tableResourceKey, LootTable.lootTable().withPool(builder));
                    }
                });
            }));
        });
    }
}
