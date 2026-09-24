package first.lyra.client;

import first.lyra.client.render.model.bbmodel.BBModelManager;
import first.lyra.client.render.model.geo.GeoAnimationManager;
import first.lyra.client.render.model.geo.GeoModelManager;
import first.lyra.client.tooltip.TooltipHandler;
import first.lyra.common.damageInfo.DamageInfoStyleManager;
import first.lyra.common.particle.genericParticle.GenericParticleProvider;
import first.lyra.register.LyraParticleRegister;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

public class ClientEvent {

    public static void init(IEventBus eventBus) {
        eventBus.addListener(EventPriority.LOWEST, (ItemTooltipEvent event) -> {
            Player player = event.getEntity();
            ItemStack itemStack = event.getItemStack();
            List<Component> toolTip = event.getToolTip();
            toolTip.addAll(TooltipHandler.getMinionWeaponItemTooltip(itemStack, player));
            toolTip.addAll(TooltipHandler.getCustomTooltip(itemStack, player));
        });
        eventBus.addListener((RegisterClientReloadListenersEvent event) -> {
            event.registerReloadListener(DamageInfoStyleManager.INSTANCE);
            event.registerReloadListener(GeoModelManager.INSTANCE);
            event.registerReloadListener(GeoAnimationManager.INSTANCE);
            event.registerReloadListener(BBModelManager.INSTANCE);
        });
        eventBus.addListener((RegisterParticleProvidersEvent event) -> event.registerSpriteSet(LyraParticleRegister.Generic.get(), GenericParticleProvider::new));
    }
}
