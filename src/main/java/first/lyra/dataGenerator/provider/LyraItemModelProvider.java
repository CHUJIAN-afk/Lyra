package first.lyra.dataGenerator.provider;

import first.lyra.Lyra;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
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
}
