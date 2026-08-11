package first.lyra.common.builder;

import first.lyra.Lyra;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.function.Consumer;

/**
 * 护甲物品构建器。
 * <p>
 * 26.2: ArmorItem/ArmorItem.Type/ArmorMaterial 移除,护甲组件化 ——
 * 普通 {@link Item} + {@link ArmorMaterial#createAttributes(ArmorType)} 属性
 * + {@link Item.Properties#equippable} 组件承载槽位。
 * </p>
 */
public class AttributeArmorItemBuilder {

    private final Holder<ArmorMaterial> material;
    private final ArmorType type;
    private final Item.Properties properties;
    private final ItemAttributeModifiers.Builder modifiers = ItemAttributeModifiers.builder();

    public AttributeArmorItemBuilder(Holder<ArmorMaterial> material, ArmorType type) {
        this.material = material;
        this.type = type;
        this.properties = new Item.Properties().durability(-1)
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE) // 26.2: Unbreakable 组件类型变为 Unit
                .component(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    public static AttributeArmorItemBuilder builder(Holder<ArmorMaterial> material, ArmorType type) {
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

    public Item build() {
        // 基础护甲属性(ArmorMaterial.createAttributes) + 构建器附加修饰
        ItemAttributeModifiers merged = material.value().createAttributes(type);
        for (ItemAttributeModifiers.Entry entry : modifiers.build().modifiers()) {
            merged = merged.withModifierAdded(entry.attribute(), entry.modifier(), entry.slot());
        }
        return new Item(properties
                .attributes(merged)
                .equippable(type.getSlot()));
    }
}
