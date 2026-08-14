package first.lyra.common.creativeTab;

import first.lyra.client.creativeTab.AnimBanner;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * 创造模式物品栏分类分段。
 * <p>
 * {@link #tag()} 是该分段的特征标签：任何带此标签的物品自动归入本分类，
 * 无需在物品注册时指定。{@code order} 决定分段展示顺序，texture/animBanner 驱动横幅渲染。
 * </p>
 */
public record Section(int order, ResourceLocation texture, AnimBanner animBanner, TagKey<Item> tag) implements Comparable<Section> {
    @Override
    public int compareTo(@NonNull Section o) {
        return Integer.compare(order, o.order);
    }
}
