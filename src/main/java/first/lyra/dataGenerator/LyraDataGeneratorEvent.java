package first.lyra.dataGenerator;

import first.lyra.Lyra;
import first.lyra.dataGenerator.provider.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Lyra 数据生成挂载。
 * <p>
 * 输出宿主 mod 通过 {@code LyraItemRegisterBuilder} 收集的动态条目
 * （语言/配方/标签/物品模型），以及 {@code LyraLanguageRegister.init()} 的库静态语言条目。
 * </p>
 * <p>
 * 26.2: {@link GatherDataEvent} 拆分为 Server/Client 两个事件,includeClient/includeServer
 * 与 ExistingFileHelper 已移除。
 * </p>
 */
// 26.2: @EventBusSubscriber 移除 bus 参数,FML 自动注册
@EventBusSubscriber(modid = Lyra.MODID)
public class LyraDataGeneratorEvent {

    @SubscribeEvent
    public static void gatherDataClient(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        // 语言（init 静态条目 + 宿主动态条目）
        generator.addProvider(event.includeDev(), new LyraLanguageProvider(packOutput, Lyra.MODID, "en_us"));
        generator.addProvider(event.includeDev(), new LyraLanguageProvider(packOutput, Lyra.MODID, "zh_cn"));
        // 物品模型
        generator.addProvider(event.includeDev(), new LyraItemModelProvider(packOutput));
    }

    @SubscribeEvent
    public static void gatherDataServer(GatherDataEvent.Server event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        // 合成表
        generator.addProvider(event.includeDev(), new LyraRecipeProvider.Runner(packOutput, lookupProvider));
        // 物品标签（依赖空方块标签 provider）
        LyraBlockTagsProvider blockTagsProvider = new LyraBlockTagsProvider(packOutput, lookupProvider);
        generator.addProvider(event.includeDev(), blockTagsProvider);
        generator.addProvider(event.includeDev(), new LyraItemTagsProvider(packOutput, lookupProvider));
    }
}
