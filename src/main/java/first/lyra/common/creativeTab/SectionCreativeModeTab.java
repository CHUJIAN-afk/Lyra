package first.lyra.common.creativeTab;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class SectionCreativeModeTab extends CreativeModeTab {

    private final Map<Section, List<ItemStack>> items = new TreeMap<>();

    public SectionCreativeModeTab(Builder builder, Section... sections) {
        super(builder);
        for (Section section : sections) {
            items.put(section, null);
        }
    }

    public SectionCreativeModeTab putSection(Section section) {
        items.put(section, null);
        return this;
    }

    public Set<Section> getSections() {
        return items.keySet();
    }

    public List<ItemStack> getItemStacks(HolderLookup.Provider provider, Section section) {
        if (items.containsKey(section)) {
            if (items.get(section) == null) {
                if (provider != null) {
                    List<ItemStack> list = new ArrayList<>();
                    section.consumer().accept(provider, list);
                    items.put(section, list);
                }
            }
            List<ItemStack> list = items.get(section);
            if (list != null) {
                return list;
            }
        }
        return List.of();
    }
}
