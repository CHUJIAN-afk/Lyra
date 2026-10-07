package first.lyra.mixin;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WalkAnimationState.class)
public interface WalkAnimationStateAccessor {

    @Accessor
    float getSpeedOld();

    @Mutable
    @Accessor
    void setSpeedOld(float speedOld);

    @Accessor
    float getPosition();

    @Mutable
    @Accessor
    void setPosition(float position);
}
