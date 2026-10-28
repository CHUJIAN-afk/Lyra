package first.lyra.provider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.AddTableLootModifier;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

// 向箱子与实体战利品表追加战利品：子表生成到 with/ 下，再由全局战利品修饰器挂到目标表。
public abstract class LyraAbstractLootProvider {

    private final String modid;
    private final Map<ResourceKey<LootTable>, List<LootPool.Builder>> entityLoot = new LinkedHashMap<>();
    private final Map<ResourceKey<LootTable>, List<LootPool.Builder>> chestLoot = new LinkedHashMap<>();

    protected LyraAbstractLootProvider(String modid) {
        this.modid = modid;
    }

    public void init() {
        Optional<IEventBus> optional = ModList.get().getModContainerById(modid).map(ModContainer::getEventBus);
        optional.ifPresent(eventBus -> eventBus.addListener((GatherDataEvent event) -> {
            loot();
            DataGenerator generator = event.getGenerator();
            PackOutput output = generator.getPackOutput();
            CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
            generator.addProvider(event.includeServer(), new LootTables(output, lookup, modid, entityLoot, chestLoot));
            generator.addProvider(event.includeServer(), new LootModifiers(output, lookup, modid, entityLoot.keySet(), chestLoot.keySet()));
        }));
    }

    public abstract void loot();

    public void addEntityLoot(EntityType<?> entityType, LootPool.Builder builder) {
        entityLoot.computeIfAbsent(entityType.getDefaultLootTable(), key -> new ArrayList<>()).add(builder);
    }

    public void addChestLoot(ResourceKey<LootTable> lootTable, LootPool.Builder builder) {
        chestLoot.computeIfAbsent(lootTable, key -> new ArrayList<>()).add(builder);
    }

    private static ResourceKey<LootTable> subTable(String modid, ResourceKey<LootTable> lootTable) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(modid, "with/" + lootTable.location().getPath()));
    }

    private static class LootTables extends LootTableProvider {

        private LootTables(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, String modid, Map<ResourceKey<LootTable>, List<LootPool.Builder>> entityLoot, Map<ResourceKey<LootTable>, List<LootPool.Builder>> chestLoot) {
            super(output, Set.of(), List.of(
                    new SubProviderEntry(provider -> subProvider(modid, entityLoot), LootContextParamSets.ENTITY),
                    new SubProviderEntry(provider -> subProvider(modid, chestLoot), LootContextParamSets.CHEST)
            ), lookup);
        }

        private static LootTableSubProvider subProvider(String modid, Map<ResourceKey<LootTable>, List<LootPool.Builder>> loot) {
            return output -> loot.forEach((lootTable, pools) -> {
                LootTable.Builder builder = LootTable.lootTable();
                pools.forEach(builder::withPool);
                output.accept(subTable(modid, lootTable), builder);
            });
        }
    }

    private static class LootModifiers extends GlobalLootModifierProvider {

        private final String modid;
        private final Set<ResourceKey<LootTable>> entityLoot;
        private final Set<ResourceKey<LootTable>> chestLoot;

        private LootModifiers(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, String modid, Set<ResourceKey<LootTable>> entityLoot, Set<ResourceKey<LootTable>> chestLoot) {
            super(output, lookup, modid);
            this.modid = modid;
            this.entityLoot = entityLoot;
            this.chestLoot = chestLoot;
        }

        @Override
        protected void start() {
            addModifiers(entityLoot);
            addModifiers(chestLoot);
        }

        private void addModifiers(Set<ResourceKey<LootTable>> lootTables) {
            lootTables.forEach(lootTable -> add(lootTable.location().getPath(), new AddTableLootModifier(
                    new LootItemCondition[]{LootTableIdCondition.builder(lootTable.location()).build()},
                    subTable(modid, lootTable))));
        }
    }
}
