package first.lyra.utils;

import first.lyra.common.attachment.ParticlesData;
import first.lyra.common.particle.genericParticle.GenericParticleBuilder;
import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Random;
import java.util.function.Consumer;

public class ParticleHelper {

    private final Level level;
    private final Random random = new Random();
    private ParticleOptions particleType;
    private GenericParticleBuilder genericBuilder;
    private double x, y, z;
    private double vx = 0, vy = 0, vz = 0;
    private int count = 1;
    private double speed = 1.0;
    private double spreadAngle = 0.4;
    private double offsetX = 0, offsetY = 0, offsetZ = 0;

    public ParticleHelper(Level level) {
        this.level = level;
    }

    public static ParticleHelper create(Level level) {
        return new ParticleHelper(level);
    }

    public ParticleHelper type(ParticleOptions type) {
        this.particleType = type;
        return this;
    }

    public ParticleHelper generic(Consumer<GenericParticleBuilder> configurator) {
        GenericParticleBuilder builder = GenericParticleBuilder.create();
        configurator.accept(builder);
        this.genericBuilder = builder;
        return this;
    }

    public ParticleHelper generic(GenericParticleBuilder builder) {
        this.genericBuilder = builder;
        return this;
    }

    public ParticleHelper pos(Vec3 position) {
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
        return this;
    }

    public ParticleHelper pos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public ParticleHelper velocity(Vec3 velocity) {
        this.vx = velocity.x;
        this.vy = velocity.y;
        this.vz = velocity.z;
        return this;
    }

    public ParticleHelper velocity(double vx, double vy, double vz) {
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        return this;
    }

    public ParticleHelper count(int count) {
        this.count = count;
        return this;
    }

    public ParticleHelper speed(double speed) {
        this.speed = speed;
        return this;
    }

    public ParticleHelper spread(double spreadAngle) {
        this.spreadAngle = spreadAngle;
        return this;
    }

    public ParticleHelper offset(double x, double y, double z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
        return this;
    }

    public ParticleHelper offset(double radius) {
        this.offsetX = this.offsetY = this.offsetZ = radius;
        return this;
    }

    public void emit() {
        if (particleType == null && genericBuilder == null) {
            throw new IllegalStateException("Particle type not set. Call type() or generic() first.");
        }

        boolean server = !level.isClientSide();
        ParticlesData batch = server ? level.getData(LyraAttachmentRegister.BatchedParticles) : null;

        if (count <= 0) {
            ParticleOptions options = genericBuilder != null ? genericBuilder.build() : particleType;
            if (server) {
                batch.add(options, x, y, z, vx, vy, vz);
            } else {
                level.addParticle(options, x, y, z, vx, vy, vz);
            }
        } else {
            Vec3 baseDir = new Vec3(vx, vy, vz).normalize();
            for (int i = 0; i < count; i++) {
                double theta = (random.nextDouble() - 0.5) * spreadAngle * 2;
                double phi = (random.nextDouble() - 0.5) * spreadAngle * 2;
                double speedVar = speed * (0.5 + random.nextDouble() * 0.5);

                Vec3 scatteredDir = baseDir.yRot((float) theta).xRot((float) phi);
                Vec3 velocity = scatteredDir.scale(speedVar);

                double px = x + (offsetX > 0 ? (random.nextDouble() - 0.5) * 2 * offsetX : 0);
                double py = y + (offsetY > 0 ? (random.nextDouble() - 0.5) * 2 * offsetY : 0);
                double pz = z + (offsetZ > 0 ? (random.nextDouble() - 0.5) * 2 * offsetZ : 0);

                ParticleOptions options = genericBuilder != null ? genericBuilder.build() : particleType;
                if (server) {
                    batch.add(options, px, py, pz, velocity.x, velocity.y, velocity.z);
                } else {
                    level.addParticle(options, px, py, pz, velocity.x, velocity.y, velocity.z);
                }
            }
        }
    }
}
