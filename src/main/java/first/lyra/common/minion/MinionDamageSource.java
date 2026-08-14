package first.lyra.common.minion;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MinionDamageSource extends DamageSource {

    private final Minion minion;

    public MinionDamageSource(Holder<DamageType> type, @Nullable Entity directEntity, @Nullable Entity causingEntity, @Nullable Vec3 damageSourcePosition, @NotNull Minion minion) {
        super(type, directEntity, causingEntity, damageSourcePosition);
        this.minion = minion;
    }

    @NotNull
    public Minion getMinion() {
        return minion;
    }
}
