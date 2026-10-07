package first.lyra.common.creativeTab;

import first.lyra.client.creativeTab.AnimBanner;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record Section(int order, ResourceLocation texture, AnimBanner animBanner, TagKey<Item> tag, Function<HolderLookup.Provider, List<ItemStack>> function) implements Comparable<Section> {

    public Section(int order, ResourceLocation texture, AnimBanner animBanner, TagKey<Item> tag) {
        this(order, texture, animBanner, tag, provider -> new ArrayList<>());
    }

    @Override
    public int compareTo(@NonNull Section o) {
        return Integer.compare(order, o.order);
    }
}
