package first.lyra.utils;

import first.lyra.common.entity.PathNode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public interface LyraStreamCodecs extends ByteBufCodecs {
    StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC_3 = StreamCodec.composite(ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);
    StreamCodec<RegistryFriendlyByteBuf, PathNode> PATH_NODE = StreamCodec.composite(LyraStreamCodecs.VEC_3, PathNode::pos, ByteBufCodecs.FLOAT, PathNode::yaw, ByteBufCodecs.FLOAT, PathNode::pitch, ByteBufCodecs.FLOAT, PathNode::roll, PathNode::new);
}
