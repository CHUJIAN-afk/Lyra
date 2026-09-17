package first.lyra.client.tooltip;

import first.lyra.common.armorSet.ArmorSet;
import first.lyra.common.item.SummonerWeaponItem;
import first.lyra.register.LyraRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

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
        ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(item);
        List<MutableComponent> lore = new ArrayList<>();
        String baseKey = "item" + "." + registryName.getNamespace() + "." + registryName.getPath() + "." + "tooltip" + ".";
        int index = 1;
        while (I18n.exists(baseKey + index)) {
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
            Iterable<ItemStack> armorSlots = player.getArmorSlots();
            for (ItemStack armorSlot : armorSlots) {
                armors.add(armorSlot.getItem());
            }
        }
        List<ArmorSet> list = LyraRegistries.armorSets().stream().toList();
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
            ResourceLocation id = armorSet.id();
            lines.add(Component.empty());
            List<ItemLike> items = armorSet.items();
            MutableComponent set = Component.empty();
            boolean full = player != null && armorSet.full(player);
            ChatFormatting descColor = full ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY;
            for (ItemLike itemDeferredItem : items) {
                if (items.get(0) == itemDeferredItem) {
                    set.append(Component.literal("[ ").withStyle(descColor));
                }
                Item piece = itemDeferredItem.asItem();
                ChatFormatting format = armors.contains(piece) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY;
                set.append(piece.getDescription().copy().withStyle(format)).append(Component.literal(" "));
                if (items.get(items.size() - 1) == itemDeferredItem) {
                    set.append(Component.literal("] ").withStyle(descColor));
                }
            }
            set.append(Component.translatable("item.lyra.tooltip.set_bonus_title").withStyle(descColor));
            lines.add(set);
            Collection<Map.Entry<Holder<Attribute>, AttributeModifier>> entries = armorSet.modifiers().entries();
            descColor = full ? ChatFormatting.BLUE : ChatFormatting.DARK_GRAY;
            for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : entries) {
                Attribute attr = entry.getKey().value();
                AttributeModifier modifier = entry.getValue();
                String operationKey = "attribute.modifier." + (modifier.getAmount() >= 0 ? "plus." : "take.") + modifier.getOperation().toValue();
                lines.add(Component.translatable(
                        operationKey,
                        String.format("%.2f", Math.abs(modifier.getAmount())),
                        Component.translatable(attr.getDescriptionId())
                ).withStyle(descColor));
            }
            String baseKey = id.toLanguageKey() + ".set.";
            int index = 1;
            while (I18n.exists(baseKey + index)) {
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
