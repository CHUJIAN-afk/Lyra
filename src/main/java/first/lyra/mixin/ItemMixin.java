package first.lyra.mixin;

import first.lyra.common.dataComponent.LyraRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(
            method = "getName",
            at = @At("TAIL"),
            cancellable = true
    )
    private static void getName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        if (cir.getReturnValue() instanceof MutableComponent mutableComponent) {
            cir.setReturnValue(LyraRarity.handler(stack, mutableComponent));
        }
    }
}
