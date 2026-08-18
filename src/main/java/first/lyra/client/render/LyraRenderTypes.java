package first.lyra.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import first.lyra.Lyra;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Lyra 渲染类型工厂（1.21.1，26.2 对齐）。
 * <p>
 * 模型管线：items atlas + entity translucent emissive（NEW_ENTITY 格式，26.2 行为对齐：
 * 半透明正确混合、不写深度 COLOR_WRITE、无 sortOnUpload、FULL_BRIGHT 全亮）。
 * </p>
 */
public class LyraRenderTypes {

    public static RenderType getTrail() {
        return RenderType.entityTranslucentEmissive(Lyra.rl("textures/trail.png"));
    }

    public static RenderType getModel() {
        return RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS);
    }
}
