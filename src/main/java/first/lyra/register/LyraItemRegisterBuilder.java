package first.lyra.register;

import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.entity.AttachmentEntityType;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class LyraItemRegisterBuilder<T extends Item> {

    private final LyraItemRegistries lyraItemRegistries;
    private final DeferredItem<T> register;

    public LyraItemRegisterBuilder(LyraItemRegistries lyraItemRegistries, DeferredItem<T> register) {
        this.lyraItemRegistries = lyraItemRegistries;
        this.register = register;
    }

    public LyraItemRegisterBuilder<T> lootTable(ResourceLocation location, Function<LootTable, LootPool> function) {
        lyraItemRegistries.lootTableData.computeIfAbsent(location, key -> new ArrayList<>()).add(function);
        return this;
    }

    public LyraItemRegisterBuilder<T> language(String key, String enDesc, String zhDesc) {
        if (lyraItemRegistries.isDevelopment()) {
            lyraItemRegistries.languageGenerate.put(key, new String[]{enDesc, zhDesc});
        }
        return this;
    }

    public LyraItemRegisterBuilder<T> jeiInfo(int index, String enDesc, String zhDesc) {
        ResourceLocation id = register.getId();
        String key = "item." + id.getNamespace() + "." + id.getPath() + "jei.description." + index;
        if (ModList.get().isLoaded("jei") && FMLLoader.getDist().isClient()) {
            lyraItemRegistries.jeiInfoData.computeIfAbsent(register, like -> new ArrayList<>()).add(Component.translatable(key));
        }
        return language(key, enDesc, zhDesc);
    }

    public LyraItemRegisterBuilder<T> itemLanguage(String en, String zh) {
        return language("item." + register.getId().toLanguageKey(), en, zh);
    }

    public LyraItemRegisterBuilder<T> itemLanguageTooltip(int index, String en, String zh) {
        return language("item." + register.getId().toLanguageKey() + ".tooltip." + index, en, zh);
    }

    public <A extends AttachmentEntity> LyraItemRegisterBuilder<T> summonLanguage(DeferredHolder<AttachmentEntityType<?>, AttachmentEntityType<A>> holder, String en, String zh) {
        return language("summon." + holder.getId().toLanguageKey(), en, zh);
    }

    public LyraItemRegisterBuilder<T> blockLanguage(String en, String zh) {
        return language("block." + register.getId().toLanguageKey(), en, zh);
    }
    public LyraItemRegisterBuilder<T> recipeWithLookup(BiConsumer<HolderLookup.Provider, RecipeOutput> outputConsumer) {
        if (lyraItemRegistries.isDevelopment()) {
            lyraItemRegistries.recipesGenerate.add(outputConsumer);
        }
        return this;
    }

    public LyraItemRegisterBuilder<T> itemTag(TagKey<Item> tagKey) {
        if (lyraItemRegistries.isDevelopment()) {
            lyraItemRegistries.itemTagsGenerate.computeIfAbsent(tagKey, key -> new ArrayList<>()).add(register);
        }
        return this;
    }

    public LyraItemRegisterBuilder<T> itemModel(BiConsumer<ItemModelProvider, ResourceLocation> consumer) {
        if (lyraItemRegistries.isDevelopment()) {
            lyraItemRegistries.itemModelGenerate.put(register.getId(), consumer);
        }
        return this;
    }

    public DeferredItem<T> build() {
        return register;
    }
}
