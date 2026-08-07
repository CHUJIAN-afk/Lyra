package first.lyra;

import first.lyra.client.config.ClientConfig;
import first.lyra.dataGenerator.LyraDataGeneratorEvent;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraAttributeRegister;
import first.lyra.register.LyraNetworkPacketRegister;
import first.lyra.register.LyraParticleRegister;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(Lyra.MODID)
public class Lyra {

    public static final String MODID = "lyra";

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path.toLowerCase());
    }

    public Lyra(IEventBus eventBus, Dist dist, ModContainer container) {
        LyraAttachmentRegister.register(eventBus);
        LyraAttributeRegister.register(eventBus);
        LyraParticleRegister.register(eventBus);
        if (dist.isClient()) {
            container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.Spec);
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }
}
