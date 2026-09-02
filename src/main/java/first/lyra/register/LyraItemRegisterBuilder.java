package first.lyra.register;

import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.entity.AttachmentEntityType;
import first.lyra.common.lootTable.LootTableManager;
import first.lyra.compat.jei.LyraPlugin;
import first.lyra.dataGenerator.provider.LyraItemModelProvider;
import first.lyra.dataGenerator.provider.LyraItemTagsProvider;
import first.lyra.dataGenerator.provider.LyraLanguageProvider;
import first.lyra.dataGenerator.provider.LyraRecipeProvider;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class LyraItemRegisterBuilder<T extends Item> {

    private final DeferredItem<T> register;

    private LyraItemRegisterBuilder(DeferredItem<T> register) {
        this.register = register;
    }

    /** 使用宿主 mod 的 DeferredRegister 注册物品。 */
    public static <T extends Item> LyraItemRegisterBuilder<T> build(DeferredRegister.Items register, String name, Supplier<T> supplier) {
        return new LyraItemRegisterBuilder<>(register.register(name, supplier));
    }

    /** 使用宿主 mod 的 DeferredRegister 注册物品。 */
    public static <T extends Item> LyraItemRegisterBuilder<T> build(DeferredRegister.Items register, String name, Function<ResourceLocation, T> function) {
        return new LyraItemRegisterBuilder<>(register.register(name, function));
    }

    /** 使用宿主 mod 的 DeferredRegister 注册物品。 */
    public static LyraItemRegisterBuilder<Item> build(DeferredRegister.Items register, String name) {
        return build(register, name, () -> new Item(new Item.Properties()));
    }

    public LyraItemRegisterBuilder<T> jeiInfo(int index, String enDesc, String zhDesc) {
        if (register != null) {
            ResourceLocation id = register.getId();
            String key = "item." + id.getNamespace() + "." + id.getPath() + "jei.description." + index;
            LyraLanguageProvider.addIngredientInfo(register, Component.translatable(key));
            return language(key, enDesc, zhDesc);
        }
        return this;
    }

    public LyraItemRegisterBuilder<T> language(String key, String enDesc, String zhDesc) {
        if (!FMLLoader.isProduction()) {
            LyraLanguageProvider.entry(key, enDesc, zhDesc);
        }
        return this;
    }

    public LyraItemRegisterBuilder<T> lootTable(ResourceLocation identifier, Function<LootTable, LootPool> function) {
        LootTableManager.register(identifier, function);
        return this;
    }

    public LyraItemRegisterBuilder<T> lootTable(ResourceLocation identifier, Supplier<LootPool> supplier) {
        LootTableManager.register(identifier, lootTable -> supplier.get());
        return this;
    }

    public LyraItemRegisterBuilder<T> itemLanguage(String en, String zh) {
        if (register != null) {
            ResourceLocation id = register.getId();
            return language("item." + id.getNamespace() + "." + id.getPath(), en, zh);
        }
        return this;
    }

    public LyraItemRegisterBuilder<T> itemLanguageTooltip(int index, String en, String zh) {
        if (register != null) {
            ResourceLocation id = register.getId();
            return language("item." + id.getNamespace() + "." + id.getPath() + ".tooltip." + index, en, zh);
        }
        return this;
    }

    public LyraItemRegisterBuilder<T> blockLanguage(String en, String zh) {
        if (register != null) {
            ResourceLocation id = register.getId();
            return language("block." + id.getNamespace() + "." + id.getPath(), en, zh);
        }
        return this;
    }

    public <A extends AttachmentEntity> LyraItemRegisterBuilder<T> summonLanguage(DeferredHolder<AttachmentEntityType<?>, AttachmentEntityType<A>> holder, String en, String zh) {
        ResourceLocation minionId = holder.getId();
        return language("summon." + minionId.getNamespace() + "." + minionId.getPath(), en, zh);
    }

    /** 注册配方（runData 时输出）。 */
    public LyraItemRegisterBuilder<T> recipe(Consumer<RecipeOutput> outputConsumer) {
        if (!FMLLoader.isProduction()) {
            LyraRecipeProvider.RecipeGenerate.add(outputConsumer);
        }
        return this;
    }

    /** 注册配方（runData 时输出，回调额外提供 HolderLookup，供 ShapedRecipeBuilder 等需要 HolderGetter 的构建器使用）。 */
    public LyraItemRegisterBuilder<T> recipeWithLookup(BiConsumer<HolderLookup.Provider, RecipeOutput> outputConsumer) {
        if (!FMLLoader.isProduction()) {
            LyraRecipeProvider.RecipeGenerateWithLookup.add(outputConsumer);
        }
        return this;
    }

    /** 注册物品标签（runData 时输出，护甲自动细分到原版护甲分类标签）。 */
    public LyraItemRegisterBuilder<T> itemTag(TagKey<Item> tagKey) {
        if (!FMLLoader.isProduction()) {
            LyraItemTagsProvider.ItemTagsGenerate.computeIfAbsent(tagKey, key -> new ArrayList<>()).add(register);
        }
        return this;
    }

    /** 注册物品模型（runData 时输出）。 */
    public LyraItemRegisterBuilder<T> itemModel(BiConsumer<ResourceLocation, LyraItemModelProvider> consumer) {
        if (!FMLLoader.isProduction()) {
            LyraItemModelProvider.ItemModelGenerate.put(register.getId(), consumer);
        }
        return this;
    }

    public static void basicModel(ResourceLocation location, LyraItemModelProvider provider) {
        provider.basicItem(location);
    }

    public static void handheldItem(ResourceLocation location, LyraItemModelProvider provider) {
        provider.handheldItem(location);
    }

    public DeferredItem<T> build() {
        return register;
    }
}
