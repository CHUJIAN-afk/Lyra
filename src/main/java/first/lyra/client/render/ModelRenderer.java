package first.lyra.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * 模型渲染器，使用缓存的 BakedQuad + 内联顶点变换。
 * <p>
 * 相比每帧 getModel+getQuads+putBulkData：
 * <ul>
 *   <li>缓存 BakedQuad 列表，避免每帧重新查询</li>
 *   <li>内联顶点变换，跳过 putBulkData 内部的 color/sprite 分支判断</li>
 * </ul>
 * </p>
 */
public final class ModelRenderer {

    private ModelRenderer() {
    }

    public static void renderModel(ModelResourceLocation modelLocation, PoseStack poseStack, MultiBufferSource bufferSource) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemRenderer renderer = minecraft.getItemRenderer();
        ModelManager modelManager = minecraft.getModelManager();
        renderer.renderModelLists(modelManager.getModel(modelLocation), ItemStack.EMPTY, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, poseStack, bufferSource.getBuffer(Sheets.translucentItemSheet()));
    }
}
