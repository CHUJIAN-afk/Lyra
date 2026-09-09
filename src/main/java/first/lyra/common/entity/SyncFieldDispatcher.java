package first.lyra.common.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 字段同步注册器。
 * <p>
 * 子类在 {@code AttachmentEntity#registerSyncFields} 中按声明顺序注册字段：
 * </p>
 * <pre>{@code
 * fields.field(ByteBufCodecs.BOOL, () -> shooting, value -> shooting = value);
 * }</pre>
 */
public final class SyncFieldDispatcher {

    private interface SyncEntry {
        void encode(RegistryFriendlyByteBuf buf, Level level);

        void decode(RegistryFriendlyByteBuf buf, Level level);
    }

    private final List<SyncEntry> entries = new ArrayList<>();

    public <T> void field(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Supplier<T> getter, Consumer<T> setter) {
        entries.add(new SyncEntry() {
            @Override
            public void encode(RegistryFriendlyByteBuf buf, Level level) {
                codec.encode(buf, getter.get());
            }

            @Override
            public void decode(RegistryFriendlyByteBuf buf, Level level) {
                setter.accept(codec.decode(buf));
            }
        });
    }

    public <T> void field(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Function<Level, T> getter, BiConsumer<Level, T> setter) {
        entries.add(new SyncEntry() {
            @Override
            public void encode(RegistryFriendlyByteBuf buf, Level level) {
                codec.encode(buf, getter.apply(level));
            }

            @Override
            public void decode(RegistryFriendlyByteBuf buf, Level level) {
                setter.accept(level, codec.decode(buf));
            }
        });
    }

    public <T> void field(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Supplier<T> getter, BiConsumer<Level, T> setter) {
        entries.add(new SyncEntry() {
            @Override
            public void encode(RegistryFriendlyByteBuf buf, Level level) {
                codec.encode(buf, getter.get());
            }

            @Override
            public void decode(RegistryFriendlyByteBuf buf, Level level) {
                setter.accept(level, codec.decode(buf));
            }
        });
    }

    public <T> void field(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Function<Level, T> getter, Consumer<T> setter) {
        entries.add(new SyncEntry() {
            @Override
            public void encode(RegistryFriendlyByteBuf buf, Level level) {
                codec.encode(buf, getter.apply(level));
            }

            @Override
            public void decode(RegistryFriendlyByteBuf buf, Level level) {
                setter.accept(codec.decode(buf));
            }
        });
    }

    public void encode(RegistryFriendlyByteBuf buf, Level level) {
        for (SyncEntry entry : entries) {
            entry.encode(buf, level);
        }
    }

    public void decode(RegistryFriendlyByteBuf buf, Level level) {
        for (SyncEntry entry : entries) {
            entry.decode(buf, level);
        }
    }
}
