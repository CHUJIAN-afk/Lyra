package first.lyra.common.attachmentEntity;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 字段同步与存档注册器。
 * <p>
 * 子类在 {@code AttachmentEntity#registerSyncFields} 中按声明顺序注册字段：
 * </p>
 * <pre>{@code
 * fields.field(ByteBufCodecs.BOOL, () -> shooting, value -> shooting = value);
 * }</pre>
 */
public final class SyncFieldDispatcher {

    public static SyncFieldDispatcher create(Consumer<SyncFieldDispatcher> consumer) {
        SyncFieldDispatcher dispatcher = new SyncFieldDispatcher();
        consumer.accept(dispatcher);
        return dispatcher;
    }

    private interface SyncEntry {
        boolean encode(RegistryFriendlyByteBuf buf, Level level, boolean initialSync);

        void decode(RegistryFriendlyByteBuf buf, Level level);
    }

    private interface FieldEncoder {
        void encode(RegistryFriendlyByteBuf buf, Level level);
    }

    private interface FieldDecoder {
        void decode(RegistryFriendlyByteBuf buf, Level level);
    }

    private static final class FieldSyncEntry implements SyncEntry {
        private final FieldEncoder encoder;
        private final FieldDecoder decoder;
        private byte[] previousValue;

        private FieldSyncEntry(FieldEncoder encoder, FieldDecoder decoder) {
            this.encoder = encoder;
            this.decoder = decoder;
        }

        @Override
        public boolean encode(RegistryFriendlyByteBuf buf, Level level, boolean initialSync) {
            int start = buf.writerIndex();
            encoder.encode(buf, level);
            if (!initialSync && matches(buf, start, previousValue)) {
                buf.writerIndex(start);
                return false;
            }

            // 新追踪者的全量快照不改变已有客户端的增量基线。
            if (!initialSync) {
                byte[] currentValue = new byte[buf.writerIndex() - start];
                buf.getBytes(start, currentValue);
                previousValue = currentValue;
            }
            return true;
        }

        private static boolean matches(RegistryFriendlyByteBuf buf, int start, byte[] expected) {
            int length = buf.writerIndex() - start;
            if (expected == null || expected.length != length) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (buf.getByte(start + i) != expected[i]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void decode(RegistryFriendlyByteBuf buf, Level level) {
            decoder.decode(buf, level);
        }
    }

    private final List<SyncEntry> entries = new ArrayList<>();

    public <T> void field(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Supplier<T> getter, Consumer<T> setter) {
        entries.add(new FieldSyncEntry((buf, level) -> codec.encode(buf, getter.get()), (buf, level) -> setter.accept(codec.decode(buf))));
    }

    public <T> void field(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Function<Level, T> getter, BiConsumer<Level, T> setter) {
        entries.add(new FieldSyncEntry((buf, level) -> codec.encode(buf, getter.apply(level)), (buf, level) -> setter.accept(level, codec.decode(buf))));
    }

    public <T> void field(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Supplier<T> getter, BiConsumer<Level, T> setter) {
        entries.add(new FieldSyncEntry((buf, level) -> codec.encode(buf, getter.get()), (buf, level) -> setter.accept(level, codec.decode(buf))));
    }

    public <T> void field(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Function<Level, T> getter, Consumer<T> setter) {
        entries.add(new FieldSyncEntry((buf, level) -> codec.encode(buf, getter.apply(level)), (buf, level) -> setter.accept(codec.decode(buf))));
    }

    public CompoundTag saveNBT(Level level) {
        CompoundTag tag = new CompoundTag();
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
        try {
            for (int i = 0; i < entries.size(); i++) {
                buf.clear();
                entries.get(i).encode(buf, level, true);
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                tag.putByteArray(Integer.toString(i), bytes);
            }
        } finally {
            buf.release();
        }
        return tag;
    }

    public void loadNBT(CompoundTag tag, Level level) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
        try {
            for (int i = 0; i < entries.size(); i++) {
                String key = Integer.toString(i);
                if (tag.contains(key, Tag.TAG_BYTE_ARRAY)) {
                    buf.clear();
                    buf.writeBytes(tag.getByteArray(key));
                    entries.get(i).decode(buf, level);
                }
            }
        } finally {
            buf.release();
        }
    }

    public void encode(RegistryFriendlyByteBuf buf, Level level, boolean initialSync) {
        encodeChanges(buf, level, initialSync);
    }

    public boolean encodeChanges(RegistryFriendlyByteBuf buf, Level level, boolean initialSync) {
        // 位图标记本 tick 实际需要写入的字段，字段值仍保持各自 codec 的完整编码。
        int maskStart = buf.writerIndex();
        int maskSize = (entries.size() + 7) >>> 3;
        for (int i = 0; i < maskSize; i++) {
            buf.writeByte(0);
        }
        boolean changed = false;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).encode(buf, level, initialSync)) {
                changed = true;
                int maskIndex = maskStart + (i >>> 3);
                buf.setByte(maskIndex, buf.getByte(maskIndex) | (1 << (i & 7)));
            }
        }
        return changed;
    }

    public void decode(RegistryFriendlyByteBuf buf, Level level) {
        if (entries.isEmpty()) {
            return;
        }
        int maskSize = (entries.size() + 7) >>> 3;
        byte[] mask = new byte[maskSize];
        buf.readBytes(mask);
        for (int i = 0; i < entries.size(); i++) {
            if ((mask[i >>> 3] & (1 << (i & 7))) != 0) {
                entries.get(i).decode(buf, level);
            }
        }
    }
}
