package first.lyra.client.geo;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.EasingType;
import software.bernie.geckolib.animation.keyframe.BoneAnimation;
import software.bernie.geckolib.animation.keyframe.Keyframe;
import software.bernie.geckolib.animation.keyframe.KeyframeStack;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.math.MathValue;
import software.bernie.geckolib.loading.object.BakedAnimations;

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

    private final ResourceLocation animationResource;

    /**
     * 缓存的动画解析结果（{@link Animation} 是不可变 record，安全复用）
     */
    private BakedAnimations cachedBakedAnimations;
    private String cachedAnimationName;
    private Animation cachedAnimation;

    public GeoAnimationSampler(ResourceLocation animationResource) {
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
        if (anim == null || anim.boneAnimations() == null)
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
        // GeckoLib 内置 LoopType 通过 == 比较引用即可
        return anim.loopType() == Animation.LoopType.LOOP;
    }

    // ===================== 动画解析 =====================

    @Nullable
    private Animation resolveAnimation(String animName) {
        BakedAnimations baked = GeckoLibCache.getBakedAnimations().get(this.animationResource);
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
        if (bone.getName().equals(name))
            return bone;
        for (GeoBone child : bone.getChildBones()) {
            GeoBone found = findBoneRecursive(child, name);
            if (found != null)
                return found;
        }
        return null;
    }

    // ===================== 通道采样 =====================

    private void sampleRotation(KeyframeStack<Keyframe<MathValue>> stack, double elapsed, GeoBone bone) {
        // 空列表 = 该通道无动画，保持 bone 初始值不动
        if (stack.xKeyframes().isEmpty() && stack.yKeyframes().isEmpty() && stack.zKeyframes().isEmpty())
            return;
        float x = sampleAxis(stack.xKeyframes(), elapsed);
        float y = sampleAxis(stack.yKeyframes(), elapsed);
        float z = sampleAxis(stack.zKeyframes(), elapsed);
        bone.updateRotation(x, y, z);
    }

    private void samplePosition(KeyframeStack<Keyframe<MathValue>> stack, double elapsed, GeoBone bone) {
        if (stack.xKeyframes().isEmpty() && stack.yKeyframes().isEmpty() && stack.zKeyframes().isEmpty())
            return;
        float x = sampleAxis(stack.xKeyframes(), elapsed);
        float y = sampleAxis(stack.yKeyframes(), elapsed);
        float z = sampleAxis(stack.zKeyframes(), elapsed);
        bone.updatePosition(x, y, z);
    }

    private void sampleScale(KeyframeStack<Keyframe<MathValue>> stack, double elapsed, GeoBone bone) {
        // scale 空列表时不写 bone——GeoBone 默认 scaleX/Y/Z = 1，写 0 会导致骨骼不可见
        if (stack.xKeyframes().isEmpty() && stack.yKeyframes().isEmpty() && stack.zKeyframes().isEmpty())
            return;
        float x = sampleAxis(stack.xKeyframes(), elapsed);
        float y = sampleAxis(stack.yKeyframes(), elapsed);
        float z = sampleAxis(stack.zKeyframes(), elapsed);
        bone.updateScale(x, y, z);
    }

    /**
     * 采样单轴的关键帧列表，返回在 elapsed tick 时刻的插值结果。
     * <p>
     * 关键帧按时间顺序排列，每个 keyframe 的 {@code length} 是该帧持续时间（tick），
     * 时间从 0 开始累加。
     */
    private float sampleAxis(List<Keyframe<MathValue>> keyframes, double elapsed) {
        if (keyframes.isEmpty())
            return 0f;

        double accumulatedStart = 0;

        for (int i = 0; i < keyframes.size(); i++) {
            Keyframe<MathValue> kf = keyframes.get(i);
            double kfEnd = accumulatedStart + kf.length();

            if (elapsed < kfEnd || i == keyframes.size() - 1) {
                // 找到了所在区间（或在最后一个 keyframe 之后）
                double currentTick = elapsed - accumulatedStart;
                double transitionLength = kf.length();

                if (transitionLength <= 0 || currentTick >= transitionLength) {
                    return (float) kf.endValue().get();
                }

                double lerpValue = currentTick / transitionLength;
                double easedLerp = applyEasing(kf, lerpValue);
                return (float) Mth.lerp(easedLerp, kf.startValue().get(), kf.endValue().get());
            }

            accumulatedStart = kfEnd;
        }

        // 理论上不会到达，但保险取末帧值
        return (float) keyframes.getLast().endValue().get();
    }

    /**
     * 应用 keyframe 的 easing 类型。
     */
    private double applyEasing(Keyframe<MathValue> kf, double lerpValue) {
        EasingType easingType = kf.easingType();
        Double easingArg = kf.easingArgs().isEmpty() ? null : kf.easingArgs().getFirst().get();
        return easingType.buildTransformer(easingArg).apply(lerpValue);
    }
}
