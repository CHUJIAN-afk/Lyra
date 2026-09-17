package first.lyra;

import first.lyra.client.config.ClientConfig;
import first.lyra.register.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.mesdag.portlib.network.PortNetworkHandler;

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
        Registries.register(eventBus, null);
        LyraAttachmentRegister.register();
        LyraAttributeRegister.register();
        LyraDataComponentRegister.register();
        LyraParticleRegister.register(eventBus);
        LyraNetworkPacketRegister.register();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.Spec);
        }
    }
}
