package first.lyra.common.creativeTab;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;


public final class CreativeTabDispatcher {

    /** 已接入 Lyra 分组逻辑的创造模式 Tab。 */
    private static final Set<CreativeModeTab> ManagedTabs = new HashSet<>();

    /** 已登记的分类分段。 */
    private static final List<Section> Sections = new ArrayList<>();

    private CreativeTabDispatcher() {
    }

    /** 登记宿主 mod 的 Tab，使其使用 Lyra 的分组横幅渲染与物品展示逻辑。 */
    public static void registerTab(Holder<CreativeModeTab> tab) {
        ManagedTabs.add(tab.value());
    }

    /** 判断 Tab 是否启用 Lyra 的分组逻辑（仅已登记的外部 Tab）。 */
    public static boolean isManaged(CreativeModeTab tab) {
        return ManagedTabs.contains(tab);
    }

    /** 登记一个分类分段（特征标签 + 横幅 + 顺序）。 */
    public static void registerSection(Section section) {
        Sections.add(section);
    }

    /** 按 order 排序的全部分段。 */
    public static List<Section> sortedSections() {
        List<Section> sorted = new ArrayList<>(Sections);
        sorted.sort(Comparator.comparingInt(Section::order));
        return sorted;
    }

    /** 归入该分段的全部物品（带特征标签的已注册物品）。 */
    public static List<ItemStack> itemsOf(Section section) {
        return BuiltInRegistries.ITEM.stream()
                .filter(item -> item.builtInRegistryHolder().is(section.tag()))
                .map(Item::getDefaultInstance)
                .toList();
    }
}
