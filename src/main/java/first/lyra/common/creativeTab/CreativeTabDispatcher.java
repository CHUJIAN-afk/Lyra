package first.lyra.common.creativeTab;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class CreativeTabDispatcher {

    private static final Map<CreativeModeTab, List<Section>> ManagedTabs = new HashMap<>();
    private static final Map<Section, List<ItemStack>> SectionTabs = new HashMap<>();

    private CreativeTabDispatcher() {
    }

    public static void register(Holder<CreativeModeTab> tab, Section... sections) {
        List<Section> list = ManagedTabs.computeIfAbsent(tab.value(), key -> new ArrayList<>());
        list.addAll(List.of(sections));
        Collections.sort(list);
    }

    public static boolean isManaged(CreativeModeTab tab) {
        return ManagedTabs.containsKey(tab);
    }

    public static List<Section> getSections(CreativeModeTab tab) {
        return ManagedTabs.getOrDefault(tab, new ArrayList<>());
    }

    public static List<ItemStack> itemsOf(HolderLookup.Provider provider, Section section) {
        return SectionTabs.computeIfAbsent(section, key -> {
            List<ItemStack> list = new ArrayList<>();
            list.addAll(BuiltInRegistries.ITEM.stream().map(Item::getDefaultInstance).filter(itemStack -> itemStack.is(section.tag())).toList());
            list.addAll(section.function().apply(provider));
            return list;
        });
    }

    public static List<ItemStack> itemsOf(Section section) {
        return itemsOf(null, section);
    }
}
