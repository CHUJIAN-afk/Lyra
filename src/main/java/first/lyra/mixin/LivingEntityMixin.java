package first.lyra.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import first.lyra.common.attachment.SummonMarkTracker;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.AttachmentEntityDamageSource;
import first.lyra.common.attachmentEntity.IMomentumAttachmentEntity;
import first.lyra.common.minion.Minion;
import first.lyra.common.projectile.Projectile;
import first.lyra.register.LyraAttachmentRegister;
import first.lyra.register.LyraAttributeRegister;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.Nullable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    protected abstract boolean isAlwaysExperienceDropper();

    @Shadow
    public abstract boolean wasExperienceConsumed();

    @Shadow
    protected int lastHurtByPlayerTime;

    @Shadow
    @Nullable
    protected Player lastHurtByPlayer;

    @Shadow
    public abstract int getExperienceReward(ServerLevel level, @Nullable Entity killer);

    @Shadow
    @Final
    public int invulnerableDuration;

    @SuppressWarnings("DataFlowIssue")
    @WrapWithCondition(
            method = "dropAllDeathLoot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;dropExperience(Lnet/minecraft/world/entity/Entity;)V"
            )
    )
    private boolean dropExperience(LivingEntity instance, Entity entity, @Local(argsOnly = true) DamageSource damageSource) {
        if (damageSource instanceof AttachmentEntityDamageSource source) {
            Player player = null;
            if (source.getAttachmentEntity() instanceof Minion minion && minion.getOwner() instanceof Player owner) {
                player = owner;
            }
            if (source.getAttachmentEntity() instanceof Projectile projectile && projectile.getOwner() instanceof Player owner) {
                player = owner;
            }
            if (player != null && instance.level() instanceof ServerLevel serverlevel && !wasExperienceConsumed() && (isAlwaysExperienceDropper() || lastHurtByPlayerTime > 0 && instance.shouldDropExperience() && serverlevel.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))) {
                int reward = EventHooks.getExperienceDrop(instance, lastHurtByPlayer, getExperienceReward(serverlevel, entity));
                Vec3 pos = instance.position();
                int takeXpDelay = player.takeXpDelay;
                while (reward > 0) {
                    int i = ExperienceOrb.getExperienceValue(reward);
                    reward -= i;
                    if (!ExperienceOrbAccessor.callTryMergeToExisting(serverlevel, pos, i)) {
                        ExperienceOrb experienceOrb = new ExperienceOrb(serverlevel, player.getX(), player.getY(), player.getZ(), i);
                        player.takeXpDelay = 0;
                        experienceOrb.playerTouch(player);
                        player.takeXpDelay = takeXpDelay;
                    }
                }
                return false;
            }
        }
        return true;
    }

    @WrapMethod(method = "hurt")
    public boolean hurt(DamageSource source, float amount, Operation<Boolean> original) {
        if (source instanceof AttachmentEntityDamageSource damageSource) {
            Player owner = damageSource.getOwner();
            AttributeInstance instance = owner.getAttribute(LyraAttributeRegister.SummonDamage);
            amount *= instance != null ? (float) instance.getValue() : 1;
            amount *= 0.85f + owner.getRandom().nextFloat() * 0.3f;
            SummonMarkTracker data = owner.getData(LyraAttachmentRegister.SummonMarkData);
            if (data.isTarget(LivingEntity.class.cast(this))) {
                amount = data.getDamageModifier(damageSource, amount);
            }
        }
        return original.call(source, amount);
    }

    @WrapOperation(
            method = "hurt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"
            )
    )
    private void knockback(LivingEntity target, double strength, double x, double z, Operation<Void> original, @Local(argsOnly = true) DamageSource damageSource) {
        if (damageSource instanceof AttachmentEntityDamageSource source) {
            AttachmentEntity entity = source.getAttachmentEntity();
            LivingEntity owner = entity.getOwner();
            AttributeInstance instance = owner != null ? owner.getAttribute(LyraAttributeRegister.SummonKnockback) : null;
            double scale = instance != null ? instance.getValue() : 1;
            if (owner != null) {
                scale *= 0.8 + (0.4 * owner.getRandom().nextDouble());
            }
            strength = entity.getKnockback() * scale;

            if (entity instanceof IMomentumAttachmentEntity momentum) {
                Vec3 direction = momentum.getVelocity();
                if (direction.lengthSqr() == 0.0) {
                    direction = entity.getLookAngle();
                }
                double horizontalLength = Math.hypot(direction.x, direction.z);
                if (horizontalLength > 0.0) {
                    x = -direction.x / horizontalLength;
                    z = -direction.z / horizontalLength;
                }
            }
        }
        original.call(target, strength, x, z);
    }
}
