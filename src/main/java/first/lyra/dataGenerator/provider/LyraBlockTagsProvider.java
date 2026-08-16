package first.lyra.dataGenerator.provider;

import first.lyra.Lyra;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class LyraBlockTagsProvider extends BlockTagsProvider {

    public LyraBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, Lyra.MODID);
    }

    @Override
    protected void addTags(HolderLookup.@NonNull Provider provider) {
    }
}
