package first.lyra.common.builder;

import com.google.common.collect.ImmutableMultimap;
import first.lyra.common.armorSet.ArmorSet;
import first.lyra.register.LyraItemRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ArmorSetBuilder {

    private final ResourceLocation id;
    private final List<ItemLike> items = new ArrayList<>();
    private final ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> modifiers = ImmutableMultimap.builder();
    private Consumer<Player> onStart = player -> {
    };
    private Consumer<Player> onRemove = player -> {
    };

    public ArmorSetBuilder(ResourceLocation id) {
        this.id = id;
    }

    public ArmorSetBuilder piece(ItemLike item) {
        this.items.add(item);
        return this;
    }

    public ArmorSetBuilder modifier(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
        this.modifiers.put(attribute, new AttributeModifier(id, amount, operation));
        return this;
    }

    public ArmorSetBuilder onStart(Consumer<Player> onStart) {
        this.onStart = onStart;
        return this;
    }

    public ArmorSetBuilder onRemove(Consumer<Player> oRemove) {
        this.onRemove = oRemove;
        return this;
    }

    public ArmorSetBuilder tooltip(int index, String enDesc, String zhDesc) {
        LyraItemRegistries registries = LyraItemRegistries.REGISTRIES.get(id.getNamespace());
        if (registries != null && registries.isDevelopment()) {
            registries.languageGenerate.put(id.toLanguageKey() + ".set." + index, new String[]{enDesc, zhDesc});
        }
        return this;
    }

    public ArmorSet build() {
        return new ArmorSet(id, List.copyOf(items), modifiers.build(), onStart, onRemove);
    }
}
