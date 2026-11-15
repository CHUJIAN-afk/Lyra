package first.lyra.api;

import first.lyra.client.light.DynamicLight;
import first.lyra.client.light.LyraLights;
import first.lyra.client.render.AfterimageContext;
import first.lyra.client.render.model.LyraModelRenderer;
import first.lyra.client.render.model.json.JsonModelRenderOptions;
import first.lyra.client.render.rendererHelper.LaserRendererHelper;
import first.lyra.client.render.rendererHelper.LightningRendererHelper;
import first.lyra.client.render.rendererHelper.SphereRendererHelper;
import first.lyra.client.tooltip.LyraTooltipHandler;
import first.lyra.client.tooltip.TooltipProvider;
import first.lyra.common.creativeTab.Section;
import first.lyra.common.creativeTab.SectionCreativeModeTab;
import first.lyra.common.network.CameraShakePayload;
import first.lyra.utils.ParticleHelper;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Lyra 对外唯一入口：每个功能只暴露一个方法，直接返回该功能的实现对象。
 */
public interface LyraAPI {

    static LyraHelper entity(Player player) {
        return LyraHelper.get(player);
    }

    static ParticleHelper particle(Level level) {
        return ParticleHelper.create(level);
    }

    /** 当帧光源，由调用方配置 point()/spot()。 */
    static DynamicLight light() {
        return LyraLights.next();
    }

    /** 仅服务端广播；range 为正数，单位为格；strength 为 0～1。 */
    static void shakeCamera(Level level, Vec3 pos, float range, float strength) {
        if (level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersNear(serverLevel, null, pos.x, pos.y, pos.z, range, new CameraShakePayload(pos, range, strength));
        }
    }

    static void tooltip(TooltipProvider provider) {
        LyraTooltipHandler.register(provider);
    }

    static CreativeModeTab.Builder creativeTab(Section... sections) {
        return SectionCreativeModeTab.builder()
                .withTabFactory(builder -> new SectionCreativeModeTab(builder, sections));
    }

    static LaserRendererHelper laser() {
        return LaserRendererHelper.builder();
    }

    static LightningRendererHelper lightning() {
        return LightningRendererHelper.builder();
    }

    static SphereRendererHelper sphere() {
        return SphereRendererHelper.builder();
    }

    static AfterimageContext afterimage() {
        return new AfterimageContext();
    }

    static JsonModelRenderOptions model(ResourceLocation location) {
        return LyraModelRenderer.json(location);
    }
}
