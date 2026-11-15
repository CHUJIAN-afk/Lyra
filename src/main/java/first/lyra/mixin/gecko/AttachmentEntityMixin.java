package first.lyra.mixin.gecko;

import first.lyra.common.attachmentEntity.AttachmentEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

@Mixin(AttachmentEntity.class)
public abstract class AttachmentEntityMixin implements GeoAnimatable {

    @Shadow
    protected int tickCount;
    @Unique
    private AnimatableInstanceCache lyra$animationCache;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        if (lyra$animationCache == null) {
            lyra$animationCache = GeckoLibUtil.createInstanceCache(this);
        }
        return lyra$animationCache;
    }

    @Override
    public double getTick(Object object) {
        return tickCount;
    }
}
