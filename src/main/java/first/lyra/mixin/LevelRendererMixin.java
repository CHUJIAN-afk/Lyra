package first.lyra.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import first.lyra.client.dynamicLight.DynamicLightDispatcher;
import first.lyra.client.render.AttachmentEntityRenderDispatcher;
import first.lyra.common.damageInfo.DamageInfoRenderDispatcher;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    @Nullable
    private ClientLevel level;

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @WrapMethod(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I")
    private static int getLightColor(BlockAndTintGetter level, BlockState state, BlockPos pos, Operation<Integer> original) {
        return DynamicLightDispatcher.getDynamicLight(level, state, pos, original);
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderBuffers;bufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"
            )
    )
    private void renderLevel(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci, @Local PoseStack poseStack) {
        assert level != null;
        MultiBufferSource.BufferSource bufferSource = renderBuffers.bufferSource();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        AttachmentEntityRenderDispatcher.render(level.players(), camera, poseStack, bufferSource, partialTick);
        DamageInfoRenderDispatcher.render(level, camera, bufferSource, partialTick);
    }

    @Inject(method = "renderLevel", at = @At(value = "TAIL"))
    private void renderLevel(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        DynamicLightDispatcher.update((LevelRendererAccessor) this);
    }
}
