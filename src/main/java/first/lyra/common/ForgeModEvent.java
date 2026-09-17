package first.lyra.common;

import first.lyra.Lyra;
import first.lyra.register.LyraAttributeRegister;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Lyra.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForgeModEvent {

    @SubscribeEvent
    public static void attributeModification(EntityAttributeModificationEvent event) {
        LyraAttributeRegister.handler(event);
    }
}
