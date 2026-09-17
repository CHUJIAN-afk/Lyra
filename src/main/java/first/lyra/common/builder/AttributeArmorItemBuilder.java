package first.lyra.common.builder;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import first.lyra.Lyra;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;

public class AttributeArmorItemBuilder {

    private final Holder<ArmorMaterial> material;
    private final ArmorItem.Type type;
    private final Item.Properties properties;
    private final ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> modifiers = ImmutableMultimap.builder();

    public AttributeArmorItemBuilder(Holder<ArmorMaterial> material, ArmorItem.Type type) {
        this.material = material;
        this.type = type;
        this.properties = new Item.Properties();
    }

    public static AttributeArmorItemBuilder builder(Holder<ArmorMaterial> material, ArmorItem.Type type) {
        return new AttributeArmorItemBuilder(material, type);
    }

    public AttributeArmorItemBuilder modifier(Holder<Attribute> attribute, AttributeModifier modifier) {
        modifiers.put(attribute, modifier);
        return this;
    }

    public AttributeArmorItemBuilder modifier(Holder<Attribute> attribute, double value, AttributeModifier.Operation operation) {
        String id = "armor_" + type.getName();
        UUID uuid = UUID.nameUUIDFromBytes((Lyra.MODID + ':' + id).getBytes(StandardCharsets.UTF_8));
        return modifier(attribute, new AttributeModifier(uuid, id, value, operation));
    }

    public AttributeArmorItemBuilder properties(Consumer<Item.Properties> customizer) {
        customizer.accept(properties);
        return this;
    }

    public ArmorItem build() {
        Multimap<Holder<Attribute>, AttributeModifier> built = modifiers.build();
        return new ArmorItem(material.value(), type, properties) {
            @Override
            public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
                Multimap<Attribute, AttributeModifier> original = super.getDefaultAttributeModifiers(slot);
                if (slot != type.getSlot()) {
                    return original;
                }
                ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
                builder.putAll(original);
                built.forEach((attribute, modifier) -> builder.put(attribute.value(), modifier));
                return builder.build();
            }

            @Override
            public boolean isDamageable(ItemStack stack) {
                return false;
            }
        };
    }
}
