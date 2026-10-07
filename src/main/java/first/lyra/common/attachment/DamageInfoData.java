package first.lyra.common.attachment;

import first.lyra.Lyra;
import first.lyra.common.damageInfo.DamageInfo;
import first.lyra.common.damageInfo.IDamageSourceCritical;
import first.lyra.common.network.BatchedDamageInfoPayload;
import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.mesdag.portlib.event.entity.living.PortLivingDamageEvent;
import org.mesdag.portlib.event.tick.PortLevelTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DamageInfoData {

    public static void handler(PortLivingDamageEvent.Post event) {
        DamageSource damageSource = event.getSource();
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (!level.isClientSide() && damageSource.getEntity() instanceof Player) {
            AABB box = entity.getBoundingBox();
            RandomSource random = entity.getRandom();
            Vec3 pos = box.getCenter()
                    .add(0, box.getYsize() / 2, 0);
            Vec3 velocity = pos.add(0, box.getYsize() / 2, 0)
                    .offsetRandom(random, (float) (box.getXsize() + box.getZsize()) * 0.5f)
                    .subtract(pos)
                    .normalize();
            boolean critical = damageSource instanceof IDamageSourceCritical iDamageSourceCritical && iDamageSourceCritical.lyra$isCritical();
            DamageInfoData.build(level)
                    .damageType(damageSource.typeHolder().getRegisteredName())
                    .damageAmount(event.getNewDamage())
                    .pos(pos)
                    .velocity(velocity.scale(random.nextInt(50, 70) * 0.01f))
                    .critical(critical)
                    .emit();
        }
    }

    public static void tick(PortLevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (!level.isClientSide()) {
            DamageInfoData damageData = level.getData(LyraAttachmentRegister.DamageInfoData);
            if (damageData.size() > 0) {
                Lyra.NETWORK_HANDLER.sendToPlayersInDimension(level.dimension(), new BatchedDamageInfoPayload(damageData.drain()));
            }
        } else {
            level.getData(LyraAttachmentRegister.DamageInfoData).tick();
        }
    }

    private final List<BatchedDamageInfoPayload.Entry> pendingEntries = new ArrayList<>();
    private final Map<ResourceLocation, List<DamageInfo>> activeInfos = new HashMap<>();

    public DamageInfoData() {}

    public static DamageInfoBuilder build(Level level) {
        return new DamageInfoBuilder(level);
    }

    public void addEntry(BatchedDamageInfoPayload.Entry entry) {
        pendingEntries.add(entry);
    }

    public List<BatchedDamageInfoPayload.Entry> drain() {
        List<BatchedDamageInfoPayload.Entry> snapshot = new ArrayList<>(pendingEntries);
        pendingEntries.clear();
        return snapshot;
    }

    public int size() {
        return pendingEntries.size();
    }

    public void tick() {
        activeInfos.values()
                .removeIf(infoList -> {
                    infoList.removeIf(DamageInfo::tick);
                    return infoList.isEmpty();
                });
    }

    public Map<ResourceLocation, List<DamageInfo>> getActiveInfos() {
        return activeInfos;
    }

    public static final class DamageInfoBuilder {
        private final Level level;
        private String damageType = "default";
        private float damageAmount;
        private double x, y, z;
        private double vx, vy, vz;
        private boolean critical = false;

        DamageInfoBuilder(Level level) {
            this.level = level;
        }

        public DamageInfoBuilder damageType(Holder<DamageType> damageType) {
            this.damageType = damageType.getRegisteredName();
            return this;
        }

        public DamageInfoBuilder damageType(String damageType) {
            this.damageType = damageType;
            return this;
        }

        public DamageInfoBuilder damageAmount(float amount) {
            this.damageAmount = amount;
            return this;
        }

        public DamageInfoBuilder pos(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        public DamageInfoBuilder pos(Vec3 pos) {
            return pos(pos.x, pos.y, pos.z);
        }

        public DamageInfoBuilder velocity(double vx, double vy, double vz) {
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            return this;
        }

        public DamageInfoBuilder velocity(Vec3 velocity) {
            return velocity(velocity.x, velocity.y, velocity.z);
        }

        public DamageInfoBuilder critical(boolean critical) {
            this.critical = critical;
            return this;
        }

        public void emit() {
            if (!level.isClientSide() && damageAmount >= 0.01) {
                level.getData(LyraAttachmentRegister.DamageInfoData).addEntry(new BatchedDamageInfoPayload.Entry(damageType, damageAmount, x, y, z, vx, vy, vz, critical));
            }
        }
    }
}
