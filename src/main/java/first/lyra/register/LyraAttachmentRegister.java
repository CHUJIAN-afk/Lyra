package first.lyra.register;

import first.lyra.Lyra;
import first.lyra.common.attachment.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class LyraAttachmentRegister {

    private static final DeferredRegister<AttachmentType<?>> Register =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Lyra.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AttachmentEntityData>> EntityData =
            Register.register("attachment_entity_data", () -> AttachmentType.builder(AttachmentEntityData::new)
                    .sync(new AttachmentEntityData())
                    .build()
            );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<InvincibleData>> InvincibleData =
            Register.register("invincible_data", () -> AttachmentType.builder(InvincibleData::new).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<HealthData>> HealthData =
            Register.register("health_data", () -> AttachmentType.builder(HealthData::new).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<TargetCache>> TargetCache =
            Register.register("target_cache", () -> AttachmentType.builder(TargetCache::new).build());

    /**
     * 玩家盔甲套装生效状态附件（装备变化事件维护，不同步）
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ArmorSetData>> ArmorSetData =
            Register.register("armor_set_data", () -> AttachmentType.builder(ArmorSetData::new).build());

    /**
     * Level 级批量粒子累积附件（仅服务端使用，不同步）
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ParticlesData>> BatchedParticles =
            Register.register("batched_particles", () -> AttachmentType.builder(ParticlesData::new).build());

    /**
     * Level 级伤害数字累积附件（服务端累积，客户端接收渲染，不同步）
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DamageInfoData>> DamageInfoData =
            Register.register("damage_info_data", () -> AttachmentType.builder(DamageInfoData::new).build());

    public static void register(IEventBus eventbus) {
        Register.register(eventbus);
    }

}
