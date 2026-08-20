package first.lyra.compat.jei;

import first.lyra.Lyra;
import first.lyra.dataGenerator.provider.LyraLanguageProvider;
import first.lyra.register.LyraRegistries;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@JeiPlugin
public class LyraPlugin implements IModPlugin {

    @Override
    public @NotNull Identifier getPluginUid() {
        return Lyra.id("jei_plugin");
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        LyraRegistries.ARMOR_SETS.stream().forEach(armorSet -> registration.addItemStackInfo(armorSet.items().stream().map(ItemLike::asItem).map(Item::getDefaultInstance).toList(), Component.empty()));
        LyraLanguageProvider.InfoMap.forEach((itemLike, components) -> registration.addIngredientInfo(itemLike, components.toArray(new Component[0])));
    }
}
