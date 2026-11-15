package first.lyra.client.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
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
        setClearColor(0, 0, 0, 0);
        clear(clearError);
    }

    public static HdrTarget resize(HdrTarget target, int width, int height, boolean depth) {
        if (target == null) {
            return new HdrTarget(width, height, depth);
        }
        if (target.width != width || target.height != height) {
            target.resize(width, height, Minecraft.ON_OSX);
        }
        return target;
    }
}
