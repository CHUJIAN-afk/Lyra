package first.lyra.client.dynamicLight;

import first.lyra.mixin.LevelRendererAccessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
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

    public static final DynamicLightDispatcher INSTANCE = new DynamicLightDispatcher(7.75);

    private final Object2ObjectOpenHashMap<SectionPos, Object2ObjectLinkedOpenHashMap<LightSource, AtomicInteger>> lightSources;
    private final double maxRadiusSquared;
    private final double falloff;
    private volatile Object2ObjectOpenHashMap<SectionPos, LightSource[]> snapshot;

    private DynamicLightDispatcher(double maxRadius) {
        this.lightSources = new Object2ObjectOpenHashMap<>();
        this.maxRadiusSquared = maxRadius * maxRadius;
        this.falloff = 15.0 / maxRadius;
        this.snapshot = new Object2ObjectOpenHashMap<>();
    }

    public void addLightSource(LightSource source) {
        if (source.luminance() > 0) {
            SectionPos section = SectionPos.of(source.position());
            Object2ObjectLinkedOpenHashMap<LightSource, AtomicInteger> data = lightSources.computeIfAbsent(section, k -> new Object2ObjectLinkedOpenHashMap<>());
            AtomicInteger counter = data.get(source);
            if (counter == null) {
                data.put(source, new AtomicInteger(2));
            } else {
                counter.set(1);
            }
        }
    }

    public void update(LevelRendererAccessor levelRenderer) {
        Set<SectionPos> dirtyChunks = new HashSet<>();
        Object2ObjectOpenHashMap<SectionPos, LightSource[]> snapshot = new Object2ObjectOpenHashMap<>(lightSources.size() * 2);
        if (!lightSources.isEmpty()) {
            lightSources.entrySet().removeIf(entry -> {
                Object2ObjectLinkedOpenHashMap<LightSource, AtomicInteger> data = entry.getValue();
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
        double radius = Mth.clamp(lightSource.luminance(), 0, 15) / falloff;
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
        int cx = fx >> 4, cy = fy >> 4, cz = fz >> 4;
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
                            if (distSq <= maxRadiusSquared) {
                                double light = lightSource.luminance() - Math.sqrt(distSq) * falloff;
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
        int smooth = (int) (dynamicLight * 16.0) + 8;
        return (originalLight & 0xfff00000) | smooth;
    }

    public static final class LightSource {

        private final Vec3 position;
        private final int luminance;
        private final int hash;

        public LightSource(Vec3 position, int luminance) {
            this.position = position;
            this.luminance = luminance;
            long xb = Double.doubleToLongBits(position.x());
            long yb = Double.doubleToLongBits(position.y());
            long zb = Double.doubleToLongBits(position.z());
            long h = xb * 0x9E3779B97F4A7C15L ^ yb * 0xBF58476D1CE4E5B9L ^ zb * 0x94D049BB133111EBL ^ (long) luminance * 0x27D4EB2F165667C5L;
            this.hash = Long.hashCode(h);
        }

        public Vec3 position() {
            return position;
        }

        public int luminance() {
            return luminance;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof LightSource other) {
                return luminance == other.luminance && position.equals(other.position);
            }
            return false;
        }

        @Override
        public String toString() {
            return "LightSource[position=" + position + ", luminance=" + luminance + "]";
        }
    }
}