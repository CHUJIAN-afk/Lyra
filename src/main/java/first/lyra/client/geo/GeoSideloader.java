package first.lyra.client.geo;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Geo 外挂渲染器，独立于 GeckoLib 的 Entity/Item/BlockEntity 渲染体系。
 * <p>
 * 使用自定义 {@link GeoAnimationSampler} 直接采样关键帧写入 {@link GeoBone#frameSnapshot}，
 * 完全绕开 GeckoLib 的 {@code AnimationController}/{@code handleAnimations} 管线。
 * <p>
 * 渲染器无跨帧状态，每帧创建新实例即可。动画进度由调用方精确注入，
 * 不同实例间互不污染。渲染前后均 reset 共享 bone 状态，防止交叉污染。
 * <p>
 * 26.2: GeoRenderer 接口重构为 3 泛型 + GeoRenderState + SubmitNodeCollector 驱动;
 * GeoBone 不可变,骨骼姿态写入 frameSnapshot。
 * <p>
 * 使用方式：
 * <pre>{@code
 * // 每帧创建实例，设置动画与进度后渲染
 * GeoSideloader.create(Lyra.id("laser_minigun"))
 *     .setAnimation("shooting", tickProgress)
 *     .render(poseStack, collector, cameraState, partialTick, packedLight);
 * }</pre>
 */
public class GeoSideloader implements GeoRenderer<DummyGeoAnimatable, Void, GeoRenderState> {

    private static final DummyGeoAnimatable DUMMY = new DummyGeoAnimatable();

    /**
     * 当前使用的 Geo 模型定义
     */
    private final GeoAttachmentModel geoModel;
    /**
     * 自定义关键帧采样器
     */
    private final GeoAnimationSampler sampler;
    /**
     * 当前帧要播放的动画名
     */
    private String currentAnimationName;
    /**
     * 当前帧动画进度（tick 域）
     */
    private float progress = 0f;
    /**
     * 本帧需要隐藏的骨骼名集合
     */
    private final Set<String> hiddenBones = new HashSet<>();

    private GeoSideloader(GeoAttachmentModel geoModel) {
        this.geoModel = geoModel;
        this.sampler = new GeoAnimationSampler(geoModel.getAnimationResource(DUMMY));
    }

    /**
     * 创建与指定资源位置绑定的 Sideloader 实例。
     * <p>
     * 无缓存、无跨帧状态，每帧调用即可。
     *
     * @param location 模型资源定位，命名空间+路径对应 geo/texture/animation 文件
     * @return 全新的 Sideloader 实例
     */
    public static GeoSideloader create(Identifier location) {
        return new GeoSideloader(new GeoAttachmentModel(location));
    }

    // ===================== 外挂 API =====================

    /**
     * 设置当前帧要播放的动画及进度。
     * 必须在 {@link #render} 之前调用。
     *
     * @param animationName animation.json 中定义的动画名称
     * @param progress      动画进度（tick 域，0=起始，递增推进）
     */
    public GeoSideloader setAnimation(String animationName, float progress) {
        this.currentAnimationName = animationName;
        this.progress = progress;
        return this;
    }

    /**
     * 隐藏指定骨骼（包含其子骨骼）。必须在 {@link #render} 之前调用。
     *
     * @param boneName .geo.json 中定义的骨骼名称
     */
    public GeoSideloader hideBone(String... boneName) {
        this.hiddenBones.addAll(Arrays.asList(boneName));
        return this;
    }

    /**
     * 执行一帧渲染，流程：
     * <ol>
     *   <li>构建渲染状态并采样动画写入 frameSnapshot（纯函数，无跨帧状态）</li>
     *   <li>应用骨骼可见性</li>
     *   <li>经 {@link #performRenderPass} 提交几何到 SubmitNodeCollector</li>
     *   <li>Reset 所有 bone（防止交叉污染）</li>
     * </ol>
     */
    @SuppressWarnings("all")
    public void render(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState, float partialTick, int packedLight) {
        GeoModel<DummyGeoAnimatable> model = getGeoModel();
        GeoRenderState renderState = createRenderState(DUMMY, null);
        fillRenderState(DUMMY, null, renderState, partialTick);
        BakedGeoModel bakedModel = model.getBakedModel(model.getModelResource(renderState));

        // 1. 采样动画写入 bone.frameSnapshot
        if (currentAnimationName != null) {
            sampler.sample(currentAnimationName, progress, bakedModel);
        }

        // 2. 应用骨骼可见性
        for (String boneName : hiddenBones) {
            bakedModel.getBone(boneName).ifPresent(bone -> {
                if (bone.frameSnapshot == null) {
                    bone.frameSnapshot = BoneSnapshot.create(bone);
                }
                bone.frameSnapshot.skipRender(true);
            });
        }

        // 3. 提交几何（GeoRenderer 默认驱动链:submitRenderTasks 遍历骨骼渲染）
        performRenderPass(renderState, poseStack, collector, cameraState);

        // 4. Reset bone（防止共享的 GeoBone 脏状态影响下一个渲染者）
        resetBones(bakedModel);
    }

    // ===================== Bone 重置 =====================

    /**
     * 将 BakedGeoModel 中所有骨骼的 frameSnapshot 置空，恢复默认姿态，
     * 防止共享的 GeoBone 对象被不同渲染实例交叉污染。
     */
    private void resetBones(BakedGeoModel bakedModel) {
        for (GeoBone bone : bakedModel.topLevelBones()) {
            resetBoneRecursive(bone);
        }
    }

    private void resetBoneRecursive(GeoBone bone) {
        bone.frameSnapshot = null;
        for (GeoBone child : bone.children()) {
            resetBoneRecursive(child);
        }
    }

    // ===================== GeoRenderer 接口实现 =====================

    @Override
    public GeoModel<DummyGeoAnimatable> getGeoModel() {
        return this.geoModel;
    }

    @Override
    public int getRenderColor(DummyGeoAnimatable animatable, @Nullable Void relatedObject, float partialTick) {
        return 0xFFFFFFFF;
    }

    @Override
    public int getPackedOverlay(DummyGeoAnimatable animatable, @Nullable Void relatedObject, float partialTick, float deltaTicks) {
        return OverlayTexture.NO_OVERLAY;
    }

    @Override
    public GeoRenderState createRenderState(DummyGeoAnimatable animatable, @Nullable Void relatedObject) {
        return new GeoRenderState.Impl();
    }

    @Override
    public void addRenderData(DummyGeoAnimatable animatable, @Nullable Void relatedObject, GeoRenderState renderState, float partialTick) {
    }

    @Override
    public void setMolangQueryValues(DummyGeoAnimatable animatable, @Nullable Void relatedObject, GeoRenderState renderState, float partialTick) {
    }

    @Override
    public void fireCompileRenderLayersEvent() {
    }

    @Override
    public void fireCompileRenderStateEvent(DummyGeoAnimatable animatable, @Nullable Void relatedObject, GeoRenderState renderState, float partialTick) {
    }

    @Override
    public boolean firePreRenderEvent(RenderPassInfo<GeoRenderState> renderPassInfo, SubmitNodeCollector renderTasks) {
        return true;
    }

    @Override
    public @NotNull RenderType getRenderType(GeoRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }
}
