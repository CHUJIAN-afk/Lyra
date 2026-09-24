package first.lyra.client;

import first.lyra.Lyra;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * 客户端事件监听。
 * <p>
 * {@code value = Dist.CLIENT} 保证只在客户端加载，专用服务端不会触碰这些客户端类；
 * 1.21.1 下 {@link EventBusSubscriber} 不必指定总线，自动注册器会按每个
 * {@link SubscribeEvent} 方法的入参事件判定总线（本类混用两种总线：游戏总线的
 * {@link ItemTooltipEvent}，mod 总线的 {@link RegisterClientReloadListenersEvent} 与
 * {@link RegisterParticleProvidersEvent}）。
 */
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
        event.registerReloadListener(GeoModelManager.INSTANCE);
        event.registerReloadListener(GeoAnimationManager.INSTANCE);
        event.registerReloadListener(BBModelManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(LyraParticleRegister.Generic.get(), GenericParticleProvider::new);
    }
}
