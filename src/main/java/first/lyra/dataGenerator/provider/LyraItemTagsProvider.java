package first.lyra.dataGenerator.provider;

import first.lyra.Lyra;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 物品标签数据生成：输出宿主 mod 通过 {@code LyraItemRegisterBuilder.itemTag()} 收集的标签，
 * 并自动将 ArmorItem 细分到对应的原版护甲分类标签。
 */
public class LyraItemTagsProvider extends ItemTagsProvider {

    public static final Map<TagKey<Item>, List<ItemLike>> ItemTagsGenerate = new HashMap<>();

    public LyraItemTagsProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFileHelper) {
        this(packOutput, lookupProvider, blockTags, existingFileHelper, Lyra.MODID);
    }

    /** 宿主 mod 使用:输出到宿主命名空间(标签 JSON 与物品 ID 命名空间需一致)。 */
    public LyraItemTagsProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFileHelper, String modid) {
        super(packOutput, lookupProvider, blockTags, modid, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        ItemTagsGenerate.entrySet()
                .removeIf(entry -> {
                    TagKey<Item> tag = entry.getKey();
                    IntrinsicTagAppender<Item> appender = tag(tag);
                    List<ItemLike> list = entry.getValue();
                    list.forEach(itemLike -> {
                        Item item = itemLike.asItem();
                        appender.add(item);
                        if (item instanceof ArmorItem armorItem){
                            if (tag == Tags.Items.ARMORS){
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
                    });
                    return true;
                });
    }
}
