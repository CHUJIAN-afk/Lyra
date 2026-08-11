package first.lyra.client.geo;

import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import com.geckolib.animation.object.EasingType;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.cache.animation.BoneAnimation;
import com.geckolib.cache.animation.Keyframe;
import com.geckolib.cache.animation.KeyframeStack;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;

import java.util.List;

/**
 * 自定义关键帧采样器，绕开 GeckoLib 的 {@code AnimationController}/{@code handleAnimations} 管线。
 * <p>
 * 直接从 {@link GeckoLibCache} 读取 {@link Animation}，按给定 tick 采样所有 bone 的关键帧，
 * 将插值结果写入 {@link GeoBone} 的 rot/pos/scale。
 * <p>
 * 采样是纯函数：传入 tick → 采样 → 写 bone。无跨帧状态，无需缓存。
 */
public class GeoAnimationSampler {

    private final Identifier animationResource;

    /**
     * 缓存的动画解析结果（{@link Animation} 是不可变 record，安全复用）
     */
    private BakedAnimations cachedBakedAnimations;
    private String cachedAnimationName;
    private Animation cachedAnimation;

    public GeoAnimationSampler(Identifier animationResource) {
        this.animationResource = animationResource;
    }

    /**
     * 采样指定动画在给定 tick 时刻的姿态，写入 BakedGeoModel 中的 GeoBone。
     * <p>
     * 调用前应确保 bone 处于初始姿态（由调用方 reset）；调用后 bone 持有当前帧姿态。
     *
     * @param animName   动画名（animation.json 中的 key，如 "shooting"）
     * @param tick       动画时间（tick 域，0=起始）
     * @param bakedModel 当前帧要渲染的 BakedGeoModel
     */
    public void sample(String animName, double tick, BakedGeoModel bakedModel) {
        Animation anim = resolveAnimation(animName);
        if (anim == null)
            return;

        double elapsed = computeElapsed(anim, tick);

        for (BoneAnimation boneAnim : anim.boneAnimations()) {
            GeoBone bone = findBone(bakedModel, boneAnim.boneName());
            if (bone == null)
                continue;

            sampleRotation(boneAnim.rotationKeyFrames(), elapsed, bone);
            samplePosition(boneAnim.positionKeyFrames(), elapsed, bone);
            sampleScale(boneAnim.scaleKeyFrames(), elapsed, bone);
        }
    }

    // ===================== 时间计算 =====================

    private double computeElapsed(Animation anim, double tick) {
        if (anim.length() <= 0)
            return tick;

        // LoopType 判断：LOOP 类型取模，否则钳制到末帧
        if (isLooping(anim)) {
            return tick % anim.length();
        }
        return Math.min(tick, anim.length());
    }

    /**
     * 判断动画是否循环播放。
     * GeckoLib 的 LoopType 是函数式接口，我们只检查常见的内置类型。
     */
    private boolean isLooping(Animation anim) {
        // 26.2: LoopType 移到 com.geckolib.animation.object
        return anim.loopType() == LoopType.LOOP;
    }

    // ===================== 动画解析 =====================

    @Nullable
    private Animation resolveAnimation(String animName) {
        // 26.2: GeckoLibCache 拆分为 GeckoLibResources.getBakedAnimations() (BakedAnimationCache record)
        BakedAnimations baked = GeckoLibResources.getBakedAnimations().cache().get(this.animationResource);
        if (baked == null)
            return null;

        // 缓存命中检查（避免每帧 map lookup）
        if (baked != this.cachedBakedAnimations || !animName.equals(this.cachedAnimationName)) {
            this.cachedBakedAnimations = baked;
            this.cachedAnimationName = animName;
            this.cachedAnimation = baked.getAnimation(animName);
        }

        return this.cachedAnimation;
    }

    // ===================== Bone 查找 =====================

    @Nullable
    private GeoBone findBone(BakedGeoModel model, String boneName) {
        for (GeoBone topBone : model.topLevelBones()) {
            GeoBone found = findBoneRecursive(topBone, boneName);
            if (found != null)
                return found;
        }
        return null;
    }

    @Nullable
    private GeoBone findBoneRecursive(GeoBone bone, String name) {
        // 26.2: getName→name()、getChildBones→children()(数组)
        if (bone.name().equals(name))
            return bone;
        for (GeoBone child : bone.children()) {
            GeoBone found = findBoneRecursive(child, name);
            if (found != null)
                return found;
        }
        return null;
    }

    // ===================== 通道采样 =====================

    // 26.2: GeoBone 不可变,骨骼姿态写入 frameSnapshot(BoneSnapshot)
    private BoneSnapshot ensureSnapshot(GeoBone bone) {
        if (bone.frameSnapshot == null) {
            bone.frameSnapshot = BoneSnapshot.create(bone);
        }
        return bone.frameSnapshot;
    }

    private void sampleRotation(KeyframeStack stack, double elapsed, GeoBone bone) {
        // 空列表 = 该通道无动画，保持 bone 初始值不动
        if (stack.xKeyframes().length == 0 && stack.yKeyframes().length == 0 && stack.zKeyframes().length == 0)
            return;
        float x = sampleAxis(stack.xKeyframes(), elapsed);
        float y = sampleAxis(stack.yKeyframes(), elapsed);
        float z = sampleAxis(stack.zKeyframes(), elapsed);
        ensureSnapshot(bone).setRotation(x, y, z);
    }

    private void samplePosition(KeyframeStack stack, double elapsed, GeoBone bone) {
        if (stack.xKeyframes().length == 0 && stack.yKeyframes().length == 0 && stack.zKeyframes().length == 0)
            return;
        float x = sampleAxis(stack.xKeyframes(), elapsed);
        float y = sampleAxis(stack.yKeyframes(), elapsed);
        float z = sampleAxis(stack.zKeyframes(), elapsed);
        ensureSnapshot(bone).setTranslation(x, y, z);
    }

    private void sampleScale(KeyframeStack stack, double elapsed, GeoBone bone) {
        // scale 空列表时不写 bone——GeoBone 默认 scaleX/Y/Z = 1，写 0 会导致骨骼不可见
        if (stack.xKeyframes().length == 0 && stack.yKeyframes().length == 0 && stack.zKeyframes().length == 0)
            return;
        float x = sampleAxis(stack.xKeyframes(), elapsed);
        float y = sampleAxis(stack.yKeyframes(), elapsed);
        float z = sampleAxis(stack.zKeyframes(), elapsed);
        ensureSnapshot(bone).setScale(x, y, z);
    }

    /**
     * 采样单轴的关键帧列表，返回在 elapsed tick 时刻的插值结果。
     * <p>
     * 关键帧按时间顺序排列，每个 keyframe 的 {@code length} 是该帧持续时间（tick），
     * 时间从 0 开始累加。
     */
    private float sampleAxis(Keyframe[] keyframes, double elapsed) {
        if (keyframes.length == 0)
            return 0f;

        double accumulatedStart = 0;

        for (int i = 0; i < keyframes.length; i++) {
            Keyframe kf = keyframes[i];
            double kfEnd = accumulatedStart + kf.length();

            if (elapsed < kfEnd || i == keyframes.length - 1) {
                // 找到了所在区间（或在最后一个 keyframe 之后）
                double currentTick = elapsed - accumulatedStart;
                double transitionLength = kf.length();

                if (transitionLength <= 0 || currentTick >= transitionLength) {
                    // 26.2: MathValue.get 需要 ControllerState 参数(模组绕过控制器,传 null)
                    return (float) kf.endValue().get(null);
                }

                double lerpValue = currentTick / transitionLength;
                double easedLerp = applyEasing(kf, lerpValue);
                return (float) Mth.lerp(easedLerp, kf.startValue().get(null), kf.endValue().get(null));
            }

            accumulatedStart = kfEnd;
        }

        // 理论上不会到达，但保险取末帧值
        return (float) keyframes[keyframes.length - 1].endValue().get(null);
    }

    /**
     * 应用 keyframe 的 easing 类型。
     */
    private double applyEasing(Keyframe kf, double lerpValue) {
        EasingType easingType = kf.easingType();
        Double easingArg = kf.easingArgs().length == 0 ? null : kf.easingArgs()[0].get(null);
        return easingType.buildTransformer(easingArg).apply(lerpValue);
    }
}
