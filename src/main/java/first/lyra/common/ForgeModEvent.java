package first.lyra.common;

import first.lyra.register.LyraAttributeRegister;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import org.mesdag.portlib.event.PortEventHandler;

public final class ForgeModEvent {
    public static void init() {
        PortEventHandler.addListener(ForgeModEvent::attributeModification);
    }

    private static void attributeModification(EntityAttributeModificationEvent event) {
        LyraAttributeRegister.handler(event);
    }
}
