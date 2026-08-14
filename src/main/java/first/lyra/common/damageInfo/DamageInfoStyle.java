package first.lyra.common.damageInfo;

import net.minecraft.resources.ResourceLocation;

/**
 * 伤害数字样式定义，由 JSON 数据包驱动。
 * <p>
 * 贴图布局固定：0-9 顺序排列后追加一个小数点贴图（索引 10），
 * UV = glyphIndex / GLYPH_COUNT。
 * </p>
 *
 * @param damageType    伤害类型 ID（如 "lyra:minion"）
 * @param texture       贴图资源路径
 * @param textureWidth  完整贴图像素宽
 * @param textureHeight 完整贴图像素高
 * @param glyphSpacing  字符间距（像素）
 * @param renderSize    世界中单字符渲染尺寸
 * @param maxLife       生命周期（tick）
 * @param color         普通伤害颜色（RGB int）
 * @param criticalColor 暴击伤害颜色（RGB int）
 */
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

    /** 贴图中字符总数：0-9 共 10 个数字 + 1 个小数点（位于末尾，索引 10） */
    public static final int GLYPH_COUNT = 11;

    /** 单字符在贴图中的像素宽度 */
    public int glyphPixelWidth() {
        return textureWidth / GLYPH_COUNT;
    }

    /** 单字符在贴图中的像素高度 */
    public int glyphPixelHeight() {
        return textureHeight;
    }

    /**
     * 将十六进制颜色字符串解析为 RGB int。
     * <p>
     * 输入格式："FF3D00" → 输出 0xFF3D00
     * </p>
     *
     * @param hex 6 位十六进制颜色字符串（不含 # 前缀）
     * @return RGB int
     * @throws IllegalArgumentException 如果格式无效
     */
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
