package first.lyra.utils;

import first.lyra.common.attachmentEntity.PathNode;
import first.lyra.common.minion.MinionSlotType;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.mesdag.portlib.network.PortRegistryFriendlyByteBuf;
import org.mesdag.portlib.network.codec.PortByteBufCodecs;
import org.mesdag.portlib.network.codec.PortStreamCodec;

import java.util.Optional;
import java.util.UUID;

public interface LyraStreamCodecs extends PortByteBufCodecs {
    PortStreamCodec<ByteBuf, MinionSlotType> MINION_SLOT_TYPE = PortByteBufCodecs.STRING_UTF8.map(MinionSlotType::valueOf, MinionSlotType::name);
    PortStreamCodec<ByteBuf, Vec3> VEC_3 = PortStreamCodec.composite(PortByteBufCodecs.DOUBLE, Vec3::x, PortByteBufCodecs.DOUBLE, Vec3::y, PortByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);
    PortStreamCodec<ByteBuf, PathNode> PATH_NODE = PortStreamCodec.composite(LyraStreamCodecs.VEC_3, PathNode::pos, PortByteBufCodecs.FLOAT, PathNode::yaw, PortByteBufCodecs.FLOAT, PathNode::pitch, PortByteBufCodecs.FLOAT, PathNode::roll, PathNode::new);
    PortStreamCodec<PortRegistryFriendlyByteBuf, Optional<UUID>> OPTIONAL_UUID = PortStreamCodec.ofMember(
            (optionalUUID, buf) -> {
                buf.writeBoolean(optionalUUID.isPresent());
                optionalUUID.ifPresent(buf::writeUUID);
            },
            buf -> buf.readBoolean() ? Optional.of(buf.readUUID()) : Optional.empty()
    );

    static <T> PortStreamCodec<PortRegistryFriendlyByteBuf, Optional<T>> optional(PortStreamCodec<ByteBuf, T> streamCodec) {
        return new PortStreamCodec<>() {
            @Override
            public void encode(@NotNull PortRegistryFriendlyByteBuf buf, @NotNull Optional<T> optional) {
                buf.writeBoolean(optional.isPresent());
                optional.ifPresent(value -> streamCodec.encode(buf, value));
            }

            @Override
            public @NotNull Optional<T> decode(@NotNull PortRegistryFriendlyByteBuf buf) {
                return buf.readBoolean() ? Optional.of(streamCodec.decode(buf)) : Optional.empty();
            }
        };
    }
}
