package first.lyra.client.render;

import first.lyra.Lyra;
import first.lyra.client.dynamicLight.DynamicLightDispatcher;
import first.lyra.common.damageInfo.DamageInfoRenderDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * 世界渲染挂接。
 * <p>
 * 26.2: LevelRenderer.renderLevel/getLightColor/setSectionDirty 已移除,
 * NeoForge 提供 {@link SubmitCustomGeometryEvent} 作为模组自定义几何提交入口,
 * 替代原 LevelRendererMixin 的注入。
 * </p>
 */
@EventBusSubscriber(modid = Lyra.MODID, value = Dist.CLIENT)
public class LevelRenderHandler {

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        Vec3 camPos = event.getLevelRenderState().cameraRenderState.pos;
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        AttachmentEntityRenderDispatcher.render(level.players(), camPos, event.getPoseStack(), event.getSubmitNodeCollector(), partialTick);
        DamageInfoRenderDispatcher.render(level, camPos, event.getSubmitNodeCollector(), partialTick);
        DynamicLightDispatcher.update();
    }
}
