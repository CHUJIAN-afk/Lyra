package first.lyra.dataGenerator.provider;

import first.lyra.Lyra;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 物品模型数据生成：输出宿主 mod 通过 {@code LyraItemRegisterBuilder.itemModel()} 收集的模型。
 * <p>
 * 26.2: NeoForge ItemModelProvider 移除 → vanilla {@link ModelProvider};
 * basicItem/handheldItem → {@link ItemModelUtils#plainModel} + {@link ItemModelGenerators#itemModelOutput}。
 * </p>
 */
public class LyraItemModelProvider extends ModelProvider {

    public static final Map<Identifier, BiConsumer<Identifier, LyraItemModelProvider>> ItemModelGenerate = new HashMap<>();

    private ItemModelGenerators itemModels;

    public LyraItemModelProvider(PackOutput packOutput) {
        super(packOutput, Lyra.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        this.itemModels = itemModels;
        ItemModelGenerate.entrySet()
                .removeIf(entry -> {
                    Identifier key = entry.getKey();
                    BiConsumer<Identifier, LyraItemModelProvider> consumer = entry.getValue();
                    consumer.accept(key, this);
                    return true;
                });
    }

    /** 替代旧 basicItem:item/generated 基础模型 */
    public void basicItem(Identifier location) {
        itemModels.itemModelOutput.accept(BuiltInRegistries.ITEM.get(location).map(Holder.Reference::value).orElse(null),
                ItemModelUtils.plainModel(Identifier.withDefaultNamespace("item/generated")));
    }

    /** 替代旧 handheldItem:item/handheld 基础模型 */
    public void handheldItem(Identifier location) {
        itemModels.itemModelOutput.accept(BuiltInRegistries.ITEM.get(location).map(Holder.Reference::value).orElse(null),
                ItemModelUtils.plainModel(Identifier.withDefaultNamespace("item/handheld")));
    }
}
