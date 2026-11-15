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

public final class EffectPipeline {

    private static final HdrTarget[] down = new HdrTarget[4];
    private static final HdrTarget[] blur = new HdrTarget[4];
    private static HdrTarget output;
    private static ShaderInstance blurShader;
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
        ShaderInstance previous = RenderSystem.getShader();
        main.bindWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
        RenderSystem.colorMask(true, true, true, false);
        resolveShader.setSampler("Source", source.getColorTextureId());
        resolveShader.apply();
        drawFullscreenQuad();
        resolveShader.clear();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> previous);
    }

    public static void setBlurShader(ShaderInstance shader) {
        blurShader = shader;
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
            ShaderInstance previous = RenderSystem.getShader();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            RenderTarget input = source;
            for (int i = 0; i < down.length; i++) {
                int width = Math.max(1, source.width >> (i + 1));
                int height = Math.max(1, source.height >> (i + 1));
                down[i] = HdrTarget.resize(down[i], width, height, false);
                blur[i] = HdrTarget.resize(blur[i], width, height, false);
                down[i].setFilterMode(GL11.GL_LINEAR);
                blur[i].setFilterMode(GL11.GL_LINEAR);
                down[i].bindWrite(true);
                blurShader.setSampler("Source", input.getColorTextureId());
                blurShader.safeGetUniform("Direction").set(1f / input.width, 0f);
                blurShader.safeGetUniform("Threshold").set(i == 0 ? 1f : 0f);
                blurShader.apply();
                drawFullscreenQuad();
                blurShader.clear();
                blur[i].bindWrite(true);
                blurShader.setSampler("Source", down[i].getColorTextureId());
                blurShader.safeGetUniform("Direction").set(0f, 1f / height);
                blurShader.safeGetUniform("Threshold").set(0f);
                blurShader.apply();
                drawFullscreenQuad();
                blurShader.clear();
                input = blur[i];
            }
            output = HdrTarget.resize(output, source.width, source.height, false);
            output.bindWrite(true);
            compositeShader.setSampler("Source", source.getColorTextureId());
            for (int i = 0; i < blur.length; i++) {
                compositeShader.setSampler("Bloom" + i, blur[i].getColorTextureId());
            }
            compositeShader.safeGetUniform("Strength").set(ClientConfig.BloomStrength.get().floatValue());
            compositeShader.apply();
            drawFullscreenQuad();
            compositeShader.clear();
            RenderSystem.enableCull();
            RenderSystem.setShader(() -> previous);
            result = output;
        }
        return result;
    }

    public static void close() {
        for (int i = 0; i < down.length; i++) {
            if (down[i] != null) {
                down[i].destroyBuffers();
                blur[i].destroyBuffers();
                down[i] = null;
                blur[i] = null;
            }
        }
        if (output != null) {
            output.destroyBuffers();
            output = null;
        }
    }
}
