package first.lyra.dataGenerator.provider;

import com.google.gson.JsonObject;
import first.lyra.Lyra;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class LyraItemModelProvider extends ModelProvider {

    public static final Map<Identifier, BiConsumer<Identifier, LyraItemModelProvider>> ItemModelGenerate = new HashMap<>();

    private ItemModelGenerators itemModels;

    public LyraItemModelProvider(PackOutput packOutput) {
        super(packOutput, Lyra.MODID);
    }

    @Override
    protected void registerModels(@NonNull BlockModelGenerators blockModels, @NonNull ItemModelGenerators itemModels) {
        this.itemModels = itemModels;
        ItemModelGenerate.entrySet()
                .removeIf(entry -> {
                    Identifier key = entry.getKey();
                    BiConsumer<Identifier, LyraItemModelProvider> consumer = entry.getValue();
                    consumer.accept(key, this);
                    return true;
                });
    }

    public ItemModelGenerators getItemModels() {
        return itemModels;
    }

    /**
     * 替代旧 basicItem:item/generated 基础模型。
     * <p>
     * 26.2: 使用 vanilla 原生 {@link ItemModelGenerators#generateFlatItem},
     * 同时输出传统模型文件(models/item/{id}.json,parent+layer0 纹理)与
     * 物品模型描述(items/{id}.json,引用该模型),缺一不可。
     * </p>
     */
    public void basicItem(Identifier location) {
        BuiltInRegistries.ITEM.get(location).ifPresent(holder -> itemModels.generateFlatItem(holder.value(), ModelTemplates.FLAT_ITEM));
    }

    /** 替代旧 handheldItem:item/handheld 基础模型 */
    public void handheldItem(Identifier location) {
        BuiltInRegistries.ITEM.get(location).ifPresent(holder -> itemModels.generateFlatItem(holder.value(), ModelTemplates.FLAT_HANDHELD_ITEM));
    }

    /**
     * 输出 parent 指向外部模型的占位模型（如原版弩 {@code minecraft:item/crossbow}）。
     * <p>
     * 同时输出 26.2 items 描述文件（items/{id}.json 引用该模型）与传统模型文件
     * （models/item/{id}.json，内容仅 {@code {"parent": "..."}}）——与 generateFlatItem 的双输出一致。
     * </p>
     *
     * @param location 物品 id（summoner:xxx）
     * @param parent   占位模型 parent（如 minecraft:item/crossbow）
     */
    public void parentItem(Identifier location, Identifier parent) {
        Item item = BuiltInRegistries.ITEM.get(location).orElseThrow().value();
        // 模型 id 走 item/{path} 惯例（与 generateFlatItem 的 ModelLocationUtils.getModelLocation 一致）
        Identifier modelId = ModelLocationUtils.getModelLocation(item);
        // 无 requiredSlots 的自定义模板：空 TextureMapping → 输出仅 {"parent": "..."}
        // （不能用 ModelTemplates.CROSSBOW 等含 requiredSlots 的模板——空 mapping 会 NPE）
        ModelTemplate template = new ModelTemplate(Optional.of(parent), Optional.empty());
        template.create(modelId, new TextureMapping(), itemModels.modelOutput);
        itemModels.itemModelOutput.accept(item, ItemModelUtils.plainModel(modelId));
    }
}
