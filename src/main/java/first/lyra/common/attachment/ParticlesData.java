package first.lyra.common.attachment;

import first.lyra.common.network.BatchedParticlesPayload;
import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class ParticlesData {

    public static void tick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (!level.isClientSide()) {
            ParticlesData data = level.getData(LyraAttachmentRegister.BatchedParticles);
            if (!data.entries.isEmpty()) {
                List<BatchedParticlesPayload.Entry> snapshot = new ArrayList<>(data.entries);
                data.entries.clear();
                PacketDistributor.sendToPlayersInDimension((ServerLevel) level, new BatchedParticlesPayload(snapshot));
            }
        }
    }

    private final List<BatchedParticlesPayload.Entry> entries = new ArrayList<>();

    /** 累积一条粒子记录 */
    public void add(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz) {
        entries.add(new BatchedParticlesPayload.Entry(options, x, y, z, vx, vy, vz));
    }
}
