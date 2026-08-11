package first.lyra.dataGenerator.provider;

import first.lyra.Lyra;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 物品模型数据生成：输出宿主 mod 通过 {@code LyraItemRegisterBuilder.itemModel()} 收集的模型。
 */
public class LyraItemModelProvider extends ItemModelProvider {

    public static final Map<Identifier, BiConsumer<Identifier, LyraItemModelProvider>> ItemModelGenerate = new HashMap<>();

    public LyraItemModelProvider(PackOutput packOutput, ExistingFileHelper existingFileHelper) {
        this(packOutput, existingFileHelper, Lyra.MODID);
    }

    /** 宿主 mod 使用:输出到宿主命名空间(模型 JSON 与物品 ID 命名空间需一致)。 */
    public LyraItemModelProvider(PackOutput packOutput, ExistingFileHelper existingFileHelper, String modid) {
        super(packOutput, modid, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        ItemModelGenerate.entrySet()
                .removeIf(entry -> {
                    Identifier key = entry.getKey();
                    BiConsumer<Identifier, LyraItemModelProvider> consumer = entry.getValue();
                    consumer.accept(key, this);
                    return true;
                });
    }
}
