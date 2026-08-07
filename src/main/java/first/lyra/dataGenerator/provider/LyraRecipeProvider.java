package first.lyra.dataGenerator.provider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 配方数据生成：输出宿主 mod 通过 {@code LyraItemRegisterBuilder.recipe()} 收集的配方。
 */
public class LyraRecipeProvider extends RecipeProvider {

    public static final List<Consumer<RecipeOutput>> RecipeGenerate = new ArrayList<>();

    public LyraRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput output) {
        RecipeGenerate.removeIf(generate -> {
            generate.accept(output);
            return true;
        });
    }
}
