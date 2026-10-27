package first.lyra.register;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import first.lyra.Lyra;
import first.lyra.common.attachment.*;
import first.lyra.common.entity.EntitySyncData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class LyraAttachmentRegister {

    private static final Registrate REGISTER = Lyra.REGISTRATE.get();

    public static final RegistryEntry<AttachmentType<?>, AttachmentType<AttachmentEntityData>> EntityData =
            REGISTER.simple("attachment_entity_data", NeoForgeRegistries.Keys.ATTACHMENT_TYPES, () -> AttachmentType.builder(AttachmentEntityData::new)
                    .sync(new AttachmentEntityData.SyncHandler())
                    .build());

    public static final RegistryEntry<AttachmentType<?>, AttachmentType<EntitySyncData>> SyncData =
            REGISTER.simple("entity_sync_data", NeoForgeRegistries.Keys.ATTACHMENT_TYPES, () -> AttachmentType.builder(EntitySyncData::new)
                    .sync(new EntitySyncData.SyncHandler())
                    .build());

    public static final RegistryEntry<AttachmentType<?>, AttachmentType<SummonMarkTracker>> SummonMarkData =
            REGISTER.simple("summon_mark_tracker", NeoForgeRegistries.Keys.ATTACHMENT_TYPES, () -> AttachmentType.builder(SummonMarkTracker::new)
                    .sync(new SummonMarkTracker.SyncHandler())
                    .build());

    public static final RegistryEntry<AttachmentType<?>, AttachmentType<ImmunityData>> ImmunityData =
            REGISTER.simple("immunity_data", NeoForgeRegistries.Keys.ATTACHMENT_TYPES, () -> AttachmentType.builder(ImmunityData::new).build());

    public static final RegistryEntry<AttachmentType<?>, AttachmentType<HealthData>> HealthData =
            REGISTER.simple("health_data", NeoForgeRegistries.Keys.ATTACHMENT_TYPES, () -> AttachmentType.builder(HealthData::new).build());

    public static final RegistryEntry<AttachmentType<?>, AttachmentType<TargetCache>> TargetCache =
            REGISTER.simple("target_cache", NeoForgeRegistries.Keys.ATTACHMENT_TYPES, () -> AttachmentType.builder(TargetCache::new).build());

    public static final RegistryEntry<AttachmentType<?>, AttachmentType<ParticlesData>> BatchedParticles =
            REGISTER.simple("batched_particles", NeoForgeRegistries.Keys.ATTACHMENT_TYPES, () -> AttachmentType.builder(ParticlesData::new).build());

    public static void init() {
    }
}
