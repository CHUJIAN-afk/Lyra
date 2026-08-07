package first.lyra.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 地面方块判定工具，用于地面寻路的节点可行性检查。
 * <p>
 * 复用 TargetCache 的 chunk 访问思路，但寻路节点数有限（上限256），
 * 直接用 {@link ServerLevel#getBlockState} 即可，性能足够。
 * </p>
 */
public final class GroundPathHelper {

    private GroundPathHelper() {
    }

    /**
     * 方块是否为固体支撑（可站立其上）。
     * 判定：碰撞箱非空且非空气。
     */
    public static boolean isSolid(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    /**
     * 方块是否可穿过（非固体，实体可进入该格）。
     * 用于判定行走节点的身体所占格是否畅通。
     */
    public static boolean isPassable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        return state.getCollisionShape(level, pos).isEmpty();
    }

    /**
     * 节点是否可行走：脚部格与头部格可穿过，且下方一格为固体支撑。
     * <p>
     * 假设实体高度约2格（脚在 pos，头在 pos.above()）。
     * </p>
     *
     * @param level 世界
     * @param pos   脚部位置（实体将站立于此格）
     * @return true 表示可在此格站立行走
     */
    public static boolean isWalkable(ServerLevel level, BlockPos pos) {
        if (!isPassable(level, pos)) {
            return false;
        }
        if (!isPassable(level, pos.above())) {
            return false;
        }
        return isSolid(level, pos.below());
    }

    /**
     * 节点是否可行走，但不强制要求下方支撑（用于跳跃/落地判定）。
     */
    public static boolean isWalkableNoGround(ServerLevel level, BlockPos pos) {
        if (!isPassable(level, pos)) {
            return false;
        }
        return isPassable(level, pos.above());
    }

    /**
     * 是否可从 from 跳到 to（to 与 from 同层或高1格，且 to 可行走）。
     * <p>
     * 跳跃判定：目标格可行走，且 from 上方无阻挡（能起跳），
     * 目标与起点高度差 ≤ 1（可跳过1格高障碍）。
     * </p>
     */
    public static boolean canJumpTo(ServerLevel level, BlockPos from, BlockPos to) {
        // 起跳点上方必须畅通
        if (!isPassable(level, from.above())) {
            return false;
        }
        // 目标可行走
        if (!isWalkable(level, to)) {
            return false;
        }
        return true;
    }
}
