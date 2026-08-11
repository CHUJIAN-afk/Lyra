package first.lyra.client.geo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Geo 外挂渲染器，独立于 GeckoLib 的 Entity/Item/BlockEntity 渲染体系。
 * <p>
 * 使用自定义 {@link GeoAnimationSampler} 直接采样关键帧写入 {@link GeoBone}，
 * 完全绕开 GeckoLib 的 {@code AnimationController}/{@code handleAnimations} 管线。
 * <p>
 * 渲染器无跨帧状态，每帧创建新实例即可。动画进度由调用方精确注入，
 * 不同实例间互不污染。渲染前后均 reset 共享 bone 状态，防止交叉污染。
 * <p>
 * 使用方式：
 * <pre>{@code
 * // 每帧创建实例，设置动画与进度后渲染
 * GeoSideloader.create(Lyra.rl("laser_minigun"))
 *     .setAnimation("shooting", tickProgress)
 *     .render(poseStack, bufferSource, partialTick, packedLight);
 * }</pre>
 */
public class GeoSideloader implements GeoRenderer<DummyGeoAnimatable> {

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
     *   <li>Reset 所有 bone 到初始姿态（清除上一帧残留）</li>
     *   <li>采样动画写入 bone（纯函数，无跨帧状态）</li>
     *   <li>应用骨骼可见性</li>
     *   <li>绘制模型</li>
     *   <li>Reset 所有 bone 到初始姿态（防止交叉污染）</li>
     * </ol>
     */
    @SuppressWarnings("all")
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, float partialTick, int packedLight) {
        GeoModel<DummyGeoAnimatable> model = getGeoModel();
        BakedGeoModel bakedModel = model.getBakedModel(model.getModelResource(DUMMY, this));
        Identifier texture = model.getTextureResource(DUMMY, this);
        RenderType renderType = RenderType.entityTranslucent(texture);
        VertexConsumer buffer = bufferSource.getBuffer(renderType);

        poseStack.pushPose();

        // 1. Reset bone 到初始姿态（清除上一帧或其他渲染者残留的状态）
        resetBones(bakedModel);

        // 2. 采样动画写入 bone（纯函数，无跨帧状态）
        if (currentAnimationName != null) {
            sampler.sample(currentAnimationName, progress, bakedModel);
        }

        // 3. 应用骨骼可见性
        for (String boneName : hiddenBones) {
            model.getBone(boneName).ifPresent(bone -> bone.setHidden(true));
        }

        // 4. 绘制模型
        GeoRenderer.super.actuallyRender(poseStack, DUMMY, bakedModel, renderType, bufferSource, buffer, false, partialTick, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        // 5. Reset bone（防止共享的 GeoBone 脏状态影响下一个渲染者）
        resetBones(bakedModel);

        poseStack.popPose();
    }

    // ===================== Bone 重置 =====================

    /**
     * 将 BakedGeoModel 中所有骨骼的 rot/pos/scale 恢复到 initialSnapshot 值，
     * 并重置 hidden 状态，防止共享的 GeoBone 对象被不同渲染实例交叉污染。
     */
    private void resetBones(BakedGeoModel bakedModel) {
        for (GeoBone bone : bakedModel.topLevelBones()) {
            resetBoneRecursive(bone);
        }
    }

    private void resetBoneRecursive(GeoBone bone) {
        BoneSnapshot snapshot = bone.getInitialSnapshot();
        if (snapshot != null) {
            bone.setRotX(snapshot.getRotX());
            bone.setRotY(snapshot.getRotY());
            bone.setRotZ(snapshot.getRotZ());
            bone.setPosX(snapshot.getOffsetX());
            bone.setPosY(snapshot.getOffsetY());
            bone.setPosZ(snapshot.getOffsetZ());
            bone.setScaleX(snapshot.getScaleX());
            bone.setScaleY(snapshot.getScaleY());
            bone.setScaleZ(snapshot.getScaleZ());
        }
        bone.setHidden(bone.shouldNeverRender() == Boolean.TRUE);
        bone.resetStateChanges();
        for (GeoBone child : bone.getChildBones()) {
            resetBoneRecursive(child);
        }
    }

    // ===================== GeoRenderer 接口实现 =====================

    /**
     * 禁用默认 actuallyRender，渲染逻辑由 {@link #render} 自行管理
     */
    @Override
    public void actuallyRender(PoseStack poseStack, DummyGeoAnimatable animatable, BakedGeoModel model, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
    }

    @Override
    public GeoModel<DummyGeoAnimatable> getGeoModel() {
        return this.geoModel;
    }

    @Override
    public DummyGeoAnimatable getAnimatable() {
        return DUMMY;
    }

    @Override
    public @NotNull RenderType getRenderType(DummyGeoAnimatable animatable, Identifier texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void fireCompileRenderLayersEvent() {
    }

    @Override
    public boolean firePreRenderEvent(PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
        return true;
    }

    @Override
    public void firePostRenderEvent(PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
    }

    @Override
    public void updateAnimatedTextureFrame(DummyGeoAnimatable animatable) {
    }
}
