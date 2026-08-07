package first.lyra.mixin;

import first.lyra.client.creativeTab.AnimBanner;
import first.lyra.common.creativeTab.CreativeTabDispatcher;
import first.lyra.common.creativeTab.Section;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.ResourceLocation;
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
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/EffectRenderingInventoryScreen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void simulated$render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick, final CallbackInfo ci) {
        if (CreativeTabDispatcher.isManaged(selectedTab)) {
            CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
            List<Section> sections = CreativeTabDispatcher.sortedSections();

            int totalRows = 0;
            for (Section section : sections) {
                totalRows += 1;
                totalRows += (CreativeTabDispatcher.itemsOf(section).size() + 8) / 9;
            }

            int scrollRow = Math.round(scrollOffs * Math.max(0, totalRows - 5));
            int left = screen.getGuiLeft() + 8;
            int top = screen.getGuiTop() + 17;

            int currentRow = 0;
            for (Section section : sections) {
                int bannerRow = currentRow;
                int itemRows = (CreativeTabDispatcher.itemsOf(section).size() + 8) / 9;
                currentRow += 1 + itemRows;
                ResourceLocation texture = section.texture();
                AnimBanner animBanner = section.animBanner();
                int visibleRow = bannerRow - scrollRow;
                if (visibleRow < 0 || visibleRow >= 5) continue;
                int bannerY = top + visibleRow * 18;
                AnimBanner.blitAnimated(guiGraphics, texture, animBanner, left, bannerY, 162, mouseX, mouseY, true);
            }
        }
    }
}
