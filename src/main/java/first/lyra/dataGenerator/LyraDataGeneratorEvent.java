package first.lyra.dataGenerator;

import first.lyra.Lyra;
import first.lyra.dataGenerator.provider.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Lyra 数据生成挂载（由 {@code Lyra} 构造器显式注册到 mod 总线）。
 * <p>
 * 输出宿主 mod 通过 {@code LyraItemRegisterBuilder} 收集的动态条目
 * （语言/配方/标签/物品模型），以及 {@code LyraLanguageRegister.init()} 的库静态语言条目。
 * </p>
 */
@EventBusSubscriber(modid = Lyra.MODID)
public class LyraDataGeneratorEvent {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        // 语言（init 静态条目 + 宿主动态条目）
        generator.addProvider(event.includeClient(), new LyraLanguageProvider(packOutput, Lyra.MODID, "en_us"));
        generator.addProvider(event.includeClient(), new LyraLanguageProvider(packOutput, Lyra.MODID, "zh_cn"));
        // 物品模型
        generator.addProvider(event.includeClient(), new LyraItemModelProvider(packOutput, existingFileHelper));
        // 合成表
        generator.addProvider(event.includeServer(), new LyraRecipeProvider(packOutput, lookupProvider));
        // 物品标签（依赖空方块标签 provider）
        LyraBlockTagsProvider blockTagsProvider = new LyraBlockTagsProvider(packOutput, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTagsProvider);
        generator.addProvider(event.includeServer(), new LyraItemTagsProvider(packOutput, lookupProvider, blockTagsProvider.contentsGetter(), existingFileHelper));
    }
}
