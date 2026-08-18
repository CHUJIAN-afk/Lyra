package first.lyra.common.particle.genericParticle;

import java.util.Random;

/**
 * 通用粒子配置构建器，支持链式调用配置颜色、寿命、旋转、阻力、大小等参数。
 */
public class GenericParticleBuilder {

    private final Random random = new Random();
    private int centerColor = 0xFFFFFF;
    private int edgeColor = 0xFFFFFF;
    private int lifetime = 0;
    private int lifetimeJitter = 0;
    private float spinSpeed = 0.0F;
    private float spinJitter = 0.0F;
    private float friction = 1F;
    private float scale = 1F;
    private float scaleJitter = 0.0F;

    private GenericParticleBuilder() {

    }

    public static GenericParticleBuilder create() {
        return new GenericParticleBuilder();
    }

    /**
     * 设置中心色块颜色（RGB）
     */
    public GenericParticleBuilder centerColor(int rgb) {
        this.centerColor = rgb;
        return this;
    }

    /**
     * 设置边缘色块颜色（RGB）
     */
    public GenericParticleBuilder edgeColor(int rgb) {
        this.edgeColor = rgb;
        return this;
    }

    /** 设置粒子寿命（tick） */
    public GenericParticleBuilder lifetime(int lifetime) {
        this.lifetime = lifetime;
        return this;
    }

    /** 设置寿命随机抖动幅度 */
    public GenericParticleBuilder lifetimeRandom(int range) {
        this.lifetimeJitter = range;
        return this;
    }

    /** 设置旋转速度（弧度/tick） */
    public GenericParticleBuilder spin(float spinSpeed) {
        this.spinSpeed = spinSpeed;
        return this;
    }

    /** 设置旋转速度随机抖动幅度 */
    public GenericParticleBuilder spinRandom(float range) {
        this.spinJitter = range;
        return this;
    }

    /** 设置粒子阻力（每tick速度乘数） */
    public GenericParticleBuilder friction(float friction) {
        this.friction = friction;
        return this;
    }

    /**
     * 设置粒子大小
     */
    public GenericParticleBuilder scale(float scale) {
        this.scale = scale;
        return this;
    }

    /**
     * 设置大小随机抖动幅度
     */
    public GenericParticleBuilder scaleRandom(float range) {
        this.scaleJitter = range;
        return this;
    }

    /**
     * 构建粒子选项
     */
    public GenericParticleOptions build() {
        return new GenericParticleOptions(centerColor, edgeColor, lifetime + (lifetimeJitter > 0 ? random.nextInt(lifetimeJitter) : 0), spinSpeed + (spinJitter > 0 ? random.nextFloat() * spinJitter * 2 - spinJitter : 0), friction, scale + (scaleJitter > 0 ? random.nextFloat() * scaleJitter : 0));
    }
}
