package first.lyra.register;

import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.AttachmentEntityType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import org.mesdag.portlib.registries.PortDeferredItem;
import org.mesdag.portlib.registries.PortRegistryEntry;
import org.mesdag.portlib.wrapper.PortEnvironment;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class LyraItemRegisterBuilder<T extends Item> {

    private final LyraItemRegistries lyraItemRegistries;
    private final PortDeferredItem<T> register;

    public LyraItemRegisterBuilder(LyraItemRegistries lyraItemRegistries, PortDeferredItem<T> register) {
        this.lyraItemRegistries = lyraItemRegistries;
        this.register = register;
    }

    public LyraItemRegisterBuilder<T> lootTable(ResourceLocation location, Function<LootTable, LootPool> function) {
        lyraItemRegistries.lootTableData.computeIfAbsent(location, key -> new ArrayList<>()).add(function);
        return this;
    }

    public LyraItemRegisterBuilder<T> jeiInfo(int index, String enDesc, String zhDesc) {
        ResourceLocation id = register.getId();
        String key = "item." + id.getNamespace() + "." + id.getPath() + "jei.description." + index;
        if (PortEnvironment.isModLoaded("jei") && PortEnvironment.isPhysicalClient()) {
            lyraItemRegistries.jeiInfoData.computeIfAbsent(register, like -> new ArrayList<>()).add(Component.translatable(key));
        }
        lyraItemRegistries.language(key, enDesc, zhDesc);
        return this;
    }

    public LyraItemRegisterBuilder<T> itemLanguage(String en, String zh) {
        lyraItemRegistries.language("item." + register.getId().toLanguageKey(), en, zh);
        return this;
    }

    public LyraItemRegisterBuilder<T> itemLanguageTooltip(int index, String en, String zh) {
        lyraItemRegistries.language("item." + register.getId().toLanguageKey() + ".lyra_tooltip." + index, en, zh);
        return this;
    }

    public <A extends AttachmentEntity> LyraItemRegisterBuilder<T> summonLanguage(PortRegistryEntry<AttachmentEntityType<?>, AttachmentEntityType<A>> holder, String en, String zh) {
        lyraItemRegistries.language("summon." + holder.getId().toLanguageKey(), en, zh);
        return this;
    }

    public LyraItemRegisterBuilder<T> blockLanguage(String en, String zh) {
        lyraItemRegistries.language("block." + register.getId().toLanguageKey(), en, zh);
        return this;
    }

    public LyraItemRegisterBuilder<T> recipeWithLookup(BiConsumer<HolderLookup.Provider, Consumer<FinishedRecipe>> outputConsumer) {
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

    public PortDeferredItem<T> build() {
        return register;
    }
}
