package first.lyra.client.geo;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import org.jspecify.annotations.NonNull;

/**
 * {@link GeoAnimatable} 最小壳，仅用于满足 {@code GeoRenderer} 接口的形式参数要求。
 * <p>
 * 不参与动画驱动——动画采样由 {@link GeoAnimationSampler} 独立完成。
 */
public class DummyGeoAnimatable implements GeoAnimatable {

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.@NonNull ControllerRegistrar controllers) {
        // 无动画控制器，动画由 GeoAnimationSampler 外部驱动
    }

    @Override
    public @NonNull AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
