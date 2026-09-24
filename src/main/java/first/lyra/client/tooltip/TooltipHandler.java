package first.lyra.client.tooltip;

import first.lyra.common.item.SummonerWeaponItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;

public final class TooltipHandler {

    public static void handler(ItemTooltipEvent event) {
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();
        List<Component> toolTip = event.getToolTip();
        toolTip.addAll(getMinionWeaponItemTooltip(itemStack, player));
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

    private static List<Component> getMinionWeaponItemTooltip(ItemStack itemStack, Player player) {
        List<Component> lines = new ArrayList<>();
        if (itemStack.getItem() instanceof SummonerWeaponItem<?> summonerWeaponItem && player != null) {
            lines.addAll(summonerWeaponItem.getTooltips(itemStack, player));
        }
        return lines;
    }
}
