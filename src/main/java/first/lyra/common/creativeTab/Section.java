package first.lyra.common.creativeTab;

import first.lyra.client.creativeTab.AnimBanner;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 创造模式物品栏分类分段。
 * <p>
 * 物品由 {@code function} 手动填入，首次填入时执行一次并缓存结果；
 * {@code order} 决定分段展示顺序，texture/animBanner 驱动横幅渲染。
 * </p>
 */
public record Section(int order, ResourceLocation texture, AnimBanner animBanner, BiConsumer<HolderLookup.Provider, List<ItemStack>> consumer) implements Comparable<Section> {

    public Section(int order, ResourceLocation texture, AnimBanner animBanner, List<ItemStack> items) {
        this(order, texture, animBanner, (provider, itemStacks) -> {
        });
    }

    @Override
    public int compareTo(@NonNull Section o) {
        return Integer.compare(order, o.order);
    }
}
