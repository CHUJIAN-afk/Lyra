package first.lyra.client;

import first.lyra.Lyra;
import first.lyra.client.tooltip.TooltipHandler;
import first.lyra.common.damageInfo.DamageInfoStyleManager;
import first.lyra.common.particle.genericParticle.GenericParticleProvider;
import first.lyra.register.LyraParticleRegister;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = Lyra.MODID, value = Dist.CLIENT)
public class ClientEvent {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();
        List<Component> toolTip = event.getToolTip();
        toolTip.addAll(TooltipHandler.getMinionWeaponItemTooltip(itemStack, player));
        toolTip.addAll(TooltipHandler.getCustomTooltip(itemStack, player));
    }

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(DamageInfoStyleManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(LyraParticleRegister.Generic.get(), GenericParticleProvider::new);
    }
}
