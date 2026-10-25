package first.lyra.common.entity;

import first.lyra.common.attachmentEntity.SyncFieldDispatcher;
import first.lyra.register.LyraAttachmentRegister;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EntitySyncData {

    private record Update(boolean initialSync, byte[] bytes) {
    }

    private final Entity entity;
    private final SyncFieldDispatcher fields;
    private final List<Update> pending = new ArrayList<>();
    private byte[] changes;
    EntityPathData path;

    public EntitySyncData(IAttachmentHolder holder) {
        entity = (Entity) holder;
        fields = SyncFieldDispatcher.create(((ISyncEntity) entity)::registerSyncFields);
    }

    public SyncFieldDispatcher getFields() {
        return fields;
    }

    public void tick() {
        if (entity.level().isClientSide()) {
            for (Update update : pending) {
                RegistryFriendlyByteBuf buf = buffer(update.bytes);
                try {
                    fields.decode(buf, entity.level());
                } finally {
                    buf.release();
                }
                if (update.initialSync && entity instanceof IPathEntity pathEntity) {
                    EntityPathData data = pathEntity.getPathData();
                    data.history.clear();
                    data.history.add(data.node);
                    data.lastRenderTick = data.tick;
                }
            }
            pending.clear();
        } else {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), entity.registryAccess(), ConnectionType.NEOFORGE);
            boolean changed;
            try {
                changed = fields.encodeChanges(buf, entity.level(), false);
                changes = new byte[buf.readableBytes()];
                buf.readBytes(changes);
            } finally {
                buf.release();
            }
            if (changed) {
                entity.syncData(LyraAttachmentRegister.SyncData);
            }
        }
    }

    private RegistryFriendlyByteBuf buffer(byte[] bytes) {
        return new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), entity.registryAccess(), ConnectionType.NEOFORGE);
    }

    public static final class SyncHandler implements AttachmentSyncHandler<EntitySyncData> {

        @Override
        public void write(RegistryFriendlyByteBuf buf, EntitySyncData data, boolean initialSync) {
            boolean full = initialSync || data.changes == null;
            buf.writeBoolean(full);
            int lengthIndex = buf.writerIndex();
            buf.writeInt(0);
            int start = buf.writerIndex();
            if (full) {
                data.fields.encode(buf, data.entity.level(), true);
            } else {
                buf.writeBytes(data.changes);
            }
            buf.setInt(lengthIndex, buf.writerIndex() - start);
        }

        @Override
        public EntitySyncData read(IAttachmentHolder holder, RegistryFriendlyByteBuf buf, @Nullable EntitySyncData oldData) {
            EntitySyncData data = oldData == null ? new EntitySyncData(holder) : oldData;
            boolean initialSync = buf.readBoolean();
            byte[] bytes = new byte[buf.readInt()];
            buf.readBytes(bytes);
            data.pending.add(new Update(initialSync, bytes));
            return data;
        }
    }
}
