package first.lyra.register;

import com.tterrag.registrate.Registrate;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Registrate 的 Lyra 扩展：语言生成额外补充 zh_cn，其余能力全部沿用 Registrate。
 */
public class LyraRegistrate extends Registrate {

    private final Map<String, String> zhCn = new HashMap<>();

    private LyraRegistrate(String modid) {
        super(modid);
    }

    public static LyraRegistrate create(String modid) {
        LyraRegistrate registrate = new LyraRegistrate(modid);
        ModList.get().getModContainerById(modid).map(ModContainer::getEventBus).ifPresent(registrate::registerEventListeners);
        return registrate;
    }

    @Override
    public @NotNull Registrate registerEventListeners(IEventBus bus) {
        bus.addListener(this::generateZhCn);
        return super.registerEventListeners(bus);
    }

    /**
     * 一次声明中英双语。
     * <p>
     * 英文交给 Registrate 的语言生成器；若该 key 已由 Registrate 的条目语言生成器负责（如物品），
     * 这里只补中文，避免重复键。
     * </p>
     */
    public void lang(String key, String en, String zh) {
        zhCn.put(key, zh);
        if (!hasEntryLang(key)) {
            addRawLang(key, en);
        }
    }

    private boolean hasEntryLang(String key) {
        String prefix = "item." + getModid() + ".";
        return key.startsWith(prefix) && getOptional(key.substring(prefix.length()), Registries.ITEM).isPresent();
    }

    private void generateZhCn(GatherDataEvent event) {
        if (event.includeClient()) {
            PackOutput output = event.getGenerator().getPackOutput();
            event.getGenerator().addProvider(true, new LanguageProvider(output, getModid(), "zh_cn") {
                @Override
                protected void addTranslations() {
                    zhCn.forEach(this::add);
                }
            });
        }
    }
}
