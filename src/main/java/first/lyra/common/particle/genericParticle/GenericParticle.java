package first.lyra.common.particle.genericParticle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
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
    private float renderX;
    private float renderY;
    private float renderZ;
    private float renderScale;
    private float renderDepth;

    public GenericParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet, GenericParticleOptions options) {
        super(level, x, y, z);
        this.spriteSet = spriteSet;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.friction = options.friction();
        this.quadSize = options.scale();
        this.baseScale = this.quadSize;
        this.lifetime = options.lifetime();
        this.centerColor = 0xFF000000 | options.centerColor();
        this.edgeColor = 0xFF000000 | options.edgeColor();
        this.spinSpeed = options.spinSpeed();
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        if (lifetime == 0) {
            this.setSprite(spriteSet.get(0, 1));
        } else {
            this.setSpriteFromAge(spriteSet);
        }
        this.hasPhysics = false;
    }

    public static GenericParticle createWithOptions(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet, GenericParticleOptions options) {
        return new GenericParticle(level, x, y, z, vx, vy, vz, spriteSet, options);
    }

    private float getProgress(float partialTick) {
        return Mth.lerp(partialTick, age - 1, age) / (float) lifetime;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.spriteSet);
        this.oRoll = this.roll;
        this.roll += this.spinSpeed * (1 - getProgress(0));
    }

    boolean prepareRender(Camera camera, float partialTick, Frustum frustum) {
        float progress = getProgress(partialTick);
        renderScale = baseScale * (1.0F - progress * progress);
        if (renderScale == 0.0F || !Float.isFinite(renderScale)) {
            return false;
        }
        double x = Mth.lerp(partialTick, xo, this.x);
        double y = Mth.lerp(partialTick, yo, this.y);
        double z = Mth.lerp(partialTick, zo, this.z);
        double radius = Math.abs(renderScale) * 3.0 * Mth.SQRT_OF_TWO;
        if (frustum != null && !frustum.isVisible(new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius))) {
            return false;
        }
        Vec3 origin = camera.getPosition();
        renderX = (float) (x - origin.x);
        renderY = (float) (y - origin.y);
        renderZ = (float) (z - origin.z);
        renderDepth = camera.getLookVector().dot(renderX, renderY, renderZ);
        return true;
    }

    float renderDepth() {
        return renderDepth;
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTick) {
        if (prepareRender(camera, partialTick, null)) {
            renderPrepared(buffer, camera, partialTick, new Quaternionf(), new Vector3f(), new Vector3f(), false);
        }
    }

    // 使用 UV1、UV2 和 Normal 传递边缘 RGB、精灵宽度和四角坐标。
    void renderPrepared(VertexConsumer buffer, Camera camera, float partialTick, Quaternionf rotation, Vector3f first, Vector3f second, boolean customShader) {
        getFacingCameraMode().setRotation(rotation, camera, partialTick);
        rotation.rotateZ(Mth.lerp(partialTick, oRoll, roll));
        float scale = renderScale;
        first.set(-scale, -scale, 0.0F).rotate(rotation);
        second.set(scale, -scale, 0.0F).rotate(rotation);

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float uHalf = (u0 + u1) * 0.5F;
        if (customShader) {
            first.mul(3.0F);
            second.mul(3.0F);
            renderQuad(buffer, first, second, uHalf, u1, v0, v1, centerColor, edgeColor & 0xFFFFFF, sprite.contents().width(), 1.0F);
        } else {
            int light = getLightColor(partialTick);
            int overlay = OverlayTexture.NO_OVERLAY;
            float uStep = (u1 - u0) / 6.0F;
            float vStep = (v1 - v0) / 3.0F;
            renderQuad(buffer, first, second, u0 + uStep, u0 + uStep * 2, v0 + vStep, v0 + vStep * 2, centerColor, overlay, light, 0.0F);
            first.mul(3.0F);
            second.mul(3.0F);
            renderQuad(buffer, first, second, uHalf, u1, v0, v1, edgeColor, overlay, light, 0.0F);
        }
    }

    private void renderQuad(VertexConsumer buffer, Vector3f first, Vector3f second, float u0, float u1, float v0, float v1, int color, int overlay, int light, float corner) {
        float normalZ = 1.0F - corner;
        buffer.addVertex(renderX + first.x, renderY + first.y, renderZ + first.z, color, u0, v0, overlay, light, -corner, -corner, normalZ);
        buffer.addVertex(renderX + second.x, renderY + second.y, renderZ + second.z, color, u1, v0, overlay, light, corner, -corner, normalZ);
        buffer.addVertex(renderX - first.x, renderY - first.y, renderZ - first.z, color, u1, v1, overlay, light, corner, corner, normalZ);
        buffer.addVertex(renderX - second.x, renderY - second.y, renderZ - second.z, color, u0, v1, overlay, light, -corner, corner, normalZ);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }
}
