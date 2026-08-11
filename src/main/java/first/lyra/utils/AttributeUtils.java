package first.lyra.utils;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class AttributeUtils {

    public static void condition(LivingEntity livingEntity, Holder<Attribute> attribute, Identifier Identifier, double amount, AttributeModifier.Operation operation, boolean condition) {
        if (condition) {
            if (livingEntity.getAttribute(attribute) instanceof AttributeInstance attributeInstance) {
                AttributeModifier attributeModifier = attributeInstance.getModifier(Identifier);
                if ((attributeModifier == null) || (attributeModifier instanceof AttributeModifier modifier && (modifier.amount() != amount || modifier.operation() != operation))) {
                    addAttributeModifier(livingEntity, attribute, Identifier, amount, operation);
                }
            }
        } else {
            removeAttributeModifier(livingEntity, attribute, Identifier);
        }
    }

    public static void addAttributeModifier(LivingEntity livingEntity, Holder<Attribute> attribute, Identifier Identifier, double amount, AttributeModifier.Operation operation) {
        AttributeModifier modifier = new AttributeModifier(Identifier, amount, operation);
        if (livingEntity.getAttribute(attribute) instanceof AttributeInstance attributeInstance) {
            if (attributeInstance.getModifier(Identifier) != null) {
                attributeInstance.removeModifier(Identifier);
            }
            attributeInstance.addPermanentModifier(modifier);
        }
    }

    public static void removeAttributeModifier(LivingEntity livingEntity, Holder<Attribute> attribute, Identifier Identifier){
        if (livingEntity.getAttribute(attribute) instanceof AttributeInstance attributeInstance) {
            if (attributeInstance.getModifier(Identifier) != null) {
                attributeInstance.removeModifier(Identifier);
            }
        }
    }
}
