package first.lyra;

import first.lyra.client.ClientGameEvent;
import first.lyra.client.ClientModEvent;
import first.lyra.client.config.ClientConfig;
import first.lyra.common.ForgeGameEvent;
import first.lyra.common.ForgeModEvent;
import first.lyra.register.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.mesdag.portlib.network.PortNetworkHandler;
import org.mesdag.portlib.wrapper.PortEnvironment;

@Mod(Lyra.MODID)
public class Lyra {

    public static final String MODID = "lyra";
    public static final LyraItemRegistries Registries = LyraItemRegistries.create(MODID).languageInit(new LyraLanguageRegister());
    public static final PortNetworkHandler NETWORK_HANDLER = new PortNetworkHandler(MODID, "1");

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path.toLowerCase());
    }

    public Lyra(FMLJavaModLoadingContext context) {
        IEventBus eventBus = context.getModEventBus();
        Registries.register(eventBus);
        LyraAttachmentRegister.register();
        LyraAttributeRegister.register();
        LyraDataComponentRegister.register();
        LyraParticleRegister.register(eventBus);
        LyraNetworkPacketRegister.register();
        ForgeGameEvent.init();
        ForgeModEvent.init();
        if (PortEnvironment.isPhysicalClient()) {
            ClientGameEvent.init();
            ClientModEvent.init();
            context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.Spec);
        }
    }
}
