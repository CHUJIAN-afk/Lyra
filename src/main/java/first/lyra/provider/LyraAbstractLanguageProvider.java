package first.lyra.provider;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class LyraAbstractLanguageProvider {

    private final String modid;
    private final Map<String, String> zh_cnLang = new HashMap<>();
    private final Map<String, String> en_usLang = new HashMap<>();

    protected LyraAbstractLanguageProvider(String modid) {
        this.modid = modid;
    }

    public void init() {
        Optional<IEventBus> optional = ModList.get().getModContainerById(modid).map(ModContainer::getEventBus);
        optional.ifPresent(eventBus -> eventBus.addListener((GatherDataEvent event) -> {
            translations();
            DataGenerator generator = event.getGenerator();
            PackOutput output = generator.getPackOutput();
            boolean client = event.includeClient();
            generator.addProvider(client, new Provider(output, modid, "zh_cn", zh_cnLang));
            generator.addProvider(client, new Provider(output, modid, "en_us", en_usLang));
        }));
    }

    public abstract void translations();

    public void lang(String key, String zh_cn, String en_us) {
        zh_cnLang.put(key, zh_cn);
        en_usLang.put(key, en_us);
    }

    public static class Provider extends LanguageProvider {

        public final Map<String, String> extraLang;

        public Provider(PackOutput output, String modid, String locale, Map<String, String> extraLang) {
            super(output, modid, locale);
            this.extraLang = extraLang;
        }

        @Override
        protected void addTranslations() {
            extraLang.forEach(this::add);
        }
    }
}
