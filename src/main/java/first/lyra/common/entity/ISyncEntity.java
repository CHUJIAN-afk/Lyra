package first.lyra.common.entity;

import first.lyra.common.attachmentEntity.SyncFieldDispatcher;
import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public interface ISyncEntity {

    default Entity getSyncEntity() {
        return (Entity) this;
    }

    default EntitySyncData getSyncData() {
        return getSyncEntity().getData(LyraAttachmentRegister.SyncData);
    }

    default SyncFieldDispatcher getSyncFieldDispatcher() {
        return getSyncData().getFields();
    }

    default void registerSyncFields(SyncFieldDispatcher fields) {
    }

    default void saveSyncFields(CompoundTag tag) {
        tag.put("lyra:sync_fields", getSyncFieldDispatcher().saveNBT(getSyncEntity().level()));
    }

    default void loadSyncFields(CompoundTag tag) {
        getSyncFieldDispatcher().loadNBT(tag.getCompound("lyra:sync_fields"), getSyncEntity().level());
    }

    default void tickSyncFields() {
        getSyncData().tick();
    }
}
