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

    /**
     * 登记宿主 mod 的 Tab，使其使用 Lyra 的分组横幅渲染与物品展示逻辑。
     */
    public static void register(Holder<CreativeModeTab> tab, Section... sections) {
        List<Section> list = ManagedTabs.computeIfAbsent(tab.value(), key -> new ArrayList<>());
        list.addAll(List.of(sections));
        Collections.sort(list);
    }

    /**
     * 判断 Tab 是否启用 Lyra 的分组逻辑（仅已登记的外部 Tab）。
     */
    public static boolean isManaged(CreativeModeTab tab) {
        return ManagedTabs.containsKey(tab);
    }

    /**
     * 按 order 排序的全部分段。
     */
    public static List<Section> getSections(CreativeModeTab tab) {
        return ManagedTabs.getOrDefault(tab, new ArrayList<>());
    }

    /**
     * 归入该分段的全部物品（带特征标签的已注册物品 + 动态追加物品）。
     */
    public static List<ItemStack> itemsOf(HolderLookup.Provider provider, Section section) {
        return SectionTabs.computeIfAbsent(section, key -> {
            List<ItemStack> list = new ArrayList<>();
            list.addAll(BuiltInRegistries.ITEM.stream().map(Item::getDefaultInstance).filter(itemStack -> itemStack.is(section.tag())).toList());
            list.addAll(section.function().apply(provider));
            return list;
        });
    }

    /**
     * 归入该分段的全部物品（仅带特征标签的已注册物品）。
     */
    public static List<ItemStack> itemsOf(Section section) {
        return itemsOf(null, section);
    }
}
