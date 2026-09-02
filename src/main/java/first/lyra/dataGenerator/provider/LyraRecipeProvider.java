package first.lyra.dataGenerator.provider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 配方数据生成：输出宿主 mod 通过 {@code LyraItemRegisterBuilder.recipe()} / {@code recipeWithLookup()} 收集的配方。
 */
public class LyraRecipeProvider extends RecipeProvider {

    public static final List<Consumer<RecipeOutput>> RecipeGenerate = new ArrayList<>();

    /** 需要 HolderLookup 的配方回调（如 ShapedRecipeBuilder 等需要 HolderGetter 的构建器使用）。 */
    public static final List<BiConsumer<HolderLookup.Provider, RecipeOutput>> RecipeGenerateWithLookup = new ArrayList<>();

    private final CompletableFuture<HolderLookup.Provider> registriesFuture;

    public LyraRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
        this.registriesFuture = registries;
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput output) {
        RecipeGenerate.removeIf(generate -> {
            generate.accept(output);
            return true;
        });
        HolderLookup.Provider registries = registriesFuture.join();
        RecipeGenerateWithLookup.removeIf(generate -> {
            generate.accept(registries, output);
            return true;
        });
    }
}
