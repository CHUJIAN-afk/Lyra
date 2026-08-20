package first.lyra.dataGenerator.provider;

import first.lyra.register.LyraAttributeRegister;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LyraLanguageProvider extends LanguageProvider {

    public static final Map<ItemLike, List<Component>> InfoMap = new HashMap<>();
    public static final Map<String, String[]> LanguageGenerate = new HashMap<>();

    protected final String locale;

    public LyraLanguageProvider(PackOutput output, String modid, String locale) {
        super(output, modid, locale);
        this.locale = locale;
    }

    public static void entry(String key, String en, String zh) {
        LanguageGenerate.put(key, new String[]{en, zh});
    }

    public static void addIngredientInfo(ItemLike item, Component component) {
        if (ModList.get().isLoaded("jei") && FMLLoader.getDist().isClient()) {
            InfoMap.computeIfAbsent(item, key -> new ArrayList<>()).add(component);
        }
    }

    protected void init() {
        // ================= 工具提示 =================
        entry("item.lyra.tooltip.damage", "Summon Damage", "召唤伤害");
        entry("item.lyra.tooltip.knockback", "Knockback", "击退强度");
        entry("item.lyra.tooltip.armor_pierce", "Armor Pierce", "护甲穿透");
        entry("item.lyra.tooltip.summon", "Summons %s to fight for you", "召唤 %s 为你而战");
        entry("item.lyra.tooltip.minion_slots", "Minion Slots: %s / %s", "仆从栏位: %s / %s");
        entry("item.lyra.tooltip.sentry_slots", "Sentry Slots: %s / %s", "哨兵栏位: %s / %s");
        entry("item.lyra.tooltip.remove_all", "Sneak + Use to dismiss all summons of this type", "潜行右键以遣散该召唤物");
        entry("item.lyra.tooltip.set_bonus_title", "Set Rewards:", "套装奖励:");
        // ================= 属性 =================
        entry(LyraAttributeRegister.HealthRegen.get().getDescriptionId(), "Health Regen", "生命再生");
        entry(LyraAttributeRegister.MinionMaxCount.get().getDescriptionId(), "Max Minions", "仆从栏");
        entry(LyraAttributeRegister.SentryMaxCount.get().getDescriptionId(), "Max Sentrys", "哨兵栏");
        entry(LyraAttributeRegister.SummonDamage.get().getDescriptionId(), "Summon Damage", "召唤伤害");
        entry(LyraAttributeRegister.SummonKnockback.get().getDescriptionId(), "Summon Knockback", "召唤击退");
        entry(LyraAttributeRegister.SummonArmorPierce.get().getDescriptionId(), "Summon Armor Pierce", "召唤护甲穿透");
        entry(LyraAttributeRegister.SummonSearchRange.get().getDescriptionId(), "Summon Search Range", "召唤索敌范围");
        // ================= 死亡消息 =================
        entry("death.attack.lyra.summon", "%1$s was torn apart by %2$s", "%1$s 被 %2$s 撕碎");
        entry("death.attack.lyra.summon.player", "%1$s was torn apart by a summon whilst fighting %2$s", "%1$s 在与 %2$s 战斗时被召唤物撕碎");
        // ================= 客户端配置 =================
        entry("lyra.configuration.title", "Lyra Configuration", "Lyra 配置");
        entry("lyra.configuration.section.lyra.client.toml", "Client", "客户端");
        entry("lyra.configuration.section.lyra.client.toml.title", "Client Configuration", "客户端配置");
        entry("lyra.configuration.alpha_modify", "Alpha Modify", "透明度修正");
        entry("lyra.configuration.alpha_modify.tooltip", "Enable Lyra proximity alpha modification, making summons transparent at close range to avoid blocking view", "启用 Lyra 近距离透明度修正，近距离透明化避免遮挡视线");
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
