package first.lyra.dataGenerator.provider;

import first.lyra.Lyra;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/** 空的方块标签 provider，仅作为 {@link LyraItemTagsProvider} 的依赖。 */
public class LyraBlockTagsProvider extends BlockTagsProvider {

    public LyraBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        this(output, lookupProvider, existingFileHelper, Lyra.MODID);
    }

    /** 宿主 mod 使用:输出到宿主命名空间。 */
    public LyraBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper, String modid) {
        super(output, lookupProvider, modid, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
    }
}
