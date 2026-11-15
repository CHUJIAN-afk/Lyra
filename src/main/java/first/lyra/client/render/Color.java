package first.lyra.client.render;

import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

public record Color(int argbInt) {

    public static final Color WHITE = new Color(-1);

    public static Color ofOpaque(int rgb) {
        return new Color(rgb | 0xFF000000);
    }

    public static Color ofARGB(float alpha, float red, float green, float blue) {
        return new Color(FastColor.ARGB32.color(Mth.clamp((int) (alpha * 255), 0, 255),
                Mth.clamp((int) (red * 255), 0, 255), Mth.clamp((int) (green * 255), 0, 255), Mth.clamp((int) (blue * 255), 0, 255)));
    }

    public int getColor() {
        return argbInt;
    }

    public int getAlpha() {
        return FastColor.ARGB32.alpha(argbInt);
    }

    public float getAlphaFloat() {
        return getAlpha() / 255f;
    }

    public int getRed() {
        return FastColor.ARGB32.red(argbInt);
    }

    public float getRedFloat() {
        return getRed() / 255f;
    }

    public int getGreen() {
        return FastColor.ARGB32.green(argbInt);
    }

    public float getGreenFloat() {
        return getGreen() / 255f;
    }

    public int getBlue() {
        return FastColor.ARGB32.blue(argbInt);
    }

    public float getBlueFloat() {
        return getBlue() / 255f;
    }
}
