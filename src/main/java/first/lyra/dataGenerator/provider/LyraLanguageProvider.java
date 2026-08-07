package first.lyra.dataGenerator.provider;

import first.lyra.register.LyraLanguageRegister;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * 语言数据生成：输出 {@link LyraLanguageRegister} 收集的动态语言条目
 * （宿主 mod 通过 LyraItemRegisterBuilder 注册）与 {@code init()} 注册的库静态条目。
 */
public class LyraLanguageProvider extends LanguageProvider {

    private final String locale;

    public LyraLanguageProvider(PackOutput output, String modid, String locale) {
        super(output, modid, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        LyraLanguageRegister.init();
        LyraLanguageRegister.LanguageGenerate.entrySet()
                .removeIf(entry -> {
                    String key = entry.getKey();
                    String[] value = entry.getValue();
                    String enDesc = value[0];
                    String zhDesc = value[1];
                    if (key != null) {
                        if (enDesc != null && "en_us".equals(locale)) {
                            add(key, enDesc);
                        }
                        if (zhDesc != null && "zh_cn".equals(locale)) {
                            add(key, zhDesc);
                            return true;
                        }
                    }
                    return false;
                });
    }
}
