package first.lyra.common.attachment;

import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraDamageRegister;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mesdag.portlib.event.entity.living.PortLivingDamageEvent;

import java.util.UUID;

@Deprecated
public class InvincibleData {

    public static void handler(PortLivingDamageEvent.Post event) {
        DamageSource damageSource = event.getSource();
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (!level.isClientSide() && damageSource.getEntity() instanceof LivingEntity attacker) {
            InvincibleData.get(entity).recordHit(attacker.getUUID(), 100);
        }
    }

    private final Long2IntOpenHashMap hurtHistory;
    private final Long2IntOpenHashMap partialInvincibleFrames;
    private int globalInvincibleFrames;

    public InvincibleData() {
        this.hurtHistory = new Long2IntOpenHashMap();
        this.partialInvincibleFrames = new Long2IntOpenHashMap();
        this.globalInvincibleFrames = 0;
    }

    public void tick() {
        if (!hurtHistory.isEmpty()) {
            hurtHistory.replaceAll((k, v) -> v - 1);
            hurtHistory.values().removeIf(v -> v <= 0);
        }
        if (!partialInvincibleFrames.isEmpty()) {
            partialInvincibleFrames.replaceAll((k, v) -> v - 1);
            partialInvincibleFrames.values().removeIf(v -> v <= 0);
        }
        if (globalInvincibleFrames > 0) {
            globalInvincibleFrames--;
        }
    }

    public static InvincibleData get(LivingEntity living) {
        return living.getData(LyraAttachmentRegister.InvincibleData);
    }

    public boolean hasAttack(UUID uuid) {
        return hurtHistory.containsKey(uuid.getMostSignificantBits());
    }

    public void recordHit(@NotNull UUID uuid, int ticks) {
        hurtHistory.put(uuid.getMostSignificantBits(), ticks);
    }

    public static AttackBuilder attack(LivingEntity target) {
        return new AttackBuilder(target);
    }

    public enum Type {
        PARTIAL, GLOBAL
    }

    public static final class AttackBuilder {
        private final @NotNull LivingEntity target;
        private @Nullable UUID uuid = null;
        private int invincibleTime = 0;
        private @Nullable DamageSource damageSource = null;
        private float damageAmount = 0;
        private @NotNull Type type = Type.PARTIAL;
        private @Nullable MobEffectInstance mobEffectInstance = null;

        private AttackBuilder(@NotNull LivingEntity target) {
            this.target = target;
        }

        public AttackBuilder attacker(@Nullable UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        public AttackBuilder damageSource(@Nullable DamageSource damageSource) {
            this.damageSource = damageSource;
            return this;
        }

        public AttackBuilder damageAmount(float damageAmount) {
            this.damageAmount = damageAmount;
            return this;
        }

        public AttackBuilder invincibleTime(int ticks) {
            this.invincibleTime = ticks;
            return this;
        }

        public AttackBuilder global() {
            this.type = Type.GLOBAL;
            return this;
        }

        public AttackBuilder effect(@Nullable MobEffectInstance mobEffectInstance) {
            this.mobEffectInstance = mobEffectInstance;
            return this;
        }

        public boolean apply() {
            if (target.isAlive() && damageAmount > 0) {
                InvincibleData invincibleData = InvincibleData.get(target);
                boolean canDamage = uuid == null;
                if (!canDamage) {
                    long uuidKey = uuid.getMostSignificantBits();
                    canDamage = switch (type) {
                        case PARTIAL -> !invincibleData.partialInvincibleFrames.containsKey(uuidKey);
                        case GLOBAL -> invincibleData.globalInvincibleFrames <= 0;
                    };
                }
                if (canDamage) {
                    Level level = target.level();
                    if (damageSource == null) {
                        damageSource = LyraDamageRegister.getDamageSource(DamageTypes.GENERIC, level);
                    }
                    int invulnerableTime = target.invulnerableTime;
                    target.invulnerableTime = 0;
                    boolean hurt = target.hurt(damageSource, damageAmount);
                    target.invulnerableTime = invulnerableTime;
                    if (hurt) {
                        if (mobEffectInstance != null) {
                            target.addEffect(mobEffectInstance);
                        }
                        if (invincibleTime > 0) {
                            if (type == Type.PARTIAL && uuid != null) {
                                invincibleData.partialInvincibleFrames.put(uuid.getMostSignificantBits(), invincibleTime);
                            } else {
                                invincibleData.globalInvincibleFrames = invincibleTime;
                            }
                        }
                    }
                    return hurt;
                }
            }
            return false;
        }
    }
}
