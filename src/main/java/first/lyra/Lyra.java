package first.lyra;

import first.lyra.client.config.ClientConfig;
import first.lyra.register.*;
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
    public static final LyraItemRegistries Registries = LyraItemRegistries.create(MODID).languageInit(new LyraLanguageRegister());

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path.toLowerCase());
    }

    public Lyra(IEventBus eventBus, Dist dist, ModContainer container) {
        Registries.register(eventBus, null);
        LyraAttachmentRegister.register(eventBus);
        LyraAttributeRegister.register(eventBus);
        LyraDataComponentRegister.register(eventBus);
        LyraParticleRegister.register(eventBus);
        if (dist.isClient()) {
            container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.Spec);
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }
}
