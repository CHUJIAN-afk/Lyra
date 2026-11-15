package first.lyra.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import first.lyra.client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.ArrayList;
import java.util.List;

public final class EffectPipeline {

    private static final List<HdrTarget> levels = new ArrayList<>();
    private static HdrTarget highlight;
    private static HdrTarget output;
    private static ShaderInstance extractShader;
    private static ShaderInstance downsampleShader;
    private static ShaderInstance upsampleShader;
    private static ShaderInstance compositeShader;
    private static ShaderInstance textureShader;
    private static ShaderInstance resolveShader;

    public static void setTextureShader(ShaderInstance shader) {
        textureShader = shader;
    }

    public static void setResolveShader(ShaderInstance shader) {
        resolveShader = shader;
    }

    public static ShaderInstance textureShader() {
        return textureShader;
    }

    public static void composite(RenderTarget source) {
        var main = Minecraft.getInstance().getMainRenderTarget();
        RenderState state = new RenderState();
        main.bindWrite(true);
        RenderSystem.disableScissor();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendEquation(GL14.GL_FUNC_ADD);
        RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
        RenderSystem.colorMask(true, true, true, false);
        resolveShader.setSampler("Source", source.getColorTextureId());
        resolveShader.apply();
        drawFullscreenQuad();
        resolveShader.clear();
        state.restore();
        main.bindWrite(true);
    }

    public static void setExtractShader(ShaderInstance shader) {
        extractShader = shader;
    }

    public static void setDownsampleShader(ShaderInstance shader) {
        downsampleShader = shader;
    }

    public static void setUpsampleShader(ShaderInstance shader) {
        upsampleShader = shader;
    }

    public static void setCompositeShader(ShaderInstance shader) {
        compositeShader = shader;
    }

    public static void drawFullscreenQuad() {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.addVertex(-1, -1, 0);
        builder.addVertex(1, -1, 0);
        builder.addVertex(1, 1, 0);
        builder.addVertex(-1, 1, 0);
        BufferUploader.draw(builder.buildOrThrow());
    }

    public static RenderTarget bloom(RenderTarget source) {
        RenderTarget result = source;
        if (ClientConfig.Bloom.get() && (!IrisCompat.isUsingShaderPack() || ClientConfig.BloomWithShaders.get())) {
            RenderState state = new RenderState();
            RenderSystem.disableScissor();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            int count = ClientConfig.BloomLevels.get();
            while (levels.size() > count) {
                levels.removeLast().destroyBuffers();
            }
            highlight = HdrTarget.resize(highlight, Math.max(1, source.width / 2), Math.max(1, source.height / 2), false);
            highlight.bindWrite(true);
            extractShader.setSampler("Source", source.getColorTextureId());
            extractShader.safeGetUniform("Threshold").set(ClientConfig.BloomThreshold.get().floatValue());
            extractShader.apply();
            drawFullscreenQuad();
            extractShader.clear();
            RenderTarget input = highlight;
            for (int i = 0; i < count; i++) {
                HdrTarget level = HdrTarget.resize(i < levels.size() ? levels.get(i) : null,
                        Math.max(1, input.width / 2), Math.max(1, input.height / 2), false);
                if (i == levels.size()) {
                    levels.add(level);
                } else {
                    levels.set(i, level);
                }
                level.bindWrite(true);
                downsampleShader.setSampler("Source", input.getColorTextureId());
                downsampleShader.safeGetUniform("TexelSize").set(1f / input.width, 1f / input.height);
                downsampleShader.apply();
                drawFullscreenQuad();
                downsampleShader.clear();
                input = level;
            }
            RenderSystem.enableBlend();
            RenderSystem.blendEquation(GL14.GL_FUNC_ADD);
            RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
            for (int i = levels.size() - 2; i >= 0; i--) {
                levels.get(i).bindWrite(true);
                upsampleShader.setSampler("Source", levels.get(i + 1).getColorTextureId());
                upsampleShader.apply();
                drawFullscreenQuad();
                upsampleShader.clear();
            }
            RenderSystem.disableBlend();
            output = HdrTarget.resize(output, source.width, source.height, false);
            output.bindWrite(true);
            compositeShader.setSampler("Source", source.getColorTextureId());
            compositeShader.setSampler("Bloom", levels.getFirst().getColorTextureId());
            compositeShader.safeGetUniform("Strength").set(ClientConfig.BloomStrength.get().floatValue());
            compositeShader.safeGetUniform("Threshold").set(ClientConfig.BloomThreshold.get().floatValue());
            compositeShader.apply();
            drawFullscreenQuad();
            compositeShader.clear();
            state.restore();
            result = output;
        }
        return result;
    }

    public static void close() {
        for (HdrTarget level : levels) {
            level.destroyBuffers();
        }
        levels.clear();
        if (highlight != null) {
            highlight.destroyBuffers();
            highlight = null;
        }
        if (output != null) {
            output.destroyBuffers();
            output = null;
        }
    }
}
