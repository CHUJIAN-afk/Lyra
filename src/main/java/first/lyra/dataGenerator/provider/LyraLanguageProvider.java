package first.lyra.dataGenerator.provider;

import first.lyra.register.LyraAttributeRegister;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.HashMap;
import java.util.Map;

public class LyraLanguageProvider extends LanguageProvider {

    public static final Map<String, String[]> LanguageGenerate = new HashMap<>();

    protected final String locale;

    public LyraLanguageProvider(PackOutput output, String modid, String locale) {
        super(output, modid, locale);
        this.locale = locale;
    }

    public static void entry(String key, String en, String zh) {
        LanguageGenerate.put(key, new String[]{en, zh});
    }

    protected void init() {
        // ================= 工具提示 =================
        entry("item.lyra.tooltip.damage", "Summon Damage", "仆从伤害");
        entry("item.lyra.tooltip.knockback", "Knockback", "击退强度");
        entry("item.lyra.tooltip.armor_pierce", "Armor Pierce", "护甲穿透");
        entry("item.lyra.tooltip.summon", "Summons %s to fight for you", "召唤 %s 为你而战");
        entry("item.lyra.tooltip.servant_slots", "Servant Slots: %s / %s", "仆从栏位: %s / %s");
        entry("item.lyra.tooltip.sentry_servant_slots", "Sentry Servant Slots: %s / %s", "哨戒仆从栏位: %s / %s");
        entry("item.lyra.tooltip.remove_all", "Sneak + Use to dismiss all servants of this type", "潜行右键以遣散该类型仆从");
        entry("item.lyra.tooltip.set_bonus_title", "Set Rewards:", "套装奖励:");
        // ================= 属性 =================
        entry(LyraAttributeRegister.HealthRegen.get().getDescriptionId(), "Health Regen", "生命再生");
        entry(LyraAttributeRegister.ServantMaxCount.get().getDescriptionId(), "Max Servants", "仆从栏");
        entry(LyraAttributeRegister.SentryServantMaxCount.get().getDescriptionId(), "Max Sentry Servants", "哨戒仆从栏");
        entry(LyraAttributeRegister.ServantDamage.get().getDescriptionId(), "Servant Damage", "仆从伤害");
        entry(LyraAttributeRegister.ServantKnockback.get().getDescriptionId(), "Servant Knockback", "仆从击退");
        entry(LyraAttributeRegister.ServantArmorPierce.get().getDescriptionId(), "Servant Armor Pierce", "仆从护甲穿透");
        entry(LyraAttributeRegister.ServantSearchRange.get().getDescriptionId(), "Servant Search Range", "仆从索敌范围");
        // ================= 死亡消息 =================
        // %2$s = 仆从翻译名称(servant.<namespace>.<path>,宿主 mod 语言文件提供)
        entry("death.attack.lyra.servant", "%1$s was torn apart by %2$s", "%1$s 被 %2$s 撕碎");
        entry("death.attack.lyra.servant.player", "%1$s was torn apart by a servant whilst fighting %2$s", "%1$s 在与 %2$s 战斗时被撕碎");
        // ================= 客户端配置 =================
        entry("lyra.configuration.title", "Lyra Configuration", "Lyra 配置");
        entry("lyra.configuration.section.lyra.client.toml", "Client", "客户端");
        entry("lyra.configuration.section.lyra.client.toml.title", "Client Configuration", "客户端配置");
        entry("lyra.configuration.dynamic_light", "Dynamic Light", "动态光照");
        entry("lyra.configuration.dynamic_light.tooltip", "Enable Lyra dynamic lighting, which increases additional rendering performance cost", "启用 Lyra 动态光照，会增加额外的渲染性能消耗");
        entry("lyra.configuration.alpha_modify", "Alpha Modify", "透明度修正");
        entry("lyra.configuration.alpha_modify.tooltip", "Enable Lyra proximity alpha modification, making servants transparent at close range to avoid blocking view", "启用 Lyra 近距离透明度修正，近距离透明化避免遮挡视线");
        entry("lyra.configuration.damage_info", "Damage Info", "伤害信息");
        entry("lyra.configuration.damage_info.tooltip", "Enable Lyra high-performance damage info, with negligible impact on client rendering performance", "启用 Lyra 高性能伤害信息，几乎不会影响客户端的渲染性能");
        entry("lyra.configuration.debug_mode", "Debug Mode", "调试模式");
        entry("lyra.configuration.debug_mode.tooltip", "Enable virtual entity debug strokes", "启用虚拟实体调试描边");
    }

    @Override
    protected void addTranslations() {
        init();
        LanguageGenerate.forEach((key, value) -> {
            if (key != null) {
                String enDesc = value[0];
                String zhDesc = value[1];
                if (enDesc != null && "en_us".equals(locale)) {
                    add(key, enDesc);
                }
                if (zhDesc != null && "zh_cn".equals(locale)) {
                    add(key, zhDesc);
                }
            }
        });
    }
}
