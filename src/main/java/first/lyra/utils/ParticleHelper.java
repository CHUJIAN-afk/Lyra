package first.lyra.utils;

import first.lyra.common.attachment.ParticlesData;
import first.lyra.common.particle.genericParticle.GenericParticleBuilder;
import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Random;
import java.util.function.Consumer;

/**
 * 粒子生成辅助类，支持链式调用配置粒子参数。
 * <p>
 * 当 count > 0 时：发射多个粒子，使用随机散射
 * 当 count = 0 时：发射单个粒子，使用精确速度
 * </p>
 */
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

    /**
     * 设置粒子类型。
     */
    public ParticleHelper type(ParticleOptions type) {
        this.particleType = type;
        return this;
    }

    /**
     * 使用通用粒子类型，通过Consumer配置参数。
     */
    public ParticleHelper generic(Consumer<GenericParticleBuilder> configurator) {
        GenericParticleBuilder builder = GenericParticleBuilder.create();
        configurator.accept(builder);
        this.genericBuilder = builder;
        return this;
    }

    /**
     * 使用通用粒子类型，通过Builder配置参数。
     */
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

    /**
     * 设置粒子数量。
     * count > 0：发射多个粒子，使用随机散射
     * count = 0：发射单个粒子，使用精确速度
     */
    public ParticleHelper count(int count) {
        this.count = count;
        return this;
    }

    public ParticleHelper speed(double speed) {
        this.speed = speed;
        return this;
    }

    /**
     * 设置散射角度（仅当 count > 0 时有效）。
     */
    public ParticleHelper spread(double spreadAngle) {
        this.spreadAngle = spreadAngle;
        return this;
    }

    /**
     * 设置位置偏移范围（仅当 count > 0 时有效，每个粒子在基础位置上添加随机偏移）。
     */
    public ParticleHelper offset(double x, double y, double z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
        return this;
    }

    /**
     * 设置位置偏移范围（仅当 count > 0 时有效）。
     */
    public ParticleHelper offset(double radius) {
        this.offsetX = this.offsetY = this.offsetZ = radius;
        return this;
    }

    /**
     * 发射粒子，根据 count 自动选择模式。
     * <p>
     * 服务端：累积到 Level 的 {@link ParticlesData} 附件，由 tick 末统一打包下发，
     * 避免每个粒子单独发送网络包。客户端：直接调用 {@link Level#addParticle} 生成粒子。
     * </p>
     */
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

                // 位置偏移
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