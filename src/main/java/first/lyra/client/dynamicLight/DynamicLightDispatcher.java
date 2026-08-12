package first.lyra.client.dynamicLight;

import first.lyra.common.entity.PathNode;
import first.lyra.client.config.ClientConfig;
import net.minecraft.client.Minecraft;
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

    /**
     * 26.2: LevelRenderer.setSectionDirty 移入 {@link net.minecraft.client.renderer.extract.LevelExtractor},
     * 通过 Minecraft.levelExtractor.setSectionDirty 触发区块重编译。
     */
    public static void update() {
        // 本帧光源所在区块（含 7 邻居扩展）
        Set<Long> currentSectionSet = new HashSet<>();
        LightSources.forEach((lightPos, luminance) -> {
            SectionPos sectionPos = SectionPos.of(lightPos);
            currentSectionSet.add(sectionPos.asLong());
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
                currentSectionSet.add(SectionPos.asLong(cx, cy, cz));
            }
        });
        // 刷新上一帧光源区块（光源移走后恢复原光照）+ 本帧光源区块（点亮）
        Set<Long> updateSectionSet = new HashSet<>(LastUpdateSectionSet);
        updateSectionSet.addAll(currentSectionSet);
        SnapshotLightSources = new HashMap<>(LightSources);
        LightSources.clear();
        updateSectionSet.forEach(key -> Minecraft.getInstance().levelExtractor.setSectionDirty(SectionPos.x(key), SectionPos.y(key), SectionPos.z(key)));
        // 下一帧的"上一帧区块" = 本帧光源所在区块（不能累积历史）
        LastUpdateSectionSet.clear();
        LastUpdateSectionSet.addAll(currentSectionSet);
    }

    // ==================== 方块路径（BlockPos 级，GPU 顶点插值处理平滑） ====================

    /**
     * 方块路径（26.2 重构）:由 {@code BlockModelLighterMixin} 在区块编译阶段调用,
     * 返回提升后的 packed light。
     */
    public static int getDynamicLight(BlockAndLightGetter level, BlockState state, BlockPos blockPos, int originalLight) {
        Map<Vec3, Integer> lights = SnapshotLightSources;
        if (!lights.isEmpty() && !state.isSolidRender()) {
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
