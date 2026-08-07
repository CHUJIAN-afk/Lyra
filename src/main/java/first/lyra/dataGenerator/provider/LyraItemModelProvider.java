package first.lyra.dataGenerator.provider;

import first.lyra.Lyra;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 物品模型数据生成：输出宿主 mod 通过 {@code LyraItemRegisterBuilder.itemModel()} 收集的模型。
 */
public class LyraItemModelProvider extends ItemModelProvider {

    public static final Map<ResourceLocation, BiConsumer<ResourceLocation, LyraItemModelProvider>> ItemModelGenerate = new HashMap<>();

    public LyraItemModelProvider(PackOutput packOutput, ExistingFileHelper existingFileHelper) {
        super(packOutput, Lyra.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        ItemModelGenerate.entrySet()
                .removeIf(entry -> {
                    ResourceLocation key = entry.getKey();
                    BiConsumer<ResourceLocation, LyraItemModelProvider> consumer = entry.getValue();
                    consumer.accept(key, this);
                    return true;
                });
    }
}
