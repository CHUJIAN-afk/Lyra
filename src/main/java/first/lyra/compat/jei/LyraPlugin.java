package first.lyra.compat.jei;

import first.lyra.Lyra;
import first.lyra.dataGenerator.provider.LyraLanguageProvider;
import first.lyra.register.LyraRegistries;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@JeiPlugin
public class LyraPlugin implements IModPlugin {

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return Lyra.rl("jei_plugin");
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        LyraRegistries.ARMOR_SETS.forEach(armorSet -> registration.addItemStackInfo(armorSet.items().stream().map(ItemLike::asItem).map(Item::getDefaultInstance).toList(), Component.empty()));
        LyraLanguageProvider.InfoMap.forEach((itemLike, components) -> registration.addIngredientInfo(itemLike, components.toArray(new Component[0])));
    }
}
