package first.lyra.common.entity;

import net.minecraft.world.phys.Vec3;

public interface ICollision {

    default PathNode getCollisionAfterPathNode(Vec3 endPos, PathNode beforeNode) {
        return new PathNode(endPos, beforeNode.yaw(), beforeNode.pitch(), beforeNode.roll());
    }
}
