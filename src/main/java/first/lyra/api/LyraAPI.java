package first.lyra.api;

import first.lyra.client.light.LyraLights;
import first.lyra.client.render.rendererHelper.FlowingLaserRendererHelper;
import first.lyra.common.network.CameraShakePayload;
import first.lyra.utils.ParticleHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public interface LyraAPI {

    static LyraHelper entity(Player player) {
        return LyraHelper.get(player);
    }

    static ParticleHelper particle(Level level) {
        return ParticleHelper.create(level);
    }

    static FlowingLaserRendererHelper flowingLaser() {
        return FlowingLaserRendererHelper.builder();
    }

    /** 仅服务端广播；range 为正数，单位为格；strength 为 0～1。 */
    static void shakeCamera(Level level, Vec3 pos, float range, float strength) {
        if (level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersNear(serverLevel, null, pos.x, pos.y, pos.z, range, new CameraShakePayload(pos, range, strength));
        }
    }

    static void light(Vec3 pos, int level) {
        light(pos, Math.clamp(level, 0, 15) / 15f);
    }

    static void light(Vec3 pos, float strength) {
        float value = Math.clamp(strength, 0f, 1f);
        LyraLights.point(pos, 0xFFFFFF, 8f * value, 7.75f * value);
    }

    /** RGB 为 0xRRGGBB；intensity/range 使用 Photon 单位，不经过原版亮度等级。 */
    static void light(Vec3 pos, int rgb, float intensity, float range) {
        LyraLights.point(pos, rgb, intensity, range);
    }

    /** 沿线放置点光；intensity 为每格线长的强度，spacing 为期望采样间距。 */
    static void lineLight(Vec3 start, Vec3 end, int rgb, float intensity, float range, double spacing) {
        LyraLights.line(start, end, rgb, intensity, range, spacing);
    }
}
