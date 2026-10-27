package first.lyra.register;

public class LyraLanguageProvider extends LyraAbstractLanguageProvider {

    public LyraLanguageProvider(String modid) {
        super(modid);
    }

    @Override
    public void translations() {
        // ================= 工具提示 =================
        lang("item.lyra.tooltip.damage", "召唤伤害", "Summon Damage");
        lang("item.lyra.tooltip.knockback", "击退强度", "Knockback");
        lang("item.lyra.tooltip.armor_pierce", "护甲穿透", "Armor Pierce");
        lang("item.lyra.tooltip.summon", "召唤 %s 为你而战", "Summons %s to fight for you");
        lang("item.lyra.tooltip.minion_slots", "仆从栏位: %s / %s", "Minion Slots: %s / %s");
        lang("item.lyra.tooltip.sentry_slots", "哨兵栏位: %s / %s", "Sentry Slots: %s / %s");
        lang("item.lyra.tooltip.remove_all", "潜行右键以遣散该召唤物", "Sneak + Use to dismiss all summons of this type");
        lang("item.lyra.tooltip.set_bonus_title", "套装奖励:", "Set Rewards:");
        // ================= 属性 =================
        lang("attribute.lyra.health_regen", "生命再生", "Health Regen");
        lang("attribute.lyra.minion_max_count", "仆从栏", "Max Minions");
        lang("attribute.lyra.sentry_max_count", "哨兵栏", "Max Sentrys");
        lang("attribute.lyra.summon_damage", "召唤伤害", "Summon Damage");
        lang("attribute.lyra.summon_knockback", "召唤击退", "Summon Knockback");
        lang("attribute.lyra.summon_armor_pierce", "召唤护甲穿透", "Summon Armor Pierce");
        lang("attribute.lyra.summon_search_range", "召唤索敌范围", "Summon Search Range");
        // ================= 死亡消息 =================
        lang("death.attack.lyra.summon", "%1$s 被 %2$s 撕碎", "%1$s was torn apart by %2$s");
        lang("death.attack.lyra.summon.player", "%1$s 在与 %2$s 战斗时被召唤物撕碎", "%1$s was torn apart by a summon whilst fighting %2$s");
        // ================= 客户端配置 =================
        lang("lyra.configuration.title", "Lyra 配置", "Lyra Configuration");
        lang("lyra.configuration.section.lyra.client.toml", "客户端", "Client");
        lang("lyra.configuration.section.lyra.client.toml.title", "客户端配置", "Client Configuration");
        lang("lyra.configuration.alpha_modify", "透明度修正", "Alpha Modify");
        lang("lyra.configuration.alpha_modify.tooltip", "启用 Lyra 近距离透明度修正，近距离透明化避免遮挡视线", "Enable Lyra proximity alpha modification, making summons transparent at close range to avoid blocking view");
        lang("lyra.configuration.debug_mode", "调试模式", "Debug Mode");
        lang("lyra.configuration.debug_mode.tooltip", "启用虚拟实体调试描边", "Enable virtual entity debug strokes");
    }
}
