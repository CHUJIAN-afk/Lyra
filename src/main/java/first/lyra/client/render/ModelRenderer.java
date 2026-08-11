package first.lyra.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 模型渲染器。
 * <p>
 * 26.2: ItemRenderer 类移除,物品渲染改为 ItemModel → ItemStackRenderState → submit 管线。
 * </p>
 */
public final class ModelRenderer {

    private ModelRenderer() {
    }

    /**
     * 渲染指定物品 id 的模型。
     *
     * @param itemId      物品注册 id
     * @param poseStack   姿态栈
     * @param collector   提交节点收集器
     * @param packedLight 光照值
     */
    public static void renderModel(Identifier itemId, PoseStack poseStack, SubmitNodeCollector collector, int packedLight) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(itemId).map(Holder.Reference::value).orElse(Items.AIR));
        ItemStackRenderState renderState = new ItemStackRenderState();
        ItemModel model = minecraft.getModelManager().getItemModel(itemId);
        model.update(renderState, stack, minecraft.getItemModelResolver(), ItemDisplayContext.NONE, minecraft.level, null, 0);
        renderState.submit(poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY, 0);
    }
}
