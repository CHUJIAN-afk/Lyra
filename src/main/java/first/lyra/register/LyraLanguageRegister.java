package first.lyra.register;

import first.lyra.Lyra;

public class LyraLanguageRegister {

    public static void init() {
        // ================= 工具提示 =================
        language("item.lyra.tooltip.damage", "Summon Damage", "召唤伤害");
        language("item.lyra.tooltip.knockback", "Knockback", "击退强度");
        language("item.lyra.tooltip.armor_pierce", "Armor Pierce", "护甲穿透");
        language("item.lyra.tooltip.summon", "Summons %s to fight for you", "召唤 %s 为你而战");
        language("item.lyra.tooltip.minion_slots", "Minion Slots: %s / %s", "仆从栏位: %s / %s");
        language("item.lyra.tooltip.sentry_slots", "Sentry Slots: %s / %s", "哨兵栏位: %s / %s");
        language("item.lyra.tooltip.remove_all", "Sneak + Use to dismiss all summons of this type", "潜行右键以遣散该召唤物");
        language("item.lyra.tooltip.set_bonus_title", "Set Rewards:", "套装奖励:");
        // ================= 属性 =================
        language("attribute.lyra.health_regen", "Health Regen", "生命再生");
        language("attribute.lyra.minion_max_count", "Max Minions", "仆从栏");
        language("attribute.lyra.sentry_max_count", "Max Sentrys", "哨兵栏");
        language("attribute.lyra.summon_damage", "Summon Damage", "召唤伤害");
        language("attribute.lyra.summon_knockback", "Summon Knockback", "召唤击退");
        language("attribute.lyra.summon_armor_pierce", "Summon Armor Pierce", "召唤护甲穿透");
        language("attribute.lyra.summon_search_range", "Summon Search Range", "召唤索敌范围");
        // ================= 死亡消息 =================
        language("death.attack.lyra.summon", "%1$s was torn apart by %2$s", "%1$s 被 %2$s 撕碎");
        language("death.attack.lyra.summon.player", "%1$s was torn apart by a summon whilst fighting %2$s", "%1$s 在与 %2$s 战斗时被召唤物撕碎");
        // ================= 客户端配置 =================
        language("lyra.configuration.title", "Lyra Configuration", "Lyra 配置");
        language("lyra.configuration.section.lyra.client.toml", "Client", "客户端");
        language("lyra.configuration.section.lyra.client.toml.title", "Client Configuration", "客户端配置");
        language("lyra.configuration.alpha_modify", "Alpha Modify", "透明度修正");
        language("lyra.configuration.alpha_modify.tooltip", "Enable Lyra proximity alpha modification, making summons transparent at close range to avoid blocking view", "启用 Lyra 近距离透明度修正，近距离透明化避免遮挡视线");
        language("lyra.configuration.debug_mode", "Debug Mode", "调试模式");
        language("lyra.configuration.debug_mode.tooltip", "Enable virtual entity debug strokes", "启用虚拟实体调试描边");
    }

    private static void language(String key, String en, String zh) {
        Lyra.REGISTRATE.get().lang(key, en, zh);
    }
}
