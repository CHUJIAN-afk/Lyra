package first.lyra.mixin;

import first.lyra.common.particle.genericParticle.GenericParticle;
import first.lyra.common.particle.genericParticle.GenericParticleRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Unique
    private final GenericParticleRenderer lyra$genericParticles = new GenericParticleRenderer();

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void lyra$addGenericParticle(Particle particle, CallbackInfo ci) {
        if (particle.getClass() == GenericParticle.class) {
            lyra$genericParticles.add((GenericParticle) particle);
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void lyra$tickGenericParticles(CallbackInfo ci) {
        lyra$genericParticles.tick();
    }

    @Inject(method = "clearParticles", at = @At("HEAD"))
    private void lyra$clearGenericParticles(CallbackInfo ci) {
        lyra$genericParticles.clear();
    }

    @Inject(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightTexture;turnOffLightLayer()V"))
    private void lyra$renderGenericParticles(LightTexture lightTexture, Camera camera, float partialTick, Frustum frustum, Predicate<ParticleRenderType> predicate, CallbackInfo ci) {
        if (predicate.test(ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT)) {
            lyra$genericParticles.render(camera, partialTick, frustum, predicate.test(ParticleRenderType.PARTICLE_SHEET_OPAQUE));
        }
    }
}
