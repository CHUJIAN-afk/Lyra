package first.lyra.register;

import first.lyra.common.entity.AttachmentEntity;
import first.lyra.common.entity.AttachmentEntityType;
import first.lyra.dataGenerator.provider.LyraItemModelProvider;
import first.lyra.dataGenerator.provider.LyraItemTagsProvider;
import first.lyra.dataGenerator.provider.LyraRecipeProvider;
import first.lyra.register.LyraLanguageRegister;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Lyra 物品注册构建器（外接版）。
 * <p>
 * 库本身不注册任何物品：构建器使用宿主 mod 传入的 {@link DeferredRegister.Items} 注册物品，
 * 并链式配置：
 * <ul>
 *     <li>语言条目（写入 {@link LyraLanguageRegister}，由宿主 mod 语言数据生成器输出）</li>
 *     <li>配方/标签/模型数据生成</li>
 * </ul>
 * 创造模式分类不在此指定：物品只需带有 {@link Section} 定义的特征标签
 * （通过物品 Properties.tag 或数据生成标签），即自动归入对应分段。
 * 用法：
 * <pre>{@code
 * DeferredRegister.Items items = DeferredRegister.createItems("my_mod");
 * LyraItemRegisterBuilder.build(items, "my_item", () -> new MyItem(new Item.Properties().tag(MyTags.SECTION_A)))
 *         .itemLanguage("My Item", "我的物品")
 *         .recipe(output -> ShapedRecipeBuilder.shaped(...).save(output))
 *         .itemModel(LyraItemRegisterBuilder::basicModel)
 *         .build();
 * }</pre>
 * 数据生成条目（语言/配方/标签/模型）写入 Lyra 的 datagen 收集器，
 * 由 {@code LyraDataGeneratorEvent} 在宿主跑 runData 时输出。
 */
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

    public LyraItemRegisterBuilder<T> language(String key, String enDesc, String zhDesc) {
        if (!FMLLoader.isProduction()) {
            LyraLanguageRegister.entry(key, enDesc, zhDesc);
        }
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

    public <A extends AttachmentEntity> LyraItemRegisterBuilder<T> servantLanguage(DeferredHolder<AttachmentEntityType<?>, AttachmentEntityType<A>> holder, String en, String zh) {
        ResourceLocation servantId = holder.getId();
        return language("servant." + servantId.getNamespace() + "." + servantId.getPath(), en, zh);
    }

    /** 注册配方（runData 时输出）。 */
    public LyraItemRegisterBuilder<T> recipe(Consumer<RecipeOutput> outputConsumer) {
        if (!FMLLoader.isProduction()) {
            LyraRecipeProvider.RecipeGenerate.add(outputConsumer);
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
