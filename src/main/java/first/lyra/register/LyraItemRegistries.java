package first.lyra.register;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class LyraItemRegistries {

    public static final List<LyraItemRegistries> REGISTRIES = new CopyOnWriteArrayList<>();

    public final Map<ItemLike, List<Component>> jeiInfoData = new HashMap<>();
    public final Map<ResourceLocation, List<Function<LootTable, LootPool>>> lootTableData = new HashMap<>();

    public final Map<String, String[]> languageGenerate = new HashMap<>();
    public final List<BiConsumer<HolderLookup.Provider, RecipeOutput>> recipesGenerate = new ArrayList<>();
    public final Map<TagKey<Item>, List<ItemLike>> itemTagsGenerate = new HashMap<>();
    public final Map<ResourceLocation, BiConsumer<ItemModelProvider, ResourceLocation>> itemModelGenerate = new HashMap<>();
    private final String modid;
    private final DeferredRegister.Items register;
    private final boolean development;

    public LyraItemRegistries(String modid) {
        this.modid = modid;
        this.register = DeferredRegister.createItems(modid);
        this.development = !FMLLoader.isProduction();
        REGISTRIES.add(this);
    }

    public <T extends Item> LyraItemRegisterBuilder<T> build(String name, Function<ResourceLocation, T> function) {
        return new LyraItemRegisterBuilder<>(this, register.register(name, function));
    }

    public <T extends Item> LyraItemRegisterBuilder<T> build(String name, Supplier<T> supplier) {
        return build(name, location -> supplier.get());
    }

    public LyraItemRegisterBuilder<Item> build(DeferredRegister.Items register, String name) {
        return build(name, () -> new Item(new Item.Properties()));
    }

    public DeferredRegister.Items getRegister() {
        return register;
    }

    public boolean isDevelopment() {
        return development;
    }

    public void register(IEventBus eventBus) {
        register.register(eventBus);
        eventBus.addListener((GatherDataEvent event) -> {
            boolean client = event.includeClient();
            boolean server = event.includeServer();
            DataGenerator generator = event.getGenerator();
            PackOutput packOutput = generator.getPackOutput();
            ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
            CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
            Function<String, LanguageProvider> function = locale -> new LanguageProvider(packOutput, modid, locale) {
                @Override
                protected void addTranslations() {
                    languageGenerate.forEach((key, value) -> {
                        if (key != null) {
                            String enDesc = value[0];
                            String zhDesc = value[1];
                            if (enDesc != null && "en_us".equals(locale)) {
                                add(key, enDesc);
                            }
                            if (zhDesc != null && "zh_cn".equals(locale)) {
                                add(key, zhDesc);
                            }
                        }
                    });
                }
            };
            generator.addProvider(client, function.apply("en_us"));
            generator.addProvider(client, function.apply("zh_cn"));
            generator.addProvider(client, new ItemModelProvider(packOutput, modid, existingFileHelper) {
                @Override
                protected void registerModels() {
                    itemModelGenerate.forEach((key, consumer) -> {
                        if (modid.equals(key.getNamespace())) {
                            consumer.accept(this, key);
                        }
                    });
                }
            });
            generator.addProvider(server, new RecipeProvider(packOutput, lookupProvider) {
                @Override
                protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
                    recipesGenerate.removeIf(generate -> {
                        generate.accept(lookupProvider.join(), recipeOutput);
                        return true;
                    });
                }
            });
            BlockTagsProvider blockTagsProvider = new BlockTagsProvider(packOutput, lookupProvider, modid, existingFileHelper) {
                @Override
                protected void addTags(HolderLookup.@NotNull Provider provider) {
                }
            };
            generator.addProvider(server, blockTagsProvider);
            generator.addProvider(server, new ItemTagsProvider(packOutput, lookupProvider, blockTagsProvider.contentsGetter(), modid, existingFileHelper) {
                @Override
                protected void addTags(HolderLookup.@NotNull Provider provider) {
                    itemTagsGenerate.forEach((tag, list) -> {
                        IntrinsicTagAppender<Item> appender = tag(tag);
                        list.forEach(itemLike -> {
                            Item item = itemLike.asItem();
                            if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(modId)) {
                                appender.add(item);
                                if (item instanceof ArmorItem armorItem) {
                                    if (tag == Tags.Items.ARMORS) {
                                        EquipmentSlot equipmentSlot = armorItem.getEquipmentSlot();
                                        switch (equipmentSlot) {
                                            case HEAD -> tag(ItemTags.HEAD_ARMOR).add(armorItem);
                                            case CHEST -> tag(ItemTags.CHEST_ARMOR).add(armorItem);
                                            case LEGS -> tag(ItemTags.LEG_ARMOR).add(armorItem);
                                            case FEET -> tag(ItemTags.FOOT_ARMOR).add(armorItem);
                                        }
                                    }
                                    if (tag == ItemTags.ARMOR_ENCHANTABLE) {
                                        EquipmentSlot equipmentSlot = armorItem.getEquipmentSlot();
                                        switch (equipmentSlot) {
                                            case HEAD -> tag(ItemTags.HEAD_ARMOR_ENCHANTABLE).add(armorItem);
                                            case CHEST -> tag(ItemTags.CHEST_ARMOR_ENCHANTABLE).add(armorItem);
                                            case LEGS -> tag(ItemTags.LEG_ARMOR_ENCHANTABLE).add(armorItem);
                                            case FEET -> tag(ItemTags.FOOT_ARMOR_ENCHANTABLE).add(armorItem);
                                        }
                                    }
                                }
                            }
                        });
                    });
                }
            });
        });
    }
}
