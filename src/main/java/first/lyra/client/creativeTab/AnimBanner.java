package first.lyra.client.creativeTab;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public record AnimBanner(int frameHeight, int frameTime, int totalFrames) {

    private static final Map<Identifier, long[]> animState = new HashMap<>();

    private static int currentFrame(AnimBanner banner, Identifier texture, boolean playing) {
        long now = System.currentTimeMillis();
        long[] state = animState.computeIfAbsent(texture, k -> new long[]{0, now});
        if (playing) {
            state[0] += now - state[1];
        }
        state[1] = now;
        return (int) ((state[0] / (banner.frameTime * 50L)) % banner.totalFrames);
    }

    public static void blitAnimated(GuiGraphicsExtractor graphics, Identifier texture, AnimBanner info, int x, int y, int width, int mouseX, int mouseY, boolean hoverDriven) {
        boolean playing = !hoverDriven || mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + info.frameHeight;
        int frame = currentFrame(info, texture, playing);
        // 26.2: 9 参 blit 语义改变,改用带 RenderPipeline 的重载
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, frame * info.frameHeight, width, info.frameHeight, width, info.totalFrames * info.frameHeight);
    }
}
