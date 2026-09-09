package first.lyra.mixin;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 虚拟实体渲染器需要把幽灵实体的 walk 状态精确重置到当前帧姿态，
 * 原版只提供 speed setter，没有 position/speedOld setter。
 */
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
