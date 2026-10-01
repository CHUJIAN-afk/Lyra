package first.lyra.mixin.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Sodium 兼容(由 {@code LyraMixinConfigPlugin} 门控):SMOOTH(平滑光照开)路径的角点光照混合。
 * <p>
 * Sodium 把原版 {@code ModelBlockRenderer.Cache.blend} 的"零值取中心值"规则换成了 {@code minNonZero}:
 * 只要角点四邻里有 0,就把 0 抬到该组里的最小非零值。原版方块光每格只差 1 级,这个抬升很小,所以看不出来;
 * 但动态光每格衰减约 1.94 级,光斑最外一圈会被整体抬高(实测色调 0.0234 而原版规则只有 0.0075,约 3 倍),
 * 再往外一格没有动态光就直接掉到 0 —— 这就是"平滑光照开时动态光边缘硬跳"的原因。
 * </p>
 * <p>
 * 这里恢复原版规则(零值取中心的第 4 个值,再四角平均),让 Sodium 的 SMOOTH 光照与原版语义一致,动态光边缘正常渐隐。
 * FLAT(平滑光照关)路径不经过本方法,所以本来就正常。
 * </p>
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.light.smooth.AoFaceData")
public class SodiumAoFaceDataMixin {

    @Overwrite
    private static int calculateCornerBrightness(int a, int b, int c, int d, boolean aem, boolean bem, boolean cem, boolean dem) {
        if (a == 0) {
            a = d;
        }
        if (b == 0) {
            b = d;
        }
        if (c == 0) {
            c = d;
        }
        if (aem) {
            a = a & 0xFF0000 | 240;
        }
        if (bem) {
            b = b & 0xFF0000 | 240;
        }
        if (cem) {
            c = c & 0xFF0000 | 240;
        }
        if (dem) {
            d = d & 0xFF0000 | 240;
        }
        return a + b + c + d >> 2 & 0x00FF00FF;
    }
}
