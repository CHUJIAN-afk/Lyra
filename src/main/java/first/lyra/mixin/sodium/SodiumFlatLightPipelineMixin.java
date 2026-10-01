package first.lyra.mixin.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Sodium 兼容(由 {@code LyraMixinConfigPlugin} 门控):{@code FlatLightPipeline.getOffsetLightmap}
 * 是第二处组装 lightmap 坐标的地方。
 * <p>
 * 原实现 {@code LightTexture.pack(max(BL, LU), SL)} = {@code BL << 4 | SL << 20},前提是 BL 为 4bit 级别;
 * 经 {@link SodiumLightDataAccessMixin} 加宽后 {@code max(unpackBL, unpackLU)} 已是 8bit smooth 值,
 * 再左移 4 位会超出 lightmap,所以这里按 {@code smooth | sky << 20} 组装。
 * </p>
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.model.light.flat.FlatLightPipeline")
public class SodiumFlatLightPipelineMixin {

    @Redirect(
            method = "getOffsetLightmap",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightTexture;pack(II)I")
    )
    private static int smoothLightmap(int block, int sky) {
        return block | sky << 20;
    }
}
