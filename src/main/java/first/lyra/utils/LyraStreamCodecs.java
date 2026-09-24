package first.lyra.utils;

import first.lyra.common.attachmentEntity.PathNode;
import first.lyra.common.minion.MinionSlotType;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public interface LyraStreamCodecs extends ByteBufCodecs {
    StreamCodec<RegistryFriendlyByteBuf, MinionSlotType> MINION_SLOT_TYPE = ByteBufCodecs.STRING_UTF8.map(MinionSlotType::valueOf, MinionSlotType::name).cast();
    StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC_3 = StreamCodec.composite(ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);
    StreamCodec<RegistryFriendlyByteBuf, PathNode> PATH_NODE = StreamCodec.composite(LyraStreamCodecs.VEC_3, PathNode::pos, ByteBufCodecs.FLOAT, PathNode::yaw, ByteBufCodecs.FLOAT, PathNode::pitch, ByteBufCodecs.FLOAT, PathNode::roll, PathNode::new);
    StreamCodec<RegistryFriendlyByteBuf, Optional<UUID>> OPTIONAL_UUID = optional(UUIDUtil.STREAM_CODEC);

    static <T> StreamCodec<RegistryFriendlyByteBuf, Optional<T>> optional(StreamCodec<ByteBuf, T> streamCodec) {
        return new StreamCodec<>() {
            @Override
            public void encode(@NotNull RegistryFriendlyByteBuf buf, @NotNull Optional<T> optional) {
                buf.writeBoolean(optional.isPresent());
                optional.ifPresent(value -> streamCodec.encode(buf, value));
            }

            @Override
            public @NotNull Optional<T> decode(@NotNull RegistryFriendlyByteBuf buf) {
                return buf.readBoolean() ? Optional.of(streamCodec.decode(buf)) : Optional.empty();
            }
        };
    }
}
