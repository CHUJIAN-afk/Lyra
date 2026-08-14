package first.lyra.common.minion;

import first.lyra.utils.GroundPathHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * A* 寻路器（不依赖 vanilla Mob）。
 * <p>
 * 在 {@link ServerLevel} 上做方块级 A* 搜索，支持水平/对角线移动与1格高跳跃。
 * 返回方块坐标序列（起点→终点），空列表表示不可达。
 * </p>
 */
public class PathNavigator {

    /** 单次搜索最大扩展节点数，防止卡顿。 */
    private static final int MAX_NODES = 512;
    /** 目标容差：到达目标 1 格内即视为成功。 */
    private static final double GOAL_TOLERANCE_SQ = 1.0;

    /** 8 方向邻居偏移（X/Z 平面）。 */
    private static final int[][] NEIGHBORS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    /**
     * 寻路。
     *
     * @param level    世界
     * @param startVec 起点世界坐标
     * @param targetVec 目标世界坐标
     * @param maxRange  最大搜索半径（格）
     * @return 方块坐标列表（含起点，终点为最后一项）；不可达返回空列表
     */
    public List<BlockPos> findPath(ServerLevel level, Vec3 startVec, Vec3 targetVec, float maxRange) {
        BlockPos start = BlockPos.containing(startVec.x, startVec.y, startVec.z);
        BlockPos target = BlockPos.containing(targetVec.x, targetVec.y, targetVec.z);

        // 起点修正：若当前脚部不可行走，向下找最近支撑
        start = findGroundBelow(level, start);
        target = findGroundBelow(level, target);

        if (start.equals(target)) {
            return List.of(start);
        }
        if (!GroundPathHelper.isWalkable(level, target)) {
            // 目标不可站立，仍尝试靠近（容差判定）
        }

        double rangeSq = (double) maxRange * maxRange;

        PriorityQueue<Node> open = new PriorityQueue<>();
        Map<BlockPos, Node> nodeMap = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();

        Node startNode = new Node(start, 0, heuristic(start, target), null);
        open.add(startNode);
        nodeMap.put(start, startNode);

        int expanded = 0;
        while (!open.isEmpty() && expanded < MAX_NODES) {
            Node current = open.poll();
            if (current.pos.equals(target) || distSq(current.pos, target) <= GOAL_TOLERANCE_SQ) {
                return reconstruct(current);
            }
            if (closed.contains(current.pos)) {
                continue;
            }
            closed.add(current.pos);
            expanded++;

            for (int[] off : NEIGHBORS) {
                BlockPos neighbor = current.pos.offset(off[0], 0, off[1]);

                // 超出搜索范围
                if (distSq(neighbor, start) > rangeSq && distSq(neighbor, target) > rangeSq) {
                    continue;
                }
                if (closed.contains(neighbor)) {
                    continue;
                }

                // 尝试同层移动；若不可行走则尝试跳1格高
                if (GroundPathHelper.isWalkable(level, neighbor)) {
                    // 对角线需两侧畅通
                    if (off[0] != 0 && off[1] != 0) {
                        if (!GroundPathHelper.isPassable(level, current.pos.offset(off[0], 0, 0))
                                || !GroundPathHelper.isPassable(level, current.pos.offset(0, 0, off[1]))) {
                            continue;
                        }
                    }
                    addNeighbor(open, nodeMap, current, neighbor, target, off[0] != 0 && off[1] != 0 ? 1.414 : 1.0);
                } else {
                    // 同层不可行走 → 尝试向上一格跳跃（越过1格高障碍）
                    BlockPos jumpTarget = neighbor.above();
                    if (GroundPathHelper.canJumpTo(level, current.pos, jumpTarget)) {
                        addNeighbor(open, nodeMap, current, jumpTarget, target, off[0] != 0 && off[1] != 0 ? 1.732 : 1.414);
                    }
                    // 同层不可行走 → 尝试向下一格（下台阶/落差）
                    BlockPos dropTarget = neighbor.below();
                    if (GroundPathHelper.isWalkable(level, dropTarget)
                            && GroundPathHelper.isPassable(level, neighbor)) {
                        addNeighbor(open, nodeMap, current, dropTarget, target, off[0] != 0 && off[1] != 0 ? 1.732 : 1.414);
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private void addNeighbor(PriorityQueue<Node> open, Map<BlockPos, Node> nodeMap,
                             Node current, BlockPos neighbor, BlockPos target, double stepCost) {
        double g = current.g + stepCost;
        Node existing = nodeMap.get(neighbor);
        if (existing == null || g < existing.g) {
            Node n = new Node(neighbor, g, heuristic(neighbor, target), current);
            nodeMap.put(neighbor, n);
            open.add(n);
        }
    }

    private List<BlockPos> reconstruct(Node end) {
        List<BlockPos> path = new ArrayList<>();
        Node n = end;
        while (n != null) {
            path.add(n.pos);
            n = n.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private double heuristic(BlockPos a, BlockPos b) {
        // 欧氏距离启发
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private double distSq(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * 从 pos 向下寻找最近的可站立地面（脚部位置）。
     */
    private BlockPos findGroundBelow(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < 4; i++) {
            if (GroundPathHelper.isWalkable(level, pos)) {
                return pos;
            }
            pos = pos.below();
        }
        // 向上找
        pos = BlockPos.containing(pos.getX(), pos.getY() + 4, pos.getZ());
        for (int i = 0; i < 4; i++) {
            if (GroundPathHelper.isWalkable(level, pos)) {
                return pos;
            }
            pos = pos.below();
        }
        return pos;
    }

    /** A* 节点。 */
    private static final class Node implements Comparable<Node> {
        final BlockPos pos;
        final double g;
        final double h;
        final Node parent;

        Node(BlockPos pos, double g, double h, Node parent) {
            this.pos = pos;
            this.g = g;
            this.h = h;
            this.parent = parent;
        }

        double f() {
            return g + h;
        }

        @Override
        public int compareTo(Node o) {
            return Double.compare(this.f(), o.f());
        }
    }
}
