package first.lyra.dataGenerator.provider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 配方数据生成：输出宿主 mod 通过 {@code LyraItemRegisterBuilder.recipe()} 收集的配方。
 * <p>
 * 26.2: RecipeProvider 构造改为 (HolderLookup.Provider, RecipeOutput),子类需走嵌套 Runner。
 * </p>
 */
public class LyraRecipeProvider extends RecipeProvider {

    public static final List<Consumer<RecipeOutput>> RecipeGenerate = new ArrayList<>();

    protected LyraRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        RecipeGenerate.removeIf(generate -> {
            generate.accept(this.output);
            return true;
        });
    }

    public static class Runner extends RecipeProvider.Runner {

        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new LyraRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Lyra Recipes";
        }
    }
}
