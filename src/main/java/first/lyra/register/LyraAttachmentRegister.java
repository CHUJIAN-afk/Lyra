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
                    .sync(new AttachmentEntityData.SyncHandler()));

    public static final PortRegistryEntry<PortAttachmentType<?>, PortAttachmentType<SummonMarkTracker>> SummonMarkData =
            Register.registerSimple("summon_mark_tracker", () -> PortAttachmentType.builder(SummonMarkTracker::new)
                    .sync(new SummonMarkTracker.SyncHandler()));

    public static final PortRegistryEntry<PortAttachmentType<?>, PortAttachmentType<ImmunityData>> ImmunityData =
            Register.registerSimple("immunity_data", () -> PortAttachmentType.builder(ImmunityData::new));

    public static final PortRegistryEntry<PortAttachmentType<?>, PortAttachmentType<InvincibleData>> InvincibleData =
            Register.registerSimple("invincible_data", () -> PortAttachmentType.builder(InvincibleData::new));

    public static final PortRegistryEntry<PortAttachmentType<?>, PortAttachmentType<HealthData>> HealthData =
            Register.registerSimple("health_data", () -> PortAttachmentType.builder(HealthData::new));

    public static final PortRegistryEntry<PortAttachmentType<?>, PortAttachmentType<TargetCache>> TargetCache =
            Register.registerSimple("target_cache", () -> PortAttachmentType.builder(TargetCache::new));

    public static final PortRegistryEntry<PortAttachmentType<?>, PortAttachmentType<ParticlesData>> BatchedParticles =
            Register.registerSimple("batched_particles", () -> PortAttachmentType.builder(ParticlesData::new));

    public static void register() {
    }
}
