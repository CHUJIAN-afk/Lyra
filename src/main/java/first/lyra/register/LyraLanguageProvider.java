package first.lyra.register;

import first.lyra.provider.LyraAbstractLanguageProvider;

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
        lang("lyra.configuration.dynamic_lights", "动态光源", "Dynamic Lights");
        lang("lyra.configuration.dynamic_lights.tooltip", "启用 Lyra 动态光源渲染", "Enable Lyra dynamic lighting rendering");
        lang("lyra.configuration.max_lights", "最大光源数量", "Max Lights");
        lang("lyra.configuration.max_lights.tooltip", "单帧最多渲染的动态光源数量，按距离和强度优先保留", "Maximum number of dynamic lights rendered per frame, prioritized by distance and intensity");
        lang("lyra.configuration.bloom", "泛光效果", "Bloom");
        lang("lyra.configuration.bloom.tooltip", "启用 Lyra 特效的泛光后处理效果", "Enable bloom post-processing for Lyra effects");
        lang("lyra.configuration.bloom_with_shaders", "光影下启用泛光", "Bloom With Shaders");
        lang("lyra.configuration.bloom_with_shaders.tooltip", "使用光影包时仍启用 Lyra 泛光效果", "Keep Lyra bloom enabled while using a shader pack");
        lang("lyra.configuration.bloom_levels", "泛光层级", "Bloom Levels");
        lang("lyra.configuration.bloom_levels.tooltip", "泛光模糊的采样层级数，数值越大扩散范围越广但开销越高", "Number of bloom blur pyramid levels; higher values widen the glow at higher performance cost");
        lang("lyra.configuration.bloom_threshold", "泛光阈值", "Bloom Threshold");
        lang("lyra.configuration.bloom_threshold.tooltip", "触发泛光的亮度阈值，仅亮度高于该值的像素会产生泛光", "Brightness threshold above which pixels contribute to the bloom effect");
        lang("lyra.configuration.bloom_strength", "泛光强度", "Bloom Strength");
        lang("lyra.configuration.bloom_strength.tooltip", "泛光叠加到画面中的强度倍率", "Intensity multiplier for compositing the bloom effect");
    }
}
