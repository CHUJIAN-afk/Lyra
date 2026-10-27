package first.lyra;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import first.lyra.client.config.ClientConfig;
import first.lyra.register.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(Lyra.MODID)
public class Lyra {

    public static final String MODID = "lyra";
    public static final NonNullSupplier<Registrate> REGISTRATE = NonNullSupplier.lazy(() -> Registrate.create(MODID));

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path.toLowerCase());
    }

    public Lyra(Dist dist, ModContainer container) {
        // 触发注册类的类加载，保证所有条目在 RegisterEvent 之前完成登记
        LyraAttachmentRegister.init();
        LyraAttributeRegister.init();
        LyraDataComponentRegister.init();
        LyraParticleRegister.init();
        LyraRegistries.init();
        LyraDamageRegister.init();
        new LyraLanguageProvider(MODID).init();
        if (dist.isClient()) {
            container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.Spec);
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }
}
