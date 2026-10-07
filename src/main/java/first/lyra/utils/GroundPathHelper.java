package first.lyra.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class GroundPathHelper {

    private GroundPathHelper() {
    }

    public static boolean isSolid(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    public static boolean isPassable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        return state.getCollisionShape(level, pos).isEmpty();
    }

    public static boolean isWalkable(ServerLevel level, BlockPos pos) {
        if (!isPassable(level, pos)) {
            return false;
        }
        if (!isPassable(level, pos.above())) {
            return false;
        }
        return isSolid(level, pos.below());
    }

    public static boolean isWalkableNoGround(ServerLevel level, BlockPos pos) {
        if (!isPassable(level, pos)) {
            return false;
        }
        return isPassable(level, pos.above());
    }

    public static boolean canJumpTo(ServerLevel level, BlockPos from, BlockPos to) {

        if (!isPassable(level, from.above())) {
            return false;
        }

        if (!isWalkable(level, to)) {
            return false;
        }
        return true;
    }
}
