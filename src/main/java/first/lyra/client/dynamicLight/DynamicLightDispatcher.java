package first.lyra.client.dynamicLight;

import first.lyra.mixin.LevelRendererAccessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class DynamicLightDispatcher {

    public static final DynamicLightDispatcher INSTANCE = new DynamicLightDispatcher();

    private static final double MAX_RADIUS = 7.75;
    private static final double MAX_RADIUS_SQUARED = MAX_RADIUS * MAX_RADIUS;
    private static final double FALLOFF = 15.0 / MAX_RADIUS;
    private static final int SECTION_BITS = 4;
    private static final double SMOOTH_SCALE = 16.0;
    private static final int SMOOTH_BIAS = 8;

    private final Object2ObjectOpenHashMap<SectionPos, Object2ObjectOpenHashMap<LightSource, AtomicInteger>> lightSources = new Object2ObjectOpenHashMap<>();
    private volatile Object2ObjectOpenHashMap<SectionPos, LightSource[]> snapshot = new Object2ObjectOpenHashMap<>();

    private DynamicLightDispatcher() {
    }

    public void addLightSource(LightSource source) {
        if (source.luminance() > 0) {
            Object2ObjectOpenHashMap<LightSource, AtomicInteger> data = lightSources.computeIfAbsent(SectionPos.of(source.position()), k -> new Object2ObjectOpenHashMap<>());
            data.put(source, new AtomicInteger(data.containsKey(source) ? 1 : 2));
        }
    }

    public void update(LevelRendererAccessor levelRenderer) {
        Set<SectionPos> dirtyChunks = new HashSet<>();
        Object2ObjectOpenHashMap<SectionPos, LightSource[]> snapshot = new Object2ObjectOpenHashMap<>(lightSources.size() * 2);
        if (!lightSources.isEmpty()) {
            lightSources.entrySet().removeIf(entry -> {
                Object2ObjectOpenHashMap<LightSource, AtomicInteger> data = entry.getValue();
                data.entrySet().removeIf(integerEntry -> {
                    LightSource lightSource = integerEntry.getKey();
                    AtomicInteger atomicInteger = integerEntry.getValue();
                    int integer = atomicInteger.addAndGet(-1);
                    if (integer != 0) {
                        if (integer == 1) {
                            atomicInteger.addAndGet(-1);
                        }
                        dirtyChunks.addAll(getImpactSectionPos(lightSource));
                    }
                    return atomicInteger.get() < 0;
                });
                snapshot.put(entry.getKey(), data.keySet().toArray(new LightSource[0]));
                return data.isEmpty();
            });
        }
        if (!dirtyChunks.isEmpty()) {
            for (SectionPos sectionPos : dirtyChunks) {
                levelRenderer.callSetSectionDirty(sectionPos.x(), sectionPos.y(), sectionPos.z(), false);
            }
        }
        this.snapshot = snapshot;
    }

    public List<SectionPos> getImpactSectionPos(LightSource lightSource) {
        Vec3 position = lightSource.position();
        double radius = Mth.clamp(lightSource.luminance(), 0, 15) / FALLOFF;
        int minX = SectionPos.blockToSectionCoord(position.x() - radius);
        int maxX = SectionPos.blockToSectionCoord(position.x() + radius);
        int minY = SectionPos.blockToSectionCoord(position.y() - radius);
        int maxY = SectionPos.blockToSectionCoord(position.y() + radius);
        int minZ = SectionPos.blockToSectionCoord(position.z() - radius);
        int maxZ = SectionPos.blockToSectionCoord(position.z() + radius);
        List<SectionPos> sections = new ArrayList<>((maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1));
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cy = minY; cy <= maxY; cy++) {
                for (int cz = minZ; cz <= maxZ; cz++) {
                    sections.add(SectionPos.of(cx, cy, cz));
                }
            }
        }
        return sections;
    }

    public int getDynamicLight(BlockAndTintGetter level, BlockState state, BlockPos blockPos, int originalLight) {
        int light = originalLight;
        if (!state.isSolidRender(level, blockPos)) {
            double dynamicLight = getDynamicLightLevel(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
            if (dynamicLight > LightTexture.block(originalLight)) {
                light = withDynamicLight(originalLight, dynamicLight);
            }
        }
        return light;
    }

    public int getDynamicLight(Vec3 eyePos, int originalLight) {
        int light = originalLight;
        double dynamicLight = getDynamicLightLevel(eyePos.x, eyePos.y, eyePos.z);
        if (dynamicLight > LightTexture.block(originalLight)) {
            light = withDynamicLight(originalLight, dynamicLight);
        }
        return light;
    }

    private double getDynamicLightLevel(double x, double y, double z) {
        Object2ObjectOpenHashMap<SectionPos, LightSource[]> sources = snapshot;
        if (sources.isEmpty()) {
            return 0;
        }
        double result = 0;
        int fx = Mth.floor(x), fy = Mth.floor(y), fz = Mth.floor(z);
        int cx = fx >> SECTION_BITS, cy = fy >> SECTION_BITS, cz = fz >> SECTION_BITS;
        int ox = fx & 15, oy = fy & 15, oz = fz & 15;
        int x0 = ox <= 6 ? cx - 1 : cx;
        int x1 = ox >= 9 ? cx + 1 : cx;
        int y0 = oy <= 6 ? cy - 1 : cy;
        int y1 = oy >= 9 ? cy + 1 : cy;
        int z0 = oz <= 6 ? cz - 1 : cz;
        int z1 = oz >= 9 ? cz + 1 : cz;
        for (int bx = x0; bx <= x1; bx++) {
            for (int by = y0; by <= y1; by++) {
                for (int bz = z0; bz <= z1; bz++) {
                    LightSource[] group = sources.get(SectionPos.of(bx, by, bz));
                    if (group != null) {
                        for (LightSource lightSource : group) {
                            Vec3 pos = lightSource.position();
                            double dx = x - pos.x;
                            double dy = y - pos.y;
                            double dz = z - pos.z;
                            double distSq = dx * dx + dy * dy + dz * dz;
                            if (distSq <= MAX_RADIUS_SQUARED) {
                                double light = lightSource.luminance() - Math.sqrt(distSq) * FALLOFF;
                                if (light > result) {
                                    result = light;
                                }
                            }
                        }
                    }
                }
            }
        }
        return Mth.clamp(result, 0.0, 15.0);
    }

    private static int withDynamicLight(int originalLight, double dynamicLight) {
        int smooth = (int) (dynamicLight * SMOOTH_SCALE) + SMOOTH_BIAS;
        return (originalLight & 0xfff00000) | smooth;
    }

    public record LightSource(Vec3 position, int luminance) {

        @Override
        public int hashCode() {
            double x = position.x();
            double y = position.y();
            double z = position.z();
            long h = Double.doubleToRawLongBits(x) * 0x9E3779B97F4A7C15L ^ Double.doubleToRawLongBits(y) * 0xBF58476D1CE4E5B9L ^ Double.doubleToRawLongBits(z) * 0x94D049BB133111EBL ^ Double.doubleToRawLongBits(luminance) * 0x27D4EB2F165667C5L;
            h ^= h >>> 32;
            return Long.hashCode(h);
        }
    }
}