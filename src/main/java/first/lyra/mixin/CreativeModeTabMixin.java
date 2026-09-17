package first.lyra.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import first.lyra.common.creativeTab.CreativeTabDispatcher;
import first.lyra.common.creativeTab.Section;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.*;

@Mixin(CreativeModeTab.class)
public class CreativeModeTabMixin {

    @Shadow
    private Collection<ItemStack> displayItems;

    @Shadow
    private Set<ItemStack> displayItemsSearchTab;

    @WrapMethod(method = "buildContents")
    private void simulated$buildContents(CreativeModeTab.ItemDisplayParameters parameters, Operation<Void> original) {
        CreativeModeTab tab = (CreativeModeTab) (Object) this;
        HolderLookup.Provider holders = parameters.holders();
        if (CreativeTabDispatcher.isManaged(tab)) {
            List<ItemStack> displayItems = new LinkedList<>();
            Set<ItemStack> searchItems = new LinkedHashSet<>();
            List<Section> sections = CreativeTabDispatcher.getSections(tab);
            for (Section section : sections) {
                List<ItemStack> stacks = new ArrayList<>(CreativeTabDispatcher.itemsOf(holders, section));
                for (int i = 0; i < 9; i++) {
                    stacks.add(0, ItemStack.EMPTY);
                }
                while (stacks.size() % 9 != 0) {
                    stacks.add(ItemStack.EMPTY);
                }
                for (ItemStack stack : stacks) {
                    displayItems.add(stack);
                    if (!stack.isEmpty()) {
                        searchItems.add(stack);
                    }
                }
            }
            this.displayItems = displayItems;
            this.displayItemsSearchTab = searchItems;
        } else {
            original.call(parameters);
        }
    }
}
