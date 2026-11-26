package first.lyra.client.render;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

public final class TrailRenderBatch {

    private static ByteBufferBuilder storage;
    private static BufferBuilder vertices;
    private static ShaderInstance fabulousShader;
    private static final Matrix4f view = new Matrix4f();
    private static Matrix4f projection;
    private static VertexSorting sorting;
    private static Fog fog;

    public static void setFabulousShader(ShaderInstance shader) {
        fabulousShader = shader;
    }

    static ShaderInstance fabulousShader() {
        return fabulousShader;
    }

    public static MultiBufferSource collect(MultiBufferSource buffers) {
        return type -> type == LyraRenderTypes.getTrail() ? trailBuffer() : buffers.getBuffer(type);
    }

    private static VertexConsumer trailBuffer() {
        if (vertices == null) {
            var trail = LyraRenderTypes.getTrail();
            if (storage == null) {
                storage = new ByteBufferBuilder(trail.bufferSize());
            }
            vertices = new BufferBuilder(storage, trail.mode(), trail.format());
            view.set(RenderSystem.getModelViewMatrix());
            projection = RenderSystem.getProjectionMatrix();
            sorting = RenderSystem.getVertexSorting();
            fog = Fog.capture();
        }
        return vertices;
    }

    public static void draw() {
        if (vertices != null) {
            var mesh = vertices.build();
            vertices = null;
            if (mesh != null) {
                mesh.sortQuads(storage, sorting);
                var minecraft = Minecraft.getInstance();
                var main = minecraft.getMainRenderTarget();
                var scene = Minecraft.useShaderTransparency() ? minecraft.levelRenderer.getParticlesTarget() : null;
                RenderState state = new RenderState();
                Matrix4f previousProjection = RenderSystem.getProjectionMatrix();
                VertexSorting previousSorting = RenderSystem.getVertexSorting();
                Fog previousFog = Fog.capture();
                main.bindWrite(true);
                RenderSystem.getModelViewStack().pushMatrix().set(view);
                RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(projection, sorting);
                fog.apply();
                if (scene != null) {
                    var clouds = minecraft.levelRenderer.getCloudsTarget();
                    fabulousShader.setSampler("SceneDepth", scene.getDepthTextureId());
                    fabulousShader.setSampler("CloudColor", clouds.getColorTextureId());
                    fabulousShader.setSampler("CloudDepth", clouds.getDepthTextureId());
                    fabulousShader.safeGetUniform("HasClouds").set(minecraft.options.getCloudsType() != CloudStatus.OFF ? 1 : 0);
                    RenderSystem.disableDepthTest();
                    LyraRenderTypes.FABULOUS_TRAIL.draw(mesh);
                } else {
                    LyraRenderTypes.getTrail().draw(mesh);
                }
                previousFog.apply();
                RenderSystem.setProjectionMatrix(previousProjection, previousSorting);
                RenderSystem.getModelViewStack().popMatrix();
                RenderSystem.applyModelViewMatrix();
                state.restore();
            }
        }
    }

    public static void close() {
        if (storage != null) {
            storage.close();
            storage = null;
            vertices = null;
        }
    }

    private record Fog(float start, float end, FogShape shape, float[] color) {

        private static Fog capture() {
            return new Fog(RenderSystem.getShaderFogStart(), RenderSystem.getShaderFogEnd(),
                    RenderSystem.getShaderFogShape(), RenderSystem.getShaderFogColor().clone());
        }

        private void apply() {
            RenderSystem.setShaderFogStart(start);
            RenderSystem.setShaderFogEnd(end);
            RenderSystem.setShaderFogShape(shape);
            RenderSystem.setShaderFogColor(color[0], color[1], color[2], color[3]);
        }
    }
}
