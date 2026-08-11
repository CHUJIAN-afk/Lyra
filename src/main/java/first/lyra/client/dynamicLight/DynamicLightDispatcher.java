package first.lyra.client.dynamicLight;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import first.lyra.common.entity.PathNode;
import first.lyra.client.config.ClientConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 动态光照调度器。
 * <p>
 * 双路径架构：
 * - 方块路径：通过 BlockPos 级 packed light 修改（LevelRendererMixin），GPU 顶点插值实现跨方块平滑
 * - 实体路径：直接用 Vec3 精确位置计算光照，天然无跳变
 */
public class DynamicLightDispatcher {

    private static final double MAX_RADIUS = 7.75;
    private static final Map<Vec3, Integer> LightSources = new HashMap<>();
    private static final Set<Long> LastUpdateSectionSet = new HashSet<>();
    private static volatile Map<Vec3, Integer> SnapshotLightSources = new HashMap<>();

    public static void addLightSources(PathNode pathNode, AABB aabb, int light) {
        if (ClientConfig.DynamicLight.isTrue()) {
            Vec3 pos = pathNode.pos();
            double minX = aabb.minX, maxX = aabb.maxX;
            double minY = aabb.minY, maxY = aabb.maxY;
            double minZ = aabb.minZ, maxZ = aabb.maxZ;

            // Z轴方向最长，沿Z轴等间隔放置点光源（间隔≤0.5格）
            double zLen = maxZ - minZ;
            int count = Math.max(1, Mth.ceil(zLen / 0.5));
            double step = zLen / count;

            // 局部→世界旋转：先绕Y旋转yaw，再绕X旋转pitch，再绕Z旋转roll
            float yawRad = (float) Math.toRadians(-pathNode.yaw());
            float pitchRad = (float) Math.toRadians(pathNode.pitch());
            double cosY = Math.cos(yawRad), sinY = Math.sin(yawRad);
            double cosP = Math.cos(pitchRad), sinP = Math.sin(pitchRad);

            for (int i = 0; i <= count; i++) {
                double lz = minZ + step * i;

                double lx = (minX + maxX) * 0.5;
                double ly = (minY + maxY) * 0.5;

                double rry = ly * cosP - lz * sinP;
                double rrz = ly * sinP + lz * cosP;

                double wwx = lx * cosY + rrz * sinY;
                double wwz = -lx * sinY + rrz * cosY;

                LightSources.put(pos.add(wwx, rry, wwz), light);
            }
        }
    }

    public static void addLightSources(Vec3 pos, int light) {
        if (ClientConfig.DynamicLight.isTrue()) {
            LightSources.put(pos, light);
        }
    }

    // 26.2: LevelRenderer.setSectionDirty 已移除(区块渲染管线重构),不再触发区块重编译;
    // 光照快照更新保留,实体路径(getDynamicLight(Vec3, int))不受影响。
    public static void update() {
        Set<Long> updateSectionSet = new HashSet<>(LastUpdateSectionSet);
        LastUpdateSectionSet.clear();
        LightSources.forEach((lightPos, luminance) -> {
            SectionPos sectionPos = SectionPos.of(lightPos);
            updateSectionSet.add(sectionPos.asLong());
            Direction dirX = (Mth.floor(lightPos.x) & 15) >= 8 ? Direction.EAST : Direction.WEST;
            Direction dirY = (Mth.floor(lightPos.y) & 15) >= 8 ? Direction.UP : Direction.DOWN;
            Direction dirZ = (Mth.floor(lightPos.z) & 15) >= 8 ? Direction.SOUTH : Direction.NORTH;
            int cx = sectionPos.x(), cy = sectionPos.y(), cz = sectionPos.z();
            for (int i = 0; i < 7; i++) {
                switch (i % 4) {
                    case 0 -> cx += dirX.getStepX();
                    case 1 -> cz += dirZ.getStepZ();
                    case 2 -> cx -= dirX.getStepX();
                    case 3 -> {
                        cz -= dirZ.getStepZ();
                        cy += dirY.getStepY();
                    }
                }
                updateSectionSet.add(SectionPos.asLong(cx, cy, cz));
            }
        });
        SnapshotLightSources = new HashMap<>(LightSources);
        LightSources.clear();
        LastUpdateSectionSet.addAll(updateSectionSet);
    }

    // ==================== 方块路径（BlockPos 级，GPU 顶点插值处理平滑） ====================

    public static int getDynamicLight(BlockAndLightGetter level, BlockState state, BlockPos blockPos, Operation<Integer> original) {
        Map<Vec3, Integer> lights = SnapshotLightSources;
        int originalLight = original.call(level, state, blockPos);
        if (!lights.isEmpty() && !level.getBlockState(blockPos).isSolidRender()) {
            double maxLight = computeRawBlockLightAtBlockPos(blockPos);
            if (maxLight > 0) {
                int blockLevel = LightCoordsUtil.block(originalLight);
                if (maxLight > blockLevel) {
                    int newBlockLight = Mth.clamp((int) Math.round(maxLight), 0, 15);
                    return LightCoordsUtil.pack(newBlockLight, LightCoordsUtil.sky(originalLight));
                }
            }
        }
        return originalLight;
    }

    // ==================== 实体路径（Vec3 精确计算，天然无跳变） ====================

    /**
     * 使用实体眼睛的精确 Vec3 位置计算动态光照。
     * <p>
     * 直接用连续坐标计算到光源的距离，无需截断为 BlockPos，
     * 光照值随实体移动连续变化，天然消除跨方块跳变。
     *
     * @param eyePos       实体眼睛的连续世界坐标
     * @param originalLight vanilla 原始 packed light
     * @return 修改后的 packed light（仅 block-light 可能被提升，sky-light 保持不变）
     */
    public static int getDynamicLight(Vec3 eyePos, int originalLight) {
        Map<Vec3, Integer> lights = SnapshotLightSources;
        if (lights.isEmpty()) return originalLight;

        double maxLight = 0;
        for (Map.Entry<Vec3, Integer> entry : lights.entrySet()) {
            Vec3 pos = entry.getKey();
            int luminance = entry.getValue();
            double dx = eyePos.x - pos.x;
            double dy = eyePos.y - pos.y;
            double dz = eyePos.z - pos.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq <= MAX_RADIUS * MAX_RADIUS) {
                double contribution = luminance - Math.sqrt(distSq) / MAX_RADIUS * 15.0;
                if (contribution > maxLight) {
                    maxLight = contribution;
                }
            }
        }
        if (maxLight > 0) {
            int blockLevel = LightCoordsUtil.block(originalLight);
            if (maxLight > blockLevel) {
                int newBlockLight = Mth.clamp((int) Math.round(maxLight), 0, 15);
                return LightCoordsUtil.pack(newBlockLight, LightCoordsUtil.sky(originalLight));
            }
        }
        return originalLight;
    }

    // ==================== 核心计算 ====================

    /**
     * 计算指定 BlockPos 处的动态光照贡献（方块中心采样，供方块路径使用）。
     */
    private static double computeRawBlockLightAtBlockPos(BlockPos blockPos) {
        Map<Vec3, Integer> lights = SnapshotLightSources;
        if (lights.isEmpty()) return 0.0;

        double maxLight = 0;
        for (Map.Entry<Vec3, Integer> entry : lights.entrySet()) {
            Vec3 pos = entry.getKey();
            int luminance = entry.getValue();
            double dx = blockPos.getX() - pos.x + 0.5;
            double dy = blockPos.getY() - pos.y + 0.5;
            double dz = blockPos.getZ() - pos.z + 0.5;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq <= MAX_RADIUS * MAX_RADIUS) {
                double contribution = luminance - Math.sqrt(distSq) / MAX_RADIUS * 15.0;
                if (contribution > maxLight) {
                    maxLight = contribution;
                }
            }
        }
        return maxLight;
    }
}
