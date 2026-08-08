package first.lyra.mixin;

import first.lyra.common.damageInfo.IDamageSourceCritical;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DamageSource.class)
public class DamageSourceMixin implements IDamageSourceCritical {

    @Unique
    private boolean lyra$isCritical = false;

    @Override
    public boolean lyra$isCritical() {
        return lyra$isCritical;
    }

    @Override
    public void lyra$setCritical(boolean isCritical) {
        this.lyra$isCritical = isCritical;
    }
}
