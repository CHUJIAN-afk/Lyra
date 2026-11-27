package first.lyra.common.particle.genericParticle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import first.lyra.client.render.IrisCompat;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Comparator;

public final class GenericParticleRenderer {

    private static ShaderInstance shader;
    private final ObjectArrayList<GenericParticle> pending = new ObjectArrayList<>();
    private final ObjectArrayList<GenericParticle> active = new ObjectArrayList<>();
    private final ObjectArrayList<GenericParticle> visible = new ObjectArrayList<>();
    private final Comparator<GenericParticle> farToNear = Comparator.comparingDouble(GenericParticle::renderDepth).reversed();
    private final Quaternionf rotation = new Quaternionf();
    private final Vector3f first = new Vector3f();
    private final Vector3f second = new Vector3f();

    public static void setShader(ShaderInstance shader) {
        GenericParticleRenderer.shader = shader;
    }

    public void add(GenericParticle particle) {
        pending.add(particle);
    }

    public void tick() {
        int alive = 0;
        for (int i = 0, size = active.size(); i < size; i++) {
            GenericParticle particle = active.get(i);
            particle.tick();
            if (particle.isAlive()) {
                active.set(alive++, particle);
            }
        }
        active.size(alive);
        if (!pending.isEmpty()) {
            active.addAll(pending);
            pending.clear();
        }
    }

    public void clear() {
        pending.clear();
        active.clear();
        visible.clear();
    }

    @SuppressWarnings("deprecation")
    public void render(Camera camera, float partialTick, Frustum frustum, boolean includesOpaqueParticles) {
        if (!active.isEmpty() && !IrisCompat.isShadowPass()) {
            visible.clear();
            for (GenericParticle particle : active) {
                if (particle.prepareRender(camera, partialTick, frustum)) {
                    visible.add(particle);
                }
            }
            if (!visible.isEmpty()) {
                visible.sort(farToNear);
                boolean usesShaderPack = IrisCompat.isUsingShaderPack();
                boolean customShader = shader != null && !usesShaderPack;
                if (customShader) {
                    RenderSystem.setShader(() -> shader);
                } else if (usesShaderPack && !includesOpaqueParticles) {
                    IrisCompat.useTranslucentParticleShader();
                } else {
                    RenderSystem.setShader(GameRenderer::getParticleShader);
                }
                RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                RenderSystem.depthMask(true);

                try {
                    VertexFormat format = customShader ? DefaultVertexFormat.NEW_ENTITY : DefaultVertexFormat.PARTICLE;
                    BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, format);
                    for (GenericParticle particle : visible) {
                        particle.renderPrepared(buffer, camera, partialTick, rotation, first, second, customShader);
                    }
                    BufferUploader.drawWithShader(buffer.buildOrThrow());
                } finally {
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.disableBlend();
                    visible.clear();
                }
            }
        }
    }
}
