package first.lyra.common.attachment;

import first.lyra.common.damageInfo.DamageInfo;
import first.lyra.common.damageInfo.IDamageSourceCritical;
import first.lyra.common.network.BatchedDamageInfoPayload;
import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DamageInfoData {

    public static void handler(LivingDamageEvent.Post event) {
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

    public static void tick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (!level.isClientSide()) {
            DamageInfoData damageData = level.getData(LyraAttachmentRegister.DamageInfoData);
            if (damageData.size() > 0) {
                PacketDistributor.sendToPlayersInDimension((ServerLevel) level, new BatchedDamageInfoPayload(damageData.drain()));
            }
        } else {
            level.getData(LyraAttachmentRegister.DamageInfoData).tick();
        }
    }

    /** 服务端：累积 Entry 待发包 */
    private final List<BatchedDamageInfoPayload.Entry> pendingEntries = new ArrayList<>();
    /**
     * 客户端：持有活跃 DamageInfo 渲染列表
     */
    private final Map<ResourceLocation, List<DamageInfo>> activeInfos = new HashMap<>();

    public DamageInfoData() {}

    // ===================== 服务端 =====================

    /** 开启一次链式伤害数字构建 */
    public static DamageInfoBuilder build(Level level) {
        return new DamageInfoBuilder(level);
    }

    /** 累积一条伤害记录 */
    public void addEntry(BatchedDamageInfoPayload.Entry entry) {
        pendingEntries.add(entry);
    }

    /** 取出并清空当前累积的所有伤害记录 */
    public List<BatchedDamageInfoPayload.Entry> drain() {
        List<BatchedDamageInfoPayload.Entry> snapshot = new ArrayList<>(pendingEntries);
        pendingEntries.clear();
        return snapshot;
    }

    /** 当前累积伤害记录数 */
    public int size() {
        return pendingEntries.size();
    }

    // ===================== 客户端 =====================

    /** 客户端 tick：驱动 DamageInfo 生命周期衰减，移除已过期的 */
    public void tick() {
        activeInfos.values()
                .removeIf(infoList -> {
                    infoList.removeIf(DamageInfo::tick);
                    return infoList.isEmpty();
                });
    }

    /** 获取当前活跃的渲染数据列表 */
    public Map<ResourceLocation, List<DamageInfo>> getActiveInfos() {
        return activeInfos;
    }

    // ===================== 链式构建器 =====================

    /**
     * 链式伤害数字构建器：必填 damageType/damageAmount/pos，可省略其余（有默认值）。
     * <p>
     * 调用 {@link #emit()} 将记录写入 Level 附件，由 tick 末统一批发包。
     * 渲染参数（贴图、颜色、尺寸等）由客户端根据 damageType 从 JSON 样式表查询，
     * 不通过网络同步。
     * </p>
     */
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

        /**
         * 将记录写入 Level 附件，客户端调用无效
         */
        public void emit() {
            if (!level.isClientSide() && damageAmount >= 0.01) {
                level.getData(LyraAttachmentRegister.DamageInfoData)
                        .addEntry(new BatchedDamageInfoPayload.Entry(damageType, damageAmount, x, y, z, vx, vy, vz, critical));
            }
        }
    }
}
