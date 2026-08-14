package first.lyra.mixin;

import first.lyra.client.creativeTab.AnimBanner;
import first.lyra.common.creativeTab.CreativeTabDispatcher;
import first.lyra.common.creativeTab.Section;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {

    @Shadow
    private static CreativeModeTab selectedTab;

    @Shadow
    private float scrollOffs;

    @Inject(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/EffectsInInventory;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
                    shift = At.Shift.AFTER
            )
    )
    private void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (CreativeTabDispatcher.isManaged(selectedTab)) {
            CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
            List<Section> sections = CreativeTabDispatcher.getSections(selectedTab);

            int totalRows = 0;
            for (Section section : sections) {
                totalRows += 1;
                totalRows += (CreativeTabDispatcher.itemsOf(section).size() + 8) / 9;
            }

            int scrollRow = Math.round(scrollOffs * Math.max(0, totalRows - 5));
            int left = screen.getLeftPos() + 8;
            int top = screen.getTopPos() + 17;

            int currentRow = 0;
            for (Section section : sections) {
                int bannerRow = currentRow;
                int itemRows = (CreativeTabDispatcher.itemsOf(section).size() + 8) / 9;
                currentRow += 1 + itemRows;
                Identifier texture = section.texture();
                AnimBanner animBanner = section.animBanner();
                int visibleRow = bannerRow - scrollRow;
                if (visibleRow < 0 || visibleRow >= 5) continue;
                int bannerY = top + visibleRow * 18;
                AnimBanner.blitAnimated(graphics, texture, animBanner, left, bannerY, 162, mouseX, mouseY, true);
            }
        }
    }
}
