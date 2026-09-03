package first.lyra.common.creativeTab;

import first.lyra.client.creativeTab.AnimBanner;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * 创造模式物品栏分类分段。
 * <p>
 * {@link #tag()} 是该分段的特征标签：任何带此标签的物品自动归入本分类，
 * 无需在物品注册时指定。{@code order} 决定分段展示顺序，texture/animBanner 驱动横幅渲染。
 * {@code function} 可在构建时动态追加物品（需要 HolderLookup 时使用）。
 * </p>
 */
public record Section(int order, ResourceLocation texture, AnimBanner animBanner, TagKey<Item> tag, Function<HolderLookup.Provider, List<ItemStack>> function) implements Comparable<Section> {

    public Section(int order, ResourceLocation texture, AnimBanner animBanner, TagKey<Item> tag) {
        this(order, texture, animBanner, tag, provider -> new ArrayList<>());
    }

    @Override
    public int compareTo(@NonNull Section o) {
        return Integer.compare(order, o.order);
    }
}
