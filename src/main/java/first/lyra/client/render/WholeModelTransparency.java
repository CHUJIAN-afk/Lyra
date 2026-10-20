package first.lyra.client.render;

import com.lowdragmc.photon.client.light.DynamicLightRenderer;
import com.lowdragmc.photon.core.mixins.accessor.LightTextureAccessor;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class WholeModelTransparency {

    // 模型保留独立深度，图集装满后按距离合成并复用，不复制世界深度。
    private static final List<Model> MODELS = new ArrayList<>();
    private static final Map<RenderType, ByteBufferBuilder> MEMORY = new IdentityHashMap<>();
    private static final Map<RenderType, Blend> BLENDS = new IdentityHashMap<>();
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Matrix4f VIEW = new Matrix4f();
    private static final Matrix4f CLIP = new Matrix4f();
    private static Capture active;
    private static ShaderInstance shader;
    private static Page atlas;
    private static int width;
    private static int height;

    private WholeModelTransparency() {
    }

    public static void setShader(ShaderInstance value) {
        shader = value;
    }

    public static void render(PoseStack poseStack, MultiBufferSource original, float alpha, int packedLight, Consumer<MultiBufferSource> draw) {
        if (alpha > 0) {
            if (active != null) {
                draw.accept(new ColorBufferSource(active).setAlpha(alpha));
            } else if (alpha >= 1) {
                draw.accept(original);
            } else {
                if (MODELS.isEmpty()) {
                    PROJECTION.set(RenderSystem.getProjectionMatrix());
                    VIEW.set(RenderSystem.getModelViewMatrix());
                    CLIP.set(PROJECTION).mul(VIEW);
                    width = GlStateManager.Viewport.width();
                    height = GlStateManager.Viewport.height();
                }
                Capture capture = new Capture();
                active = capture;
                draw.accept(capture);
                active = null;
                List<Batch> batches = new ArrayList<>();
                Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
                Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
                capture.builders.forEach((type, builder) -> {
                    MeshData mesh = builder.build();
                    if (mesh != null) {
                        if (type.sortOnUpload()) {
                            mesh.sortQuads(MEMORY.get(type), RenderSystem.getVertexSorting());
                        }
                        batches.add(new Batch(type, mesh));
                        bounds(mesh, min, max);
                    }
                });
                Rect rect = rectangle(min, max);
                if (rect.width > 0 && rect.height > 0 && !batches.isEmpty()) {
                    var pose = poseStack.last().pose();
                    double distance = (double) pose.m30() * pose.m30() + (double) pose.m31() * pose.m31() + (double) pose.m32() * pose.m32();
                    MODELS.add(new Model(batches, rect, alpha, packedLight, distance));
                } else {
                    batches.forEach(batch -> batch.mesh.close());
                }
            }
        }
    }

    public static void prepareLighting() {
        if (!MODELS.isEmpty()) {
            shader.safeGetUniform("PhotonEnabled").set(DynamicLightRenderer.hasLights() ? 1 : 0);
            if (DynamicLightRenderer.hasLights()) {
                DynamicLightRenderer.bindLights(shader);
            }
        }
    }

    public static void finishFrame() {
        if (!MODELS.isEmpty()) {
            Minecraft minecraft = Minecraft.getInstance();
            var main = minecraft.getMainRenderTarget();
            if (width == main.width && height == main.height) {
                State state = new State();
                Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
                var sorting = RenderSystem.getVertexSorting();
                var view = RenderSystem.getModelViewStack();
                view.pushMatrix().set(VIEW);
                RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(PROJECTION, sorting);
                if (atlas == null || atlas.target.width != Math.max(1024, width) || atlas.target.height != Math.max(1024, height)) {
                    if (atlas != null) {
                        atlas.target.destroyBuffers();
                    }
                    atlas = new Page(Math.max(1024, width), Math.max(1024, height));
                }
                atlas.clear();
                MODELS.sort(Comparator.comparingDouble(Model::distance).reversed());
                BufferBuilder quads = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                for (Model model : MODELS) {
                    if (!atlas.fits(model.rect)) {
                        composite(quads);
                        atlas.clear();
                        quads = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                    }
                    Slot slot = atlas.reserve(model.rect);
                    for (Batch batch : model.batches) {
                        // CULL 不主动开启剔除，先恢复被合成阶段关闭的默认状态。
                        RenderSystem.enableCull();
                        batch.type.setupRenderState();
                        atlas.target.bindWrite(false);
                        RenderSystem.viewport(slot.x - model.rect.x, slot.y - model.rect.y, width, height);
                        RenderSystem.enableScissor(slot.x, slot.y, model.rect.width, model.rect.height);
                        RenderSystem.colorMask(true, true, true, true);
                        RenderSystem.enableDepthTest();
                        RenderSystem.depthMask(true);
                        Blend blend = BLENDS.computeIfAbsent(batch.type, type -> Blend.current());
                        RenderSystem.enableBlend();
                        RenderSystem.blendFuncSeparate(blend.source, blend.additive ? GL11.GL_ONE : GL11.GL_ONE_MINUS_SRC_ALPHA,
                                blend.additive ? GL11.GL_ZERO : GL11.GL_ONE, blend.additive ? GL11.GL_ONE : GL11.GL_ONE_MINUS_SRC_ALPHA);
                        BufferUploader.drawWithShader(batch.mesh);
                        batch.type.clearRenderState();
                    }
                    quad(quads, model, slot);
                }
                composite(quads);
                RenderSystem.setProjectionMatrix(projection, sorting);
                view.popMatrix();
                RenderSystem.applyModelViewMatrix();
                state.restore();
            } else {
                MODELS.forEach(model -> model.batches.forEach(batch -> batch.mesh.close()));
            }
            MODELS.clear();
            MEMORY.values().forEach(ByteBufferBuilder::clear);
        }
    }

    private static void composite(BufferBuilder quads) {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        RenderSystem.disableScissor();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.colorMask(true, true, true, false);
        RenderSystem.setShader(() -> shader);
        shader.setSampler("ModelColor", atlas.target.getColorTextureId());
        shader.setSampler("ModelDepth", atlas.target.getDepthTextureId());
        shader.safeGetUniform("ScreenSize").set((float) width, (float) height);
        shader.safeGetUniform("InverseProjection").set(new Matrix4f(PROJECTION).invert());
        shader.safeGetUniform("FogColor").set(RenderSystem.getShaderFogColor());
        shader.safeGetUniform("FogStart").set(RenderSystem.getShaderFogStart());
        shader.safeGetUniform("FogEnd").set(RenderSystem.getShaderFogEnd());
        shader.safeGetUniform("FogShape").set(RenderSystem.getShaderFogShape().ordinal());
        BufferUploader.drawWithShader(quads.buildOrThrow());
    }

    private static void quad(BufferBuilder quads, Model model, Slot slot) {
        Rect rect = model.rect;
        float ambient = ambient(model.packedLight);
        for (int corner : new int[]{0, 1, 3, 2}) {
            int x = (corner & 1) == 0 ? 0 : rect.width;
            int y = (corner & 2) == 0 ? 0 : rect.height;
            quads.addVertex(2f * (rect.x + x) / width - 1, 2f * (rect.y + y) / height - 1, 0)
                    .setUv(model.alpha, ambient).setColor(-1).setUv2(slot.x + x, slot.y + y);
        }
    }

    private static float ambient(int packedLight) {
        var texture = Minecraft.getInstance().gameRenderer.lightTexture();
        var pixels = ((LightTextureAccessor) texture).getLightPixels();
        int color = pixels.getPixelRGBA((packedLight & 0xFFFF) / 16, (packedLight >>> 16) / 16);
        return Math.max(0.01f, (0.2126f * (color & 255) + 0.7152f * (color >> 8 & 255) + 0.0722f * (color >> 16 & 255)) / 255f);
    }

    private static void bounds(MeshData mesh, Vector3f min, Vector3f max) {
        VertexFormat format = mesh.drawState().format();
        long address = MemoryUtil.memAddress(mesh.vertexBuffer()) + format.getOffset(VertexFormatElement.POSITION);
        for (int i = 0; i < mesh.drawState().vertexCount(); i++) {
            float x = MemoryUtil.memGetFloat(address);
            float y = MemoryUtil.memGetFloat(address + 4);
            float z = MemoryUtil.memGetFloat(address + 8);
            min.set(Math.min(min.x, x), Math.min(min.y, y), Math.min(min.z, z));
            max.set(Math.max(max.x, x), Math.max(max.y, y), Math.max(max.z, z));
            address += format.getVertexSize();
        }
    }

    private static Rect rectangle(Vector3f min, Vector3f max) {
        float left = 1;
        float right = -1;
        float bottom = 1;
        float top = -1;
        Vector4f point = new Vector4f();
        boolean near = false;
        boolean behind = true;
        for (int i = 0; i < 8; i++) {
            CLIP.transform(point.set((i & 1) == 0 ? min.x : max.x, (i & 2) == 0 ? min.y : max.y, (i & 4) == 0 ? min.z : max.z, 1));
            near |= point.w <= 0.001f;
            behind &= point.w <= 0.001f;
            left = Math.min(left, point.x / point.w);
            right = Math.max(right, point.x / point.w);
            bottom = Math.min(bottom, point.y / point.w);
            top = Math.max(top, point.y / point.w);
        }
        int x0 = near ? 0 : Math.clamp((int) Math.floor((left * 0.5 + 0.5) * width) - 2, 0, width);
        int y0 = near ? 0 : Math.clamp((int) Math.floor((bottom * 0.5 + 0.5) * height) - 2, 0, height);
        int x1 = near ? width : Math.clamp((int) Math.ceil((right * 0.5 + 0.5) * width) + 2, 0, width);
        int y1 = near ? height : Math.clamp((int) Math.ceil((top * 0.5 + 0.5) * height) + 2, 0, height);
        return behind ? new Rect(0, 0, 0, 0) : new Rect(x0, y0, x1 - x0, y1 - y0);
    }

    public static void close() {
        MODELS.forEach(model -> model.batches.forEach(batch -> batch.mesh.close()));
        MODELS.clear();
        if (atlas != null) {
            atlas.target.destroyBuffers();
            atlas = null;
        }
        MEMORY.values().forEach(ByteBufferBuilder::close);
        MEMORY.clear();
        BLENDS.clear();
    }

    private record Rect(int x, int y, int width, int height) {
    }

    private record Slot(int x, int y) {
    }

    private record Batch(RenderType type, MeshData mesh) {
    }

    private record Model(List<Batch> batches, Rect rect, float alpha, int packedLight, double distance) {
    }

    private record Blend(boolean additive, int source) {
        static Blend current() {
            boolean additive = GL11.glIsEnabled(GL11.GL_BLEND) && GL11.glGetInteger(GL14.GL_BLEND_DST_RGB) == GL11.GL_ONE;
            return new Blend(additive, additive ? GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB) : GL11.GL_SRC_ALPHA);
        }
    }

    private static class Page {
        final TextureTarget target;
        int x;
        int y;
        int rowHeight;

        Page(int width, int height) {
            target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            target.setClearColor(0, 0, 0, 0);
        }

        boolean fits(Rect rect) {
            return rect.width <= target.width && (x + rect.width <= target.width ? y + Math.max(rowHeight, rect.height) : y + rowHeight + rect.height) <= target.height;
        }

        Slot reserve(Rect rect) {
            if (x + rect.width > target.width) {
                y += rowHeight;
                x = 0;
                rowHeight = 0;
            }
            Slot slot = new Slot(x, y);
            x += rect.width;
            rowHeight = Math.max(rowHeight, rect.height);
            return slot;
        }

        void clear() {
            x = 0;
            y = 0;
            rowHeight = 0;
            RenderSystem.disableScissor();
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
            target.clear(Minecraft.ON_OSX);
        }
    }

    private static class Capture implements MultiBufferSource {
        final Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();

        @Override
        public @NotNull VertexConsumer getBuffer(@NotNull RenderType type) {
            return builders.computeIfAbsent(type, key -> new BufferBuilder(
                    MEMORY.computeIfAbsent(key, ignored -> new ByteBufferBuilder(Math.max(16384, key.bufferSize()))), key.mode(), key.format()));
        }
    }

    private static class State {
        final int framebuffer = GlStateManager.getBoundFramebuffer();
        final int x = GlStateManager.Viewport.x();
        final int y = GlStateManager.Viewport.y();
        final int width = GlStateManager.Viewport.width();
        final int height = GlStateManager.Viewport.height();
        final ShaderInstance shader = RenderSystem.getShader();

        void restore() {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            RenderSystem.viewport(x, y, width, height);
            RenderSystem.disableScissor();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.setShader(() -> shader);
        }
    }
}
