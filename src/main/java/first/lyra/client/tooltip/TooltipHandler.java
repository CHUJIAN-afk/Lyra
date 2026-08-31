package first.lyra.client.tooltip;

import first.lyra.Lyra;
import first.lyra.common.armorSet.ArmorSet;
import first.lyra.common.item.SummonerWeaponItem;
import first.lyra.register.LyraRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class TooltipHandler {

    public static void handler(ItemTooltipEvent event) {
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();
        List<Component> toolTip = event.getToolTip();
        toolTip.addAll(getMinionWeaponItemTooltip(itemStack, player));
        toolTip.addAll(getArmorSetTooltip(itemStack, player));
        toolTip.addAll(getCustomTooltip(itemStack, player));
    }

    private static List<Component> getCustomTooltip(ItemStack itemStack, Player player) {
        List<Component> lines = new ArrayList<>();
        Item item = itemStack.getItem();
        Identifier registryName = BuiltInRegistries.ITEM.getKey(item);
        List<MutableComponent> lore = new ArrayList<>();
        String baseKey = "item" + "." + registryName.getNamespace() + "." + registryName.getPath() + "." + "tooltip" + ".";
        int index = 1;
        while (Language.getInstance().has(baseKey + index)) {
            lore.add(Component.translatable(baseKey + index));
            index++;
        }
        if (!lore.isEmpty()) {
            if (player != null) {
                lines.add(Component.empty());
            }
            for (MutableComponent component : lore) {
                lines.add(component.withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return lines;
    }

    public static List<Component> getArmorSetTooltip(ItemStack itemStack, Player player) {
        List<Component> lines = new ArrayList<>();
        Item item = itemStack.getItem();
        List<Item> armors = new ArrayList<>();
        if (player != null) {
            // 26.2: getArmorSlots() 已移除,遍历护甲槽(isArmor 判断 HUMAN+ANIMAL 装甲)
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.isArmor()) {
                    armors.add(player.getItemBySlot(slot).getItem());
                }
            }
        }
        List<ArmorSet> list = LyraRegistries.ARMOR_SETS.stream().toList();
        List<ArmorSet> target = new ArrayList<>();
        for (ArmorSet armorSet1 : list) {
            List<ItemLike> items1 = armorSet1.items();
            for (ItemLike itemDeferredItem1 : items1) {
                if (item == itemDeferredItem1.asItem()) {
                    target.add(armorSet1);
                    break;
                }
            }
        }
        for (ArmorSet armorSet : target) {
            Identifier id = armorSet.id();
            lines.add(Component.empty());
            List<ItemLike> items = armorSet.items();
            MutableComponent set = Component.empty();
            boolean full = player != null && armorSet.full(player);
            ChatFormatting descColor = full ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY;
            for (ItemLike itemDeferredItem : items) {
                if (items.getFirst() == itemDeferredItem) {
                    set.append(Component.literal("[ ").withStyle(descColor));
                }
                Item piece = itemDeferredItem.asItem();
                ChatFormatting format = armors.contains(piece) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY;
                set.append(Component.translatable(piece.getDescriptionId()).copy().withStyle(format)).append(Component.literal(" "));
                if (items.getLast() == itemDeferredItem) {
                    set.append(Component.literal("] ").withStyle(descColor));
                }
            }
            set.append(Component.translatable("item.lyra.tooltip.set_bonus_title").withStyle(descColor));
            lines.add(set);
            Collection<Map.Entry<Holder<Attribute>, AttributeModifier>> entries = armorSet.modifiers().entries();
            descColor = full ? ChatFormatting.BLUE : ChatFormatting.DARK_GRAY;
            for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : entries) {
                // 26.2: Attribute.toComponent 移除,改用 ItemAttributeModifiers.Display 的 vanilla 格式化
                List<Component> attrLines = new ArrayList<>();
                ItemAttributeModifiers.Display.attributeModifiers().apply(attrLines::add, player, entry.getKey(), entry.getValue());
                for (Component attrLine : attrLines) {
                    lines.add(attrLine.copy().withStyle(descColor));
                }
            }
            String baseKey = Lyra.MODID + "." + id.getNamespace() + "." + id.getPath() + "." + "set" + ".";
            int index = 1;
            while (Language.getInstance().has(baseKey + index)) {
                lines.add(Component.translatable(baseKey + index).withStyle(descColor));
                index++;
            }
        }
        return lines;
    }

    private static List<Component> getMinionWeaponItemTooltip(ItemStack itemStack, Player player) {
        List<Component> lines = new ArrayList<>();
        if (itemStack.getItem() instanceof SummonerWeaponItem<?> summonerWeaponItem && player != null) {
            lines.addAll(summonerWeaponItem.getTooltips(itemStack, player));
        }
        return lines;
    }
}
