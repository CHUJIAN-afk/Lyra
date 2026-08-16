package first.lyra.dataGenerator.provider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 配方数据生成：输出宿主 mod 通过 {@code LyraItemRegisterBuilder.recipe()} / {@code recipeWithLookup()} 收集的配方。
 * <p>
 * 26.2: RecipeProvider 构造改为 (HolderLookup.Provider, RecipeOutput),子类需走嵌套 Runner。
 * </p>
 */
public class LyraRecipeProvider extends RecipeProvider {

    public static final List<Consumer<RecipeOutput>> RecipeGenerate = new ArrayList<>();

    /** 需要 HolderLookup 的配方回调（如 ShapedRecipeBuilder.shaped 构造）。 */
    public static final List<BiConsumer<HolderLookup.Provider, RecipeOutput>> RecipeGenerateWithLookup = new ArrayList<>();

    protected LyraRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        RecipeGenerate.removeIf(generate -> {
            generate.accept(this.output);
            return true;
        });
        RecipeGenerateWithLookup.removeIf(generate -> {
            generate.accept(this.registries, this.output);
            return true;
        });
    }

    public static class Runner extends RecipeProvider.Runner {

        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        @Override
        protected @NonNull RecipeProvider createRecipeProvider(HolderLookup.@NonNull Provider registries, @NonNull RecipeOutput output) {
            return new LyraRecipeProvider(registries, output);
        }

        @Override
        public @NonNull String getName() {
            return "Lyra Recipes";
        }
    }
}
