package first.lyra.client.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class HdrTarget extends TextureTarget {

    public HdrTarget(int width, int height, boolean depth) {
        super(width, height, depth, Minecraft.ON_OSX);
    }

    @Override
    public void createBuffers(int width, int height, boolean clearError) {
        super.createBuffers(width, height, clearError);
        GlStateManager._bindTexture(getColorTextureId());
        GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, width, height, 0, GL11.GL_RGBA, GL11.GL_FLOAT, null);
        GlStateManager._bindTexture(0);
        setFilterMode(GL11.GL_LINEAR);
        setClearColor(0, 0, 0, 0);
        clear(clearError);
    }

    public static HdrTarget resize(HdrTarget target, int width, int height, boolean depth) {
        if (target == null || target.width != width || target.height != height) {
            RenderState state = new RenderState();
            RenderSystem.disableScissor();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            if (target == null) {
                target = new HdrTarget(width, height, depth);
            } else {
                target.resize(width, height, Minecraft.ON_OSX);
            }
            state.restore();
        }
        return target;
    }
}
