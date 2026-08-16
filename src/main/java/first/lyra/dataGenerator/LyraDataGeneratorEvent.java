package first.lyra.dataGenerator;

import first.lyra.Lyra;
import first.lyra.dataGenerator.provider.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Lyra.MODID)
public class LyraDataGeneratorEvent {

    @SubscribeEvent
    public static void gatherDataClient(GatherDataEvent.Client event) {
        event.createProvider(output -> new LyraLanguageProvider(output, Lyra.MODID, "en_us"));
        event.createProvider(output -> new LyraLanguageProvider(output, Lyra.MODID, "zh_cn"));
        event.createProvider(LyraItemModelProvider::new);
    }

    @SubscribeEvent
    public static void gatherDataServer(GatherDataEvent.Server event) {
        event.createProvider(LyraRecipeProvider.Runner::new);
        event.createBlockAndItemTags(LyraBlockTagsProvider::new, LyraItemTagsProvider::new);
    }
}
