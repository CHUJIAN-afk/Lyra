package first.lyra.dataGenerator.provider;

import first.lyra.Lyra;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 物品标签数据生成：输出宿主 mod 通过 {@code LyraItemRegisterBuilder.itemTag()} 收集的标签，
 * 并自动将可装备物品细分到对应的原版护甲分类标签。
 * <p>
 * 26.2: 改用 NeoForge ItemTagsProvider(移除 blockTags 依赖与 ExistingFileHelper);
 * IntrinsicTagAppender 删除 → TagAppender.add(ResourceKey);
 * ArmorItem 类删除 → DataComponents.EQUIPPABLE 组件判断。
 * </p>
 */
public class LyraItemTagsProvider extends ItemTagsProvider {

    public static final Map<TagKey<Item>, List<ItemLike>> ItemTagsGenerate = new HashMap<>();

    public LyraItemTagsProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider, Lyra.MODID);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        ItemTagsGenerate.entrySet()
                .removeIf(entry -> {
                    TagKey<Item> tag = entry.getKey();
                    TagAppender<Item> appender = tag(tag);
                    List<ItemLike> list = entry.getValue();
                    list.forEach(itemLike -> {
                        Item item = itemLike.asItem();
                        appender.add(item.builtInRegistryHolder().key());
                        Equippable equippable = item.getDefaultInstance().get(DataComponents.EQUIPPABLE);
                        if (equippable != null) {
                            EquipmentSlot equipmentSlot = equippable.slot();
                            if (tag == Tags.Items.ARMORS) {
                                switch (equipmentSlot) {
                                    case HEAD -> tag(ItemTags.HEAD_ARMOR).add(item.builtInRegistryHolder().key());
                                    case CHEST -> tag(ItemTags.CHEST_ARMOR).add(item.builtInRegistryHolder().key());
                                    case LEGS -> tag(ItemTags.LEG_ARMOR).add(item.builtInRegistryHolder().key());
                                    case FEET -> tag(ItemTags.FOOT_ARMOR).add(item.builtInRegistryHolder().key());
                                }
                            }
                            if (tag == ItemTags.ARMOR_ENCHANTABLE) {
                                switch (equipmentSlot) {
                                    case HEAD -> tag(ItemTags.HEAD_ARMOR_ENCHANTABLE).add(item.builtInRegistryHolder().key());
                                    case CHEST -> tag(ItemTags.CHEST_ARMOR_ENCHANTABLE).add(item.builtInRegistryHolder().key());
                                    case LEGS -> tag(ItemTags.LEG_ARMOR_ENCHANTABLE).add(item.builtInRegistryHolder().key());
                                    case FEET -> tag(ItemTags.FOOT_ARMOR_ENCHANTABLE).add(item.builtInRegistryHolder().key());
                                }
                            }
                        }
                    });
                    return true;
                });
    }
}
