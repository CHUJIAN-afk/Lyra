package first.lyra.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Sodium 兼容(由 {@code LyraMixinConfigPlugin} 门控:未装 Sodium 时整组不应用)。
 * <p>
 * Sodium 的下游管线本来就是按 8bit 设计的:{@code AoFaceData} 用 {@code lm & 255} 取 block 通道、
 * 角点混合用 {@code >> 2 & 0x00FF00FF},{@code CompactChunkVertex.encodeLight} 把 block/sky 各写成一个
 * byte 并 {@code clamp(v, 8, 248)} —— 这个 clamp 区间正是 26.2 的 smooth 值 {@code level * 16 + 8}
 * (8 = 半纹素偏移)。也就是说 Sodium 的 shader 端 {@code texture(u_LightTex, uv / 256)} 只要拿到
 * 8bit smooth 值就是平滑的。
 * </p>
 * <p>
 * 断链点在 {@code LightDataAccess.compute}:它拿到 {@code LevelRenderer.getLightColor} 之后立刻
 * {@code LightTexture.block(...)} 把 block 砍成 4bit 级别,{@code packBL} 也只存 4bit;之后
 * {@code getLightmap} 用 {@code LightTexture.pack} 重新 {@code << 4} 组装,小数与半纹素偏移全丢,
 * 于是每个顶点只剩 16 个整数级 → 边缘跳变。
 * </p>
 * <p>
 * 这里把 light word 的字段加宽,让 8bit smooth 值原样传下去:
 * <pre>
 * bit  0-7   block 8bit(smooth 值,0-248)
 * bit  8-11  sky 4bit
 * bit 12-15  luminance 4bit
 * bit 16-27  AO 12bit(从 16bit 让出 4bit;1.0 饱和到 4095/4096,亮度差 0.024%)
 * bit 28-31  EM / OP / FO / FC(不变)
 * </pre>
 * {@code unpackLU} 改为返回与 block 同尺度的 8bit 值,这样 {@code getLightmap} 与
 * {@code FlatLightPipeline} 里两处 {@code Math.max(BL, LU)} 都不需要额外分支。
 * </p>
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess")
public class SodiumLightDataAccessMixin {

    @Overwrite
    public static int packBL(int blockLight) {
        return blockLight & 0xFF;
    }

    @Overwrite
    public static int unpackBL(int word) {
        return word & 0xFF;
    }

    @Overwrite
    public static int packSL(int skyLight) {
        return (skyLight & 15) << 8;
    }

    @Overwrite
    public static int unpackSL(int word) {
        return word >>> 8 & 15;
    }

    @Overwrite
    public static int packLU(int luminance) {
        return (luminance & 15) << 12;
    }

    /** 返回与 block 同尺度(8bit)的发光值,两处 {@code Math.max(BL, LU)} 因此可以直接比较 */
    @Overwrite
    public static int unpackLU(int word) {
        return (word >>> 12 & 15) << 4;
    }

    @Overwrite
    public static int packAO(float ao) {
        return Math.min(4095, (int) (ao * 4096.0F)) << 16;
    }

    @Overwrite
    public static float unpackAO(int word) {
        return (word >>> 16 & 0xFFF) * 2.4414062E-4F;
    }

    /** 顶点光照值:低 8 位 = block smooth 值(即 lightmap 的 u 坐标),bit20-23 = sky */
    @Overwrite
    public static int getLightmap(int word) {
        return Math.max(unpackBL(word), unpackLU(word)) | unpackSL(word) << 20;
    }

    /** {@code LightTexture.block} 会把低 4 位(1/16 级小数)砍掉,这里直接取低 8 位 smooth 值 */
    @Redirect(
            method = "compute",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightTexture;block(I)I")
    )
    private static int smoothBlock(int light) {
        return light & 0xFF;
    }

    /**
     * emissive 分支绕过了 {@code LevelRenderer.getLightColor},直接取 4bit 级别,这里补成同一尺度。
     * {@code ordinal = 0} 即 {@code bl = level.getBrightness(LightLayer.BLOCK, pos)}(ordinal 1 是 sky,保持 4bit)。
     */
    @ModifyExpressionValue(
            method = "compute",
            at = @At(
                    value = "INVOKE",
                    ordinal = 0,
                    target = "Lnet/minecraft/world/level/BlockAndTintGetter;getBrightness(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/BlockPos;)I"
            )
    )
    private static int scaleEmission(int original) {
        return original << 4;
    }
}
