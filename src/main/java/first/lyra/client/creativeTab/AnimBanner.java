package first.lyra.client.creativeTab;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record AnimBanner(int frameHeight, int frameTime, int totalFrames) {

    private static final Map<ResourceLocation, long[]> animState = new HashMap<>();

    private static int currentFrame(AnimBanner banner, ResourceLocation texture, boolean playing) {
        long now = System.currentTimeMillis();
        long[] state = animState.computeIfAbsent(texture, k -> new long[]{0, now});
        if (playing) {
            state[0] += now - state[1];
        }
        state[1] = now;
        return (int) ((state[0] / (banner.frameTime * 50L)) % banner.totalFrames);
    }

    public static void blitAnimated(GuiGraphics graphics, ResourceLocation texture, AnimBanner info, int x, int y, int width, int mouseX, int mouseY, boolean hoverDriven) {
        boolean playing = !hoverDriven || mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + info.frameHeight;
        int frame = currentFrame(info, texture, playing);
        graphics.blit(texture, x, y, 0, frame * info.frameHeight, width, info.frameHeight, width, info.totalFrames * info.frameHeight);
    }
}
