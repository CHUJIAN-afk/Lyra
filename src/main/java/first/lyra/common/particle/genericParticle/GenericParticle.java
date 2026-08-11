package first.lyra.common.particle.genericParticle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.NonNull;

/**
 * 通用粒子 - 自定义渲染中心色块和边缘色块。
 * <p>
 * 颜色为 RGB（不含透明度），透明度由粒子进度自动控制。
 * 缩放通过 {@link #getProgress} 计算渲染进度，无冗余中间变量。
 * </p>
 * <p>
 * 26.2: TextureSheetParticle 移除 → SingleQuadParticle;render() 改为 extract() 渲染状态提取;
 * ParticleRenderType 变 record,使用 {@link SingleQuadParticle.Layer#TRANSLUCENT}。
 * </p>
 */
public class GenericParticle extends SingleQuadParticle {

    private static final SingleQuadParticle.Layer LAYER = SingleQuadParticle.Layer.TRANSLUCENT;

    private final SpriteSet spriteSet;
    private final float baseScale;
    private final float spinSpeed;
    private final int centerColor;
    private final int edgeColor;

    public GenericParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet, GenericParticleOptions options) {
        super(level, x, y, z, vx, vy, vz, spriteSet.get(0, 1));
        this.spriteSet = spriteSet;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.friction = options.friction();
        this.gravity = 0.0F;
        this.quadSize = options.scale();
        this.baseScale = this.quadSize;
        this.lifetime = options.lifetime();
        // 颜色（RGB直接存储）
        this.centerColor = options.centerColor();
        this.edgeColor = options.edgeColor();
        // 旋转
        this.spinSpeed = options.spinSpeed();
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.alpha = 1F;
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

    /**
     * 26.2: 渲染状态提取模式。贴图布局 6×3 像素：左侧 3×3 中心色块，右侧 3×3 边缘色块。
     */
    @Override
    public void extract(@NonNull QuadParticleRenderState renderState, @NonNull Camera camera, float partialTick) {
        Quaternionf rotation = new Quaternionf();
        this.getFacingCameraMode().setRotation(rotation, camera, partialTick);
        if (this.roll != 0.0F) {
            rotation.rotateZ(Mth.lerp(partialTick, this.oRoll, this.roll));
        }

        Vec3 cameraPos = camera.position();
        float x = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPos.x);
        float y = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPos.y);
        float z = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPos.z);

        float progress = getProgress(partialTick);
        float scale = this.baseScale * (1.0F - (progress * progress));
        // RGB → ARGB：透明度由进度控制（1=全不透明，0=全透明）
        int alpha = 255;
        int centerARGB = (alpha << 24) | centerColor;
        int edgeARGB = (alpha << 24) | edgeColor;

        // 纹理坐标（完整贴图范围）
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        int light = this.getLightCoords(partialTick);

        // 贴图布局：6×3 像素，左右拼接两个 3×3
        //   左侧 3×3：仅中心 (1,1) 有色 → 中心色块
        //   右侧 3×3：上下左右 (1,0)(0,1)(2,1)(1,2) 有色 → 边缘色块
        float uHalf = (u0 + u1) / 2.0F;
        float uStep = (u1 - u0) / 6.0F;
        float vStep = (v1 - v0) / 3.0F;

        // 中心色块
        renderState.add(LAYER, x, y, z, rotation.x, rotation.y, rotation.z, rotation.w,
                scale, u0 + uStep, u0 + uStep * 2, v0 + vStep, v0 + vStep * 2, centerARGB, light);
        // 边缘色块（中心像素透明自然形成十字）
        renderState.add(LAYER, x, y, z, rotation.x, rotation.y, rotation.z, rotation.w,
                scale * 3, uHalf, u1, v0, v1, edgeARGB, light);
    }

    @Override
    protected SingleQuadParticle.@NonNull Layer getLayer() {
        return LAYER;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }
}
