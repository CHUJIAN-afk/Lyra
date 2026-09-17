package first.lyra.client;

import first.lyra.client.dynamicLight.DynamicLightDispatcher;
import first.lyra.client.render.AttachmentEntityRenderDispatcher;
import first.lyra.client.tooltip.TooltipHandler;
import first.lyra.common.damageInfo.DamageInfoRenderDispatcher;
import first.lyra.mixin.LevelRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.mesdag.portlib.event.PortEventHandler;

public final class ClientGameEvent {
    public static void init() {
        PortEventHandler.addListener(ClientGameEvent::itemTooltip);
        PortEventHandler.addListener(ClientGameEvent::renderLevel);
    }

    private static void itemTooltip(ItemTooltipEvent event) {
        TooltipHandler.handler(event);
    }

    private static void renderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            if (level == null) {
                return;
            }
            MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
            float partialTick = event.getPartialTick();
            AttachmentEntityRenderDispatcher.render(level, event.getCamera(), event.getPoseStack(), bufferSource, partialTick);
            DamageInfoRenderDispatcher.render(level, event.getCamera(), bufferSource, partialTick);
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            DynamicLightDispatcher.update((LevelRendererAccessor) event.getLevelRenderer());
        }
    }
}
