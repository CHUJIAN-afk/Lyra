package first.lyra.mixinHandler;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import first.lyra.common.minion.Minion;
import first.lyra.common.minion.MinionDamageSource;
import first.lyra.register.LyraAttributeRegister;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

public class LivingEntityMixinHandler {

    public static boolean hurtServer(LivingEntity living, ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
        if (source instanceof MinionDamageSource minionDamageSource) {
            Minion minion = minionDamageSource.getMinion();
            Player owner = minion.getOwner();
            damage = getDamage(living, owner, minion, damage);
        }
        return original.call(level, source, damage);
    }

    public static float getDamage(LivingEntity living, Player owner, Minion minion, float damage) {
        AttributeInstance instance = owner.getAttribute(LyraAttributeRegister.SummonDamage);
        float scale = instance != null ? (float) instance.getValue() : 1;
        damage *= scale;
        damage *= 0.85f + owner.getRandom().nextFloat() * 0.3f;
        return damage;
    }
}
