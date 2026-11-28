package first.lyra.common.attachment;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class LineOfSight {
    private final Long2ObjectOpenHashMap<LevelChunkSection> sections = new Long2ObjectOpenHashMap<>();
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private final SectionLookup sectionLookup;
    private BlockGetter blocks;

    LineOfSight(SectionLookup sectionLookup) {
        this.sectionLookup = sectionLookup;
    }

    void reset(BlockGetter blocks) {
        this.blocks = blocks;
        sections.clear();
    }

    boolean hasLineOfSight(Vec3 from, Vec3 to) {
        if (!(from.distanceToSqr(to) <= 128.0 * 128.0)) {
            return false;
        }

        double startX = Mth.lerp(-1.0E-7, to.x, from.x);
        double startY = Mth.lerp(-1.0E-7, to.y, from.y);
        double startZ = Mth.lerp(-1.0E-7, to.z, from.z);
        double dx = Mth.lerp(-1.0E-7, from.x, to.x) - startX;
        double dy = Mth.lerp(-1.0E-7, from.y, to.y) - startY;
        double dz = Mth.lerp(-1.0E-7, from.z, to.z) - startZ;
        int x = Mth.floor(startX);
        int y = Mth.floor(startY);
        int z = Mth.floor(startZ);
        int stepX = Mth.sign(dx);
        int stepY = Mth.sign(dy);
        int stepZ = Mth.sign(dz);
        double deltaX = stepX == 0 ? 0 : stepX / dx;
        double deltaY = stepY == 0 ? 0 : stepY / dy;
        double deltaZ = stepZ == 0 ? 0 : stepZ / dz;
        double nextX = stepX == 0 ? Double.POSITIVE_INFINITY : deltaX * (stepX > 0 ? 1 - Mth.frac(startX) : Mth.frac(startX));
        double nextY = stepY == 0 ? Double.POSITIVE_INFINITY : deltaY * (stepY > 0 ? 1 - Mth.frac(startY) : Mth.frac(startY));
        double nextZ = stepZ == 0 ? Double.POSITIVE_INFINITY : deltaZ * (stepZ > 0 ? 1 - Mth.frac(startZ) : Mth.frac(startZ));

        int entryX = stepX > 0 ? 0 : 15;
        int entryY = stepY > 0 ? 0 : 15;
        int entryZ = stepZ > 0 ? 0 : 15;
        boolean findSection = true;
        LevelChunkSection section = null;
        while (true) {
            if (nextX < nextY && nextX < nextZ) {
                if (nextX > 1) {
                    return true;
                }
                x += stepX;
                nextX += deltaX;
                findSection |= (x & 15) == entryX;
            } else if (nextY < nextZ) {
                if (nextY > 1) {
                    return true;
                }
                y += stepY;
                nextY += deltaY;
                findSection |= (y & 15) == entryY;
            } else {
                if (nextZ > 1) {
                    return true;
                }
                z += stepZ;
                nextZ += deltaZ;
                findSection |= (z & 15) == entryZ;
            }

            if (findSection) {
                findSection = false;
                section = null;
                if (!blocks.isOutsideBuildHeight(y)) {
                    int sectionX = x >> 4;
                    int sectionY = y >> 4;
                    int sectionZ = z >> 4;
                    long key = SectionPos.asLong(sectionX, sectionY, sectionZ);
                    section = sections.get(key);
                    if (section == null && !sections.containsKey(key)) {
                        section = sectionLookup.get(sectionX, sectionY, sectionZ);
                        sections.put(key, section);
                    }
                    if (section == null) {
                        return false;
                    }
                    if (section.hasOnlyAir()) {
                        section = null;
                    }
                }

                if (section == null) {
                    int insideX = stepX > 0 ? 15 - (x & 15) : x & 15;
                    int insideY = stepY > 0 ? 15 - (y & 15) : y & 15;
                    int insideZ = stepZ > 0 ? 15 - (z & 15) : z & 15;
                    double exit = Math.min(nextX + insideX * deltaX, Math.min(nextY + insideY * deltaY, nextZ + insideZ * deltaZ));
                    if (exit > 1 + 1.0E-12) {
                        return true;
                    }
                    // 跳到区段边界之前，跨边界仍按逐格步进的顺序处理。
                    int skipX = stepsBeforeExit(nextX, Math.abs(dx), exit, insideX);
                    int skipY = stepsBeforeExit(nextY, Math.abs(dy), exit, insideY);
                    int skipZ = stepsBeforeExit(nextZ, Math.abs(dz), exit, insideZ);
                    x += skipX * stepX;
                    y += skipY * stepY;
                    z += skipZ * stepZ;
                    nextX = advance(nextX, deltaX, skipX);
                    nextY = advance(nextY, deltaY, skipY);
                    nextZ = advance(nextZ, deltaZ, skipZ);
                }
            }

            if (section != null) {
                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                if (!state.isAir() && !state.getCollisionShape(blocks, pos.set(x, y, z)).isEmpty()) {
                    return false;
                }
            }
        }
    }

    private static int stepsBeforeExit(double next, double speed, double exit, int inside) {
        return next < exit ? Math.min(inside, (int) ((exit - next) * speed)) : 0;
    }

    private static double advance(double next, double delta, int steps) {
        for (int i = 0; i < steps; i++) {
            next += delta;
        }
        return next;
    }

    @FunctionalInterface
    interface SectionLookup {
        @Nullable LevelChunkSection get(int x, int y, int z);
    }
}
