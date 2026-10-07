package first.lyra.common.particle.genericParticle;

import java.util.Random;

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

    public GenericParticleBuilder centerColor(int rgb) {
        this.centerColor = rgb;
        return this;
    }

    public GenericParticleBuilder edgeColor(int rgb) {
        this.edgeColor = rgb;
        return this;
    }

    public GenericParticleBuilder lifetime(int lifetime) {
        this.lifetime = lifetime;
        return this;
    }

    public GenericParticleBuilder lifetimeRandom(int range) {
        this.lifetimeJitter = range;
        return this;
    }

    public GenericParticleBuilder spin(float spinSpeed) {
        this.spinSpeed = spinSpeed;
        return this;
    }

    public GenericParticleBuilder spinRandom(float range) {
        this.spinJitter = range;
        return this;
    }

    public GenericParticleBuilder friction(float friction) {
        this.friction = friction;
        return this;
    }

    public GenericParticleBuilder scale(float scale) {
        this.scale = scale;
        return this;
    }

    public GenericParticleBuilder scaleRandom(float range) {
        this.scaleJitter = range;
        return this;
    }

    public GenericParticleOptions build() {
        return new GenericParticleOptions(centerColor, edgeColor, lifetime + (lifetimeJitter > 0 ? random.nextInt(lifetimeJitter) : 0), spinSpeed + (spinJitter > 0 ? random.nextFloat() * spinJitter * 2 - spinJitter : 0), friction, scale + (scaleJitter > 0 ? random.nextFloat() * scaleJitter : 0));
    }
}
