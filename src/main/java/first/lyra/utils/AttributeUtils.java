package first.lyra.utils;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.mesdag.portlib.wrapper.world.entity.ai.attributes.PortAttributeModifier;

public class AttributeUtils {

    public static void condition(LivingEntity livingEntity, Holder<Attribute> attribute, ResourceLocation resourceLocation, double amount, AttributeModifier.Operation operation, boolean condition) {
        if (condition) {
            AttributeInstance attributeInstance = livingEntity.getAttribute(attribute);
            if (attributeInstance != null) {
                AttributeModifier attributeModifier = attributeInstance.getModifier(PortAttributeModifier.rl2uuid(resourceLocation));
                if ((attributeModifier == null) || attributeModifier.getAmount() != amount || attributeModifier.getOperation() != operation) {
                    addAttributeModifier(livingEntity, attribute, resourceLocation, amount, operation);
                }
            }
        } else {
            removeAttributeModifier(livingEntity, attribute, resourceLocation);
        }
    }

    public static void addAttributeModifier(LivingEntity livingEntity, Holder<Attribute> attribute, ResourceLocation resourceLocation, double amount, AttributeModifier.Operation operation) {
        AttributeInstance attributeInstance = livingEntity.getAttribute(attribute);
        if (attributeInstance != null) {
            if (attributeInstance.getModifier(PortAttributeModifier.rl2uuid(resourceLocation)) != null) {
                attributeInstance.removeModifier(PortAttributeModifier.rl2uuid(resourceLocation));
            }
            attributeInstance.addPermanentModifier(new PortAttributeModifier(resourceLocation, amount, PortAttributeModifier.Operation.wrap(operation)).unwrap());
        }
    }

    public static void removeAttributeModifier(LivingEntity livingEntity, Holder<Attribute> attribute, ResourceLocation resourceLocation) {
        AttributeInstance attributeInstance = livingEntity.getAttribute(attribute);
        if (attributeInstance != null) {
            if (attributeInstance.getModifier(PortAttributeModifier.rl2uuid(resourceLocation)) != null) {
                attributeInstance.removeModifier(PortAttributeModifier.rl2uuid(resourceLocation));
            }
        }
    }
}
