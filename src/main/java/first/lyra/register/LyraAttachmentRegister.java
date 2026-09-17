package first.lyra.register;

import first.lyra.Lyra;
import first.lyra.common.attachment.*;
import org.mesdag.portlib.attachment.PortAttachmentType;
import org.mesdag.portlib.registries.PortAttachmentRegistration;
import org.mesdag.portlib.registries.PortRegisterHandler;
import org.mesdag.portlib.registries.PortRegistryEntry;

public class LyraAttachmentRegister {

    private static final PortAttachmentRegistration Register = PortRegisterHandler.attachment(Lyra.MODID);

    public static final PortRegistryEntry<PortAttachmentType<?>, PortAttachmentType<AttachmentEntityData>> EntityData =
            Register.registerSimple("attachment_entity_data", () -> PortAttachmentType.builder(AttachmentEntityData::new)
                    .sync(new AttachmentEntityData()));

    public static final PortRegistryEntry<PortAttachmentType<?>, PortAttachmentType<InvincibleData>> InvincibleData =
            Register.registerSimple("invincible_data", () -> PortAttachmentType.builder(InvincibleData::new));

    public static final PortRegistryEntry<PortAttachmentType<?>, PortAttachmentType<HealthData>> HealthData =
            Register.registerSimple("health_data", () -> PortAttachmentType.builder(HealthData::new));

    public static final PortRegistryEntry<PortAttachmentType<?>, PortAttachmentType<TargetCache>> TargetCache =
            Register.registerSimple("target_cache", () -> PortAttachmentType.builder(TargetCache::new));

    /**
     * 玩家盔甲套装生效状态附件（装备变化事件维护，不同步）
     */
    public static final PortRegistryEntry<PortAttachmentType<?>, PortAttachmentType<ArmorSetData>> ArmorSetData =
            Register.registerSimple("armor_set_data", () -> PortAttachmentType.builder(ArmorSetData::new));

    /**
     * Level 级批量粒子累积附件（仅服务端使用，不同步）
     */
    public static final PortRegistryEntry<PortAttachmentType<?>, PortAttachmentType<ParticlesData>> BatchedParticles =
            Register.registerSimple("batched_particles", () -> PortAttachmentType.builder(ParticlesData::new));

    /**
     * Level 级伤害数字累积附件（服务端累积，客户端接收渲染，不同步）
     */
    public static final PortRegistryEntry<PortAttachmentType<?>, PortAttachmentType<DamageInfoData>> DamageInfoData =
            Register.registerSimple("damage_info_data", () -> PortAttachmentType.builder(DamageInfoData::new));

    public static void register() {}

}
