package first.lyra.common.particle.genericParticle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class GenericParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;
    private final float baseScale;
    private final float spinSpeed;
    private final int centerColor;
    private final int edgeColor;

    public GenericParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet, GenericParticleOptions options) {
        super(level, x, y, z, vx, vy, vz);
        this.spriteSet = spriteSet;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.friction = options.friction();
        this.gravity = 0.0F;
        this.quadSize = options.scale();
        this.baseScale = this.quadSize;
        this.lifetime = options.lifetime();
        this.centerColor = options.centerColor();
        this.edgeColor = options.edgeColor();
        this.spinSpeed = options.spinSpeed();
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.alpha = 1F;
        this.setSpriteFromAge(spriteSet);
        this.hasPhysics = false;
    }

    public static GenericParticle createWithOptions(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet, GenericParticleOptions options) {
        return new GenericParticle(level, x, y, z, vx, vy, vz, spriteSet, options);
    }

    private float getProgress(float partialTick) {
        return (Mth.lerp(partialTick, age - 1, age)) / (float) this.lifetime;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.spriteSet);
        this.oRoll = this.roll;
        this.roll += this.spinSpeed * (1 - getProgress(0));
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTick) {
        Vec3 cameraPos = camera.getPosition();
        float x = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPos.x);
        float y = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPos.y);
        float z = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPos.z);

        Quaternionf quaternion = new Quaternionf(camera.rotation());
        if (this.roll != 0.0F) {
            quaternion.rotateZ(Mth.lerp(partialTick, this.oRoll, this.roll));
        }

        float progress = getProgress(partialTick);
        float scale = this.baseScale * (1.0F - (progress * progress));
        int alpha = 255;
        int centerARGB = (alpha << 24) | centerColor;
        int edgeARGB = (alpha << 24) | edgeColor;

        float u0 = this.sprite.getU0();
        float u1 = this.sprite.getU1();
        float v0 = this.sprite.getV0();
        float v1 = this.sprite.getV1();

        int light = getLightColor(partialTick);
        int overlay = OverlayTexture.NO_OVERLAY;

        float uHalf = (u0 + u1) / 2.0F;
        float uStep = (u1 - u0) / 6.0F;
        float vStep = (v1 - v0) / 3.0F;

        renderQuad(buffer, x, y, z, quaternion, -scale, -scale, scale, scale, u0 + uStep, u0 + uStep * 2, v0 + vStep, v0 + vStep * 2, centerARGB, light);
        renderQuad(buffer, x, y, z, quaternion, -scale * 3, -scale * 3, scale * 3, scale * 3, uHalf, u1, v0, v1, edgeARGB, light);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    private void renderQuad(VertexConsumer buffer, float cx, float cy, float cz, Quaternionf quaternion, float minX, float minY, float maxX, float maxY, float u0, float u1, float v0, float v1, int color, int light) {
        Vector3f v = new Vector3f();
        v.set(minX, minY, 0.0F).rotate(quaternion);
        addVertex(buffer, cx + v.x, cy + v.y, cz + v.z, color, u1, v1, light);
        v.set(minX, maxY, 0.0F).rotate(quaternion);
        addVertex(buffer, cx + v.x, cy + v.y, cz + v.z, color, u1, v0, light);
        v.set(maxX, maxY, 0.0F).rotate(quaternion);
        addVertex(buffer, cx + v.x, cy + v.y, cz + v.z, color, u0, v0, light);
        v.set(maxX, minY, 0.0F).rotate(quaternion);
        addVertex(buffer, cx + v.x, cy + v.y, cz + v.z, color, u0, v1, light);
    }

    private static void addVertex(VertexConsumer buffer, float x, float y, float z, int color, float u, float v, int light) {
        buffer.vertex(x, y, z)
                .uv(u, v)
                .color(color)
                .uv2(light)
                .endVertex();
    }

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }
}
