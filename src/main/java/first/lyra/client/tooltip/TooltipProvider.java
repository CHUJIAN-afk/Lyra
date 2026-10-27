package first.lyra.client.tooltip;


import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.core.net.Priority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

public record TooltipProvider(int priority, BiFunction<@NotNull ItemStack, @Nullable Player, @NotNull List<Component>> function) implements Comparable<TooltipProvider> {

    public TooltipProvider(BiFunction<@NotNull ItemStack, @Nullable Player, @NotNull List<Component>> function) {
        this(0, function);
    }

    @Override
    public int compareTo(@NotNull TooltipProvider tooltipProvider) {
        return Integer.compare(priority, tooltipProvider.priority);
    }
}
