package first.lyra.register;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
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
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class LyraItemRegistries {

    public static final Map<String, LyraItemRegistries> REGISTRIES = new ConcurrentHashMap<>();

    public final Map<ItemLike, List<Component>> jeiInfoData = new HashMap<>();
    public final Map<ResourceLocation, List<Function<LootTable, LootPool>>> lootTableData = new HashMap<>();

    public final Map<String, String[]> languageGenerate = new HashMap<>();
    public final List<BiConsumer<HolderLookup.Provider, Consumer<FinishedRecipe>>> recipesGenerate = new ArrayList<>();
    public final Map<TagKey<Item>, List<ItemLike>> itemTagsGenerate = new HashMap<>();
    public final Map<ResourceLocation, BiConsumer<ItemModelProvider, ResourceLocation>> itemModelGenerate = new HashMap<>();
    private final String modid;
    private final DeferredRegister<Item> register;
    private final boolean development;
    private LanguageInit languageInit = null;

    @FunctionalInterface
    public interface LanguageInit {
        void init(LyraItemRegistries registries);
    }

    private LyraItemRegistries(String modid) {
        this.modid = modid;
        this.register = DeferredRegister.create(ForgeRegistries.ITEMS, modid);
        this.development = !FMLLoader.isProduction();
        REGISTRIES.put(modid, this);
    }

    public static LyraItemRegistries create(String modid) {
        return new LyraItemRegistries(modid);
    }

    public <T extends Item> LyraItemRegisterBuilder<T> build(String name, Function<ResourceLocation, T> function) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(modid, name);
        return new LyraItemRegisterBuilder<>(this, register.register(name, () -> function.apply(id)));
    }

    public <T extends Item> LyraItemRegisterBuilder<T> build(String name, Supplier<T> supplier) {
        return build(name, location -> supplier.get());
    }

    public DeferredRegister<Item> getRegister() {
        return register;
    }

    public boolean isDevelopment() {
        return development;
    }

    public void language(String key, String en, String zh) {
        if (isDevelopment()) {
            languageGenerate.put(key, new String[]{en, zh});
        }
    }

    public LyraItemRegistries languageInit(LanguageInit languageInit) {
        this.languageInit = languageInit;
        return this;
    }

    public void register(IEventBus eventBus, @Nullable Runnable runnable) {
        if (runnable != null) {
            runnable.run();
        }
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
            languageInit.init(this);
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
            generator.addProvider(server, new RecipeProvider(packOutput) {
                @Override
                protected void buildRecipes(@NotNull Consumer<FinishedRecipe> recipeOutput) {
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
                        TagsProvider.TagAppender<Item> appender = tag(tag);
                        list.forEach(itemLike -> {
                            Item item = itemLike.asItem();
                            if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(modId)) {
                                appender.add(item.builtInRegistryHolder().key());
                                if (item instanceof ArmorItem armorItem) {
                                    if (tag == Tags.Items.ARMORS) {
                                        EquipmentSlot equipmentSlot = armorItem.getEquipmentSlot();
                                        // 1.20.1 has one armor tag instead of the later per-slot split.
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
