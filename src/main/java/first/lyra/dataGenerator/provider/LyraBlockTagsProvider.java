package first.lyra.dataGenerator.provider;

import first.lyra.Lyra;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

/** 空的方块标签 provider，仅作为 {@link LyraItemTagsProvider} 的依赖。 */
public class LyraBlockTagsProvider extends BlockTagsProvider {

    // 26.2: 构造器移除 ExistingFileHelper
    public LyraBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, Lyra.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
    }
}
