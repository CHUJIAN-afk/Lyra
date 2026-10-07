package first.lyra.register;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class SimpleMobEffectBuilder {

    private final MobEffectCategory category;
    private final int color;
    private final List<AttributeModifierEntry> attributeModifiers = new ArrayList<>();
    private BiConsumer<LivingEntity, Integer> applyEffectTick = null;
    private BiFunction<Integer, Integer, Boolean> shouldApplyEffectTickThisTick = null;

    public SimpleMobEffectBuilder(MobEffectCategory category, int color) {
        this.category = category;
        this.color = color;
    }

    public SimpleMobEffectBuilder applyEffectTick(BiConsumer<LivingEntity, Integer> callback) {
        this.applyEffectTick = callback;
        return this;
    }

    public SimpleMobEffectBuilder shouldApplyEffectTickThisTick(BiFunction<Integer, Integer, Boolean> callback) {
        this.shouldApplyEffectTickThisTick = callback;
        return this;
    }

    public SimpleMobEffectBuilder addAttributeModifier(Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        this.attributeModifiers.add(new AttributeModifierEntry(attribute, id, amount, operation));
        return this;
    }

    public MobEffect build() {
        MobEffect effect = new MobEffect(category, color) {
            @Override
            public void applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
                if (applyEffectTick != null) {
                    applyEffectTick.accept(entity, amplifier);
                } else {
                    super.applyEffectTick(entity, amplifier);
                }
            }

            @Override
            public boolean isDurationEffectTick(int duration, int amplifier) {
                if (shouldApplyEffectTickThisTick != null) {
                    return shouldApplyEffectTickThisTick.apply(duration, amplifier);
                }
                return super.isDurationEffectTick(duration, amplifier);
            }
        };
        for (AttributeModifierEntry entry : attributeModifiers) {
            effect.addAttributeModifier(entry.attribute.value(), entry.id.toString(), entry.amount, entry.operation);
        }
        return effect;
    }

    private record AttributeModifierEntry(Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
    }
}
