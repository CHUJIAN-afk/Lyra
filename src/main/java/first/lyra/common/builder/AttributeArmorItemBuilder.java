package first.lyra.common.builder;

import first.lyra.Lyra;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.function.Consumer;

public class AttributeArmorItemBuilder {

    private final Holder<ArmorMaterial> material;
    private final ArmorItem.Type type;
    private final Item.Properties properties;
    private final ItemAttributeModifiers.Builder modifiers = ItemAttributeModifiers.builder();

    public AttributeArmorItemBuilder(Holder<ArmorMaterial> material, ArmorItem.Type type) {
        this.material = material;
        this.type = type;
        this.properties = new Item.Properties().durability(-1)
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    public static AttributeArmorItemBuilder builder(Holder<ArmorMaterial> material, ArmorItem.Type type) {
        return new AttributeArmorItemBuilder(material, type);
    }

    public AttributeArmorItemBuilder modifier(Holder<Attribute> attribute, AttributeModifier modifier) {
        modifiers.add(attribute, modifier, EquipmentSlotGroup.bySlot(type.getSlot()));
        return this;
    }

    public AttributeArmorItemBuilder modifier(Holder<Attribute> attribute, double value, AttributeModifier.Operation operation) {
        return modifier(attribute, new AttributeModifier(Lyra.id("armor_" + type.getName()), value, operation));
    }

    public AttributeArmorItemBuilder properties(Consumer<Item.Properties> customizer) {
        customizer.accept(properties);
        return this;
    }

    public ArmorItem build() {
        return new ArmorItem(material, type, properties.attributes(modifiers.build()));
    }
}
