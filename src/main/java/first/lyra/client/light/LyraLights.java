package first.lyra.client.light;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import first.lyra.client.config.ClientConfig;
import first.lyra.client.render.EffectPipeline;
import first.lyra.client.render.IrisCompat;
import first.lyra.client.render.RenderState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LyraLights {

    private static final List<DynamicLight> lights = new ArrayList<>();
    private static final List<DynamicLight> visible = new ArrayList<>();
    private static final DynamicLight ignored = new DynamicLight();
    private static int count;
    private static int lightTexture = -1;
    private static int tileTexture = -1;
    private static int columns;
    private static int rows;
    private static TextureTarget scene;
    private static ShaderInstance shader;
    private static final Matrix4f lightProjection = new Matrix4f();
    private static final Matrix4f viewInverse = new Matrix4f();

    public static void setShader(ShaderInstance value) {
        shader = value;
    }

    public static void beginFrame() {
        count = 0;
        visible.clear();
    }

    public static DynamicLight next() {
        if (IrisCompat.isShadowPass()) {
            return ignored.reset();
        }
        if (count == lights.size()) {
            lights.add(new DynamicLight());
        }
        return lights.get(count++).reset();
    }

    public static boolean hasLights() {
        return !visible.isEmpty();
    }

    public static void bindLights(ShaderInstance target) {
        target.setSampler("LyraLightData", lightTexture);
        target.setSampler("LyraLightTiles", tileTexture);
        target.safeGetUniform("LyraTileCount").set(columns, rows);
        target.setSampler("LyraSceneDepth", scene.getDepthTextureId());
        target.safeGetUniform("LyraProjection").set(lightProjection);
        target.safeGetUniform("LyraViewInverse").set(viewInverse);
    }

    public static void render(Camera camera, Matrix4f view, Matrix4f projection) {
        var main = Minecraft.getInstance().getMainRenderTarget();
        lightProjection.set(projection);
        viewInverse.set(view).invert();
        visible.clear();
        if (ClientConfig.DynamicLights.get()) {
            Vec3 origin = camera.getPosition();
            Matrix4f clip = new Matrix4f(projection).mul(view);
            for (int i = 0; i < count; i++) {
                DynamicLight light = lights.get(i);
                if (light.enabled && light.intensity > 0 && light.range > 0
                        && clip.testSphere((float) (light.position.x - origin.x), (float) (light.position.y - origin.y),
                        (float) (light.position.z - origin.z), light.range)) {
                    visible.add(light);
                }
            }
            visible.sort(Comparator.comparingDouble(light -> light.position.distanceSquared(origin.x, origin.y, origin.z) / Math.max(0.001f, light.intensity)));
            if (visible.size() > ClientConfig.MaxLights.get()) {
                visible.subList(ClientConfig.MaxLights.get(), visible.size()).clear();
            }
            if (!visible.isEmpty()) {
                RenderState state = new RenderState();
                upload(origin, view, projection, main.width, main.height);
                if (scene == null) {
                    scene = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
                } else if (scene.width != main.width || scene.height != main.height) {
                    scene.resize(main.width, main.height, Minecraft.ON_OSX);
                }
                if (main.isStencilEnabled() && !scene.isStencilEnabled()) {
                    scene.enableStencil();
                }
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scene.frameBufferId);
                GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height,
                        GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
                main.bindWrite(true);
                RenderSystem.disableScissor();
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.disableBlend();
                RenderSystem.disableCull();
                shader.setSampler("SceneColor", scene.getColorTextureId());
                shader.setSampler("SceneDepth", scene.getDepthTextureId());
                shader.safeGetUniform("ScreenSize").set((float) main.width, (float) main.height);
                shader.safeGetUniform("InverseProjection").set(new Matrix4f(projection).invert());
                shader.safeGetUniform("FogStart").set(RenderSystem.getShaderFogStart());
                shader.safeGetUniform("FogEnd").set(RenderSystem.getShaderFogEnd());
                bindLights(shader);
                shader.apply();
                EffectPipeline.drawFullscreenQuad();
                shader.clear();
                state.restore();
            }
        }
    }

    // 每个屏幕分块保存最多 32 个光源索引，光源按距离和强度优先。
    private static void upload(Vec3 origin, Matrix4f view, Matrix4f projection, int width, int height) {
        columns = (width + 63) / 64;
        rows = (height + 63) / 64;
        float[] tiles = new float[columns * rows * 33 * 4];
        FloatBuffer data = MemoryUtil.memCallocFloat(visible.size() * 16);
        for (int i = 0; i < visible.size(); i++) {
            DynamicLight light = visible.get(i);
            Vector3f position = view.transformPosition(new Vector3f((float) (light.position.x - origin.x),
                    (float) (light.position.y - origin.y), (float) (light.position.z - origin.z)));
            Vector3f direction = view.transformDirection(new Vector3f(light.direction)).normalize();
            data.put(position.x).put(position.y).put(position.z).put(light.range);
            data.put(light.color.x * light.intensity).put(light.color.y * light.intensity).put(light.color.z * light.intensity).put(light.spotlight ? 1 : 0);
            data.put(direction.x).put(direction.y).put(direction.z).put((float) Math.cos(Math.toRadians(light.outerAngle)));
            data.put((float) Math.cos(Math.toRadians(light.innerAngle))).put(light.castShadows ? 1 : 0).put(light.sourceRadius).put(light.volumetric);
            int minX = 0;
            int minY = 0;
            int maxX = columns - 1;
            int maxY = rows - 1;
            if (position.z + light.range < -0.05f) {
                float left = 1;
                float bottom = 1;
                float right = -1;
                float top = -1;
                for (int corner = 0; corner < 8; corner++) {
                    Vector4f projected = projection.transform(new Vector4f(position.x + ((corner & 1) == 0 ? -light.range : light.range),
                            position.y + ((corner & 2) == 0 ? -light.range : light.range),
                            position.z + ((corner & 4) == 0 ? -light.range : light.range), 1));
                    left = Math.min(left, projected.x / projected.w);
                    right = Math.max(right, projected.x / projected.w);
                    bottom = Math.min(bottom, projected.y / projected.w);
                    top = Math.max(top, projected.y / projected.w);
                }
                minX = Math.max(0, (int) ((left * 0.5f + 0.5f) * width / 64));
                maxX = Math.min(maxX, (int) ((right * 0.5f + 0.5f) * width / 64));
                minY = Math.max(0, (int) ((bottom * 0.5f + 0.5f) * height / 64));
                maxY = Math.min(maxY, (int) ((top * 0.5f + 0.5f) * height / 64));
            }
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    int base = (y * columns + x) * 33 * 4;
                    int size = (int) tiles[base];
                    if (size < 32) {
                        tiles[base]++;
                        tiles[base + (size + 1) * 4] = i;
                    }
                }
            }
        }
        if (lightTexture == -1) {
            lightTexture = TextureUtil.generateTextureId();
            tileTexture = TextureUtil.generateTextureId();
        }
        // 图集上传会保留像素行跨度，浮点数据使用紧密布局。
        GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);
        GlStateManager._bindTexture(lightTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, 4, visible.size(), 0, GL11.GL_RGBA, GL11.GL_FLOAT, data.flip());
        MemoryUtil.memFree(data);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GlStateManager._bindTexture(tileTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, columns * 33, rows, 0, GL11.GL_RGBA, GL11.GL_FLOAT, tiles);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GlStateManager._bindTexture(0);
    }

    public static void clear() {
        beginFrame();
        if (scene != null) {
            scene.destroyBuffers();
            scene = null;
        }
        if (lightTexture != -1) {
            TextureUtil.releaseTextureId(lightTexture);
            TextureUtil.releaseTextureId(tileTexture);
            lightTexture = -1;
            tileTexture = -1;
        }
    }
}
