package first.lyra.register;

import first.lyra.Lyra;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public class LyraDamageRegister {

    public static final ResourceKey<DamageType> Servant = ResourceKey.create(Registries.DAMAGE_TYPE, Lyra.rl("servant"));

    public static Holder<DamageType> getDamageTypeHolder(ResourceKey<DamageType> resourceKey, Level level) {
        Registry<DamageType> damageTypes = level.registryAccess()
                .registry(Registries.DAMAGE_TYPE)
                .orElse(null);
        if (damageTypes != null) {
            Holder.Reference<DamageType> holder = damageTypes.getHolder(resourceKey)
                    .orElse(null);
            if (holder != null) {
                return holder;
            }
        }
        return level.damageSources()
                .generic()
                .typeHolder();
    }

    public static DamageSource getDamageSource(ResourceKey<DamageType> resourceKey, Level level) {
        Registry<DamageType> damageTypes = level.registryAccess()
                .registry(Registries.DAMAGE_TYPE)
                .orElse(null);
        if (damageTypes != null) {
            Holder.Reference<DamageType> holder = damageTypes.getHolder(resourceKey)
                    .orElse(null);
            if (holder != null) {
                return new DamageSource(holder, null, null, null);
            }
        }
        return level.damageSources()
                .generic();
    }
}
