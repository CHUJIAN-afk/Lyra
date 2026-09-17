package first.lyra.utils;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class AttributeUtils {

    public static void condition(LivingEntity livingEntity, Holder<Attribute> attribute, UUID uuid, double amount, AttributeModifier.Operation operation, boolean condition) {
        AttributeInstance attributeInstance = livingEntity.getAttribute(attribute);
        if (attributeInstance == null) {
            return;
        }
        if (condition) {
            AttributeModifier modifier = attributeInstance.getModifier(uuid);
            if (modifier == null || modifier.getAmount() != amount || modifier.getOperation() != operation) {
                if (modifier != null) {
                    attributeInstance.removeModifier(uuid);
                }
                attributeInstance.addPermanentModifier(new AttributeModifier(uuid, uuid.toString(), amount, operation));
            }
        } else if (attributeInstance.getModifier(uuid) != null) {
            attributeInstance.removeModifier(uuid);
        }
    }

    public static UUID modifierId(ResourceLocation resourceLocation) {
        return UUID.nameUUIDFromBytes(resourceLocation.toString().getBytes(StandardCharsets.UTF_8));
    }
}
