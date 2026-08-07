package first.lyra.common.servant;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServantDamageSource extends DamageSource {

    private final Servant servant;

    public ServantDamageSource(Holder<DamageType> type, @Nullable Entity directEntity, @Nullable Entity causingEntity, @Nullable Vec3 damageSourcePosition, @NotNull Servant servant) {
        super(type, directEntity, causingEntity, damageSourcePosition);
        this.servant = servant;
    }

    @NotNull
    public Servant getServant() {
        return servant;
    }
}
