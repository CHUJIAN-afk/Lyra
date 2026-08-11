package first.lyra.common.particle.genericParticle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class GenericParticleProvider implements ParticleProvider<GenericParticleOptions> {

    private final SpriteSet sprite;

    public GenericParticleProvider(SpriteSet sprite) {
        this.sprite = sprite;
    }

    @Override
    // 26.2: createParticle 新增第 10 参 RandomSource
    public Particle createParticle(@NotNull GenericParticleOptions options, @NotNull ClientLevel level, double x, double y, double z, double vx, double vy, double vz, @NonNull RandomSource random) {
        return GenericParticle.createWithOptions(level, x, y, z, vx, vy, vz, sprite, options);
    }
}