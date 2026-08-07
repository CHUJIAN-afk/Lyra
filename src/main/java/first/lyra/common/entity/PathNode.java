package first.lyra.common.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public record PathNode(Vec3 pos, float yaw, float pitch, float roll) {

    public PathNode lerp(PathNode to, float partialTick) {
        return new PathNode(
                this.pos().lerp(to.pos(), partialTick),
                Mth.rotLerp(partialTick, this.yaw(), to.yaw()),
                Mth.rotLerp(partialTick, this.pitch(), to.pitch()),
                Mth.rotLerp(partialTick, this.roll(), to.roll())
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof PathNode(Vec3 pos1, float yaw1, float pitch1, float roll1)) {
            return pos1.equals(this.pos()) && yaw1 == this.yaw() && pitch1 == this.pitch() && roll1 == this.roll();
        }
        return false;
    }
}