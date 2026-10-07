package first.lyra.common.damageInfo;

import net.minecraft.resources.ResourceLocation;

public record DamageInfoStyle(
        String damageType,
        ResourceLocation texture,
        int textureWidth,
        int textureHeight,
        int glyphSpacing,
        float renderSize,
        int maxLife,
        int color,
        int criticalColor
) {

    public static final int GLYPH_COUNT = 11;

    public int glyphPixelWidth() {
        return textureWidth / GLYPH_COUNT;
    }

    public int glyphPixelHeight() {
        return textureHeight;
    }

    public static int parseHexColor(String hex) {
        if (hex == null || hex.length() != 6) {
            throw new IllegalArgumentException("Invalid hex color: " + hex + " (expected 6 hex digits)");
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex color: " + hex, e);
        }
    }
}
