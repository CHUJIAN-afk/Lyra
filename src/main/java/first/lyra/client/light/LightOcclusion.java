package first.lyra.client.light;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import first.lyra.client.render.EffectPipeline;
import first.lyra.client.render.HdrTarget;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;

// 缓存光源附近的半方块遮挡体素，生成与相机方向无关的可见距离图。
public final class LightOcclusion {

    private final Long2ObjectOpenHashMap<Section> sections = new Long2ObjectOpenHashMap<>();
    private final Reference2IntOpenHashMap<BlockState> masks = new Reference2IntOpenHashMap<>();
    private final IntArrayList freeSlots = new IntArrayList();
    private final int[] pages = new int[32 * 16 * 32];
    private int nextSlot;
    private int pageTexture = -1;
    private int blockTexture = -1;
    private int originX;
    private int originY;
    private int originZ;
    private long frame;
    private HdrTarget visibility;
    private ShaderInstance shader;

    private static class Section {
        private int page;
        private boolean dirty = true;
        private long lastUsed;
    }

    public void setShader(ShaderInstance value) {
        shader = value;
    }

    public void markDirty(int x, int y, int z) {
        Section section = sections.get(SectionPos.asLong(x, y, z));
        if (section != null) {
            section.dirty = true;
        }
    }

    public void markColumnDirty(int x, int z) {
        for (var entry : sections.long2ObjectEntrySet()) {
            if (SectionPos.x(entry.getLongKey()) == x && SectionPos.z(entry.getLongKey()) == z) {
                entry.getValue().dirty = true;
            }
        }
    }

    public void bind(ShaderInstance target, Vec3 camera) {
        target.setSampler("LyraVoxelPages", Math.max(pageTexture, 0));
        target.setSampler("LyraVoxelBlocks", Math.max(blockTexture, 0));
        target.safeGetUniform("LyraVoxelOffset").set((float) (camera.x - originX * 16.0),
                (float) (camera.y - originY * 16.0), (float) (camera.z - originZ * 16.0));
        target.setSampler("LyraVisibility", visibility == null ? 0 : visibility.getColorTextureId());
    }

    public void render(ClientLevel level, Vec3 camera, Matrix4f inverseView, List<DynamicLight> lights, int lightTexture) {
        boolean shadowed = false;
        for (DynamicLight light : lights) {
            shadowed |= light.castShadows;
        }
        if (shadowed) {
            originX = SectionPos.blockToSectionCoord(camera.x) - 16;
            originY = SectionPos.blockToSectionCoord(camera.y) - 8;
            originZ = SectionPos.blockToSectionCoord(camera.z) - 16;
            frame++;
            sections.long2ObjectEntrySet().removeIf(entry -> {
                int x = SectionPos.x(entry.getLongKey()) - originX;
                int y = SectionPos.y(entry.getLongKey()) - originY;
                int z = SectionPos.z(entry.getLongKey()) - originZ;
                boolean remove = x < 0 || x >= 32 || y < 0 || y >= 16 || z < 0 || z >= 32 || frame - entry.getValue().lastUsed > 200;
                if (remove && entry.getValue().page >= 3) {
                    freeSlots.add(entry.getValue().page - 3);
                }
                return remove;
            });
            if (pageTexture == -1) {
                pageTexture = TextureUtil.generateTextureId();
                blockTexture = TextureUtil.generateTextureId();
                GlStateManager._bindTexture(blockTexture);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R8UI, 4096, 4096, 0, GL30.GL_RED_INTEGER, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            }
            long deadline = System.nanoTime() + 2_000_000;
            for (DynamicLight light : lights) {
                if (light.castShadows) {
                    int minX = Math.max(originX, SectionPos.blockToSectionCoord(light.position.x - light.range));
                    int maxX = Math.min(originX + 31, SectionPos.blockToSectionCoord(light.position.x + light.range));
                    int minY = Math.max(Math.max(originY, level.getMinSection()), SectionPos.blockToSectionCoord(light.position.y - light.range));
                    int maxY = Math.min(Math.min(originY + 15, level.getMaxSection() - 1), SectionPos.blockToSectionCoord(light.position.y + light.range));
                    int minZ = Math.max(originZ, SectionPos.blockToSectionCoord(light.position.z - light.range));
                    int maxZ = Math.min(originZ + 31, SectionPos.blockToSectionCoord(light.position.z + light.range));
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            for (int x = minX; x <= maxX; x++) {
                                Section section = sections.computeIfAbsent(SectionPos.asLong(x, y, z), key -> new Section());
                                section.lastUsed = frame;
                                if (section.dirty && System.nanoTime() < deadline) {
                                    build(level, x, y, z, section);
                                }
                            }
                        }
                    }
                }
            }
            java.util.Arrays.fill(pages, 0);
            for (var entry : sections.long2ObjectEntrySet()) {
                int x = SectionPos.x(entry.getLongKey()) - originX;
                int y = SectionPos.y(entry.getLongKey()) - originY;
                int z = SectionPos.z(entry.getLongKey()) - originZ;
                pages[x + 32 * (z + 32 * y)] = entry.getValue().page;
            }
            GlStateManager._bindTexture(pageTexture);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R32UI, 128, 128, 0, GL30.GL_RED_INTEGER, GL11.GL_UNSIGNED_INT, pages);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            visibility = HdrTarget.resize(visibility, 1024, ((lights.size() + 7) / 8) * 128, false);
            visibility.bindWrite(true);
            shader.setSampler("LyraLightData", lightTexture);
            shader.safeGetUniform("LyraLightCount").set(lights.size());
            shader.safeGetUniform("LyraViewInverse").set(inverseView);
            bind(shader, camera);
            shader.apply();
            EffectPipeline.drawFullscreenQuad();
            shader.clear();
        }
    }

    private void build(ClientLevel level, int x, int y, int z, Section section) {
        var chunk = level.getChunkSource().getChunk(x, z, false);
        if (chunk != null) {
            var blocks = chunk.getSection(level.getSectionIndexFromSectionY(y));
            int page = 1;
            if (!blocks.hasOnlyAir()) {
                ByteBuffer data = MemoryUtil.memCalloc(4096);
                int any = 0;
                int all = 255;
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                for (int by = 0; by < 16; by++) {
                    for (int bz = 0; bz < 16; bz++) {
                        for (int bx = 0; bx < 16; bx++) {
                            BlockState state = blocks.getBlockState(bx, by, bz);
                            int mask = 0;
                            if (state.is(BlockTags.LEAVES)) {
                                mask = 255;
                            } else if (state.canOcclude()) {
                                boolean dynamic = state.getBlock().hasDynamicShape();
                                if (!dynamic && masks.containsKey(state)) {
                                    mask = masks.getInt(state);
                                } else {
                                    var shape = state.getOcclusionShape(level, pos.set(x * 16 + bx, y * 16 + by, z * 16 + bz));
                                    if (Block.isShapeFullBlock(shape)) {
                                        mask = 255;
                                    } else {
                                        var boxes = shape.toAabbs();
                                        for (int cell = 0; cell < 8; cell++) {
                                            double cx = 0.25 + 0.5 * (cell & 1);
                                            double cy = 0.25 + 0.5 * ((cell >> 1) & 1);
                                            double cz = 0.25 + 0.5 * ((cell >> 2) & 1);
                                            for (var box : boxes) {
                                                if (box.contains(cx, cy, cz)) {
                                                    mask |= 1 << cell;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if (!dynamic) {
                                        masks.put(state, mask);
                                    }
                                }
                            }
                            any |= mask;
                            all &= mask;
                            data.put(bx + 16 * (by & 3) + 64 * (bz + 16 * (by >> 2)), (byte) mask);
                        }
                    }
                }
                if (any != 0) {
                    page = 2;
                    if (all != 255) {
                        int slot = section.page - 3;
                        if (slot < 0) {
                            if (freeSlots.isEmpty()) {
                                slot = nextSlot++;
                            } else {
                                slot = freeSlots.removeInt(freeSlots.size() - 1);
                            }
                        }
                        if (slot < 4096) {
                            GlStateManager._bindTexture(blockTexture);
                            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, (slot & 63) * 64, (slot >> 6) * 64, 64, 64, GL30.GL_RED_INTEGER, GL11.GL_UNSIGNED_BYTE, data);
                            page = slot + 3;
                        } else {
                            nextSlot = 4096;
                            page = 0;
                        }
                    }
                }
                MemoryUtil.memFree(data);
            }
            if (section.page >= 3 && page != section.page) {
                freeSlots.add(section.page - 3);
            }
            section.page = page;
            section.dirty = page == 0;
        } else {
            if (section.page >= 3) {
                freeSlots.add(section.page - 3);
            }
            section.page = 0;
        }
    }

    public void clear() {
        sections.clear();
        masks.clear();
        freeSlots.clear();
        nextSlot = 0;
        if (pageTexture != -1) {
            TextureUtil.releaseTextureId(pageTexture);
            TextureUtil.releaseTextureId(blockTexture);
            pageTexture = -1;
            blockTexture = -1;
        }
        if (visibility != null) {
            visibility.destroyBuffers();
            visibility = null;
        }
    }
}
