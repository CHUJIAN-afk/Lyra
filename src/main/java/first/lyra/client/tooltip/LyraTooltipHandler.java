package first.lyra.client.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LyraTooltipHandler {

    private static final List<TooltipProvider> providers = new ArrayList<>();

    public static void register(TooltipProvider tooltipProvider) {
        providers.add(tooltipProvider);
        Collections.sort(providers);
    }

    public static void handler(ItemTooltipEvent event) {
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();
        List<Component> toolTip = event.getToolTip();
        for (TooltipProvider provider : providers) {
            List<Component> tooltip = provider.function().apply(itemStack, player);
            if (!tooltip.isEmpty()) {
                toolTip.addAll(tooltip);
            }
        }
    }
}
