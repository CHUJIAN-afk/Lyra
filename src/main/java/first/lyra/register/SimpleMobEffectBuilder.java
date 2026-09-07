package first.lyra.register;

import it.unimi.dsi.fastutil.ints.Int2DoubleFunction;
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
    // 属性修改器
    private final List<AttributeModifierEntry> attributeModifiers = new ArrayList<>();
    private final List<AttributeCurveEntry> attributeCurveModifiers = new ArrayList<>();
    // 效果回调
    private BiConsumer<LivingEntity, Integer> applyEffectTick = null;
    private BiFunction<Integer, Integer, Boolean> shouldApplyEffectTickThisTick = null;

    public SimpleMobEffectBuilder(MobEffectCategory category, int color) {
        this.category = category;
        this.color = color;
    }

    /**
     * 设置每tick效果回调
     */
    public SimpleMobEffectBuilder applyEffectTick(BiConsumer<LivingEntity, Integer> callback) {
        this.applyEffectTick = callback;
        return this;
    }

    /**
     * 设置是否在当前tick触发效果
     */
    public SimpleMobEffectBuilder shouldApplyEffectTickThisTick(BiFunction<Integer, Integer, Boolean> callback) {
        this.shouldApplyEffectTickThisTick = callback;
        return this;
    }

    /**
     * 添加属性修改器
     */
    public SimpleMobEffectBuilder addAttributeModifier(Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        this.attributeModifiers.add(new AttributeModifierEntry(attribute, id, amount, operation));
        return this;
    }

    /**
     * 添加曲线属性修改器
     */
    public SimpleMobEffectBuilder addAttributeModifier(Holder<Attribute> attribute, ResourceLocation id, Int2DoubleFunction curve, AttributeModifier.Operation operation) {
        this.attributeCurveModifiers.add(new AttributeCurveEntry(attribute, id, operation, curve));
        return this;
    }

    /**
     * 构建MobEffect实例
     */
    public MobEffect build() {
        MobEffect effect = new MobEffect(category, color) {
            @Override
            public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
                if (applyEffectTick != null) {
                    applyEffectTick.accept(entity, amplifier);
                    return true;
                }
                return super.applyEffectTick(entity, amplifier);
            }

            @Override
            public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
                if (shouldApplyEffectTickThisTick != null) {
                    return shouldApplyEffectTickThisTick.apply(duration, amplifier);
                }
                return super.shouldApplyEffectTickThisTick(duration, amplifier);
            }
        };
        for (AttributeModifierEntry entry : attributeModifiers) {
            effect.addAttributeModifier(entry.attribute, entry.id, entry.amount, entry.operation);
        }
        for (AttributeCurveEntry entry : attributeCurveModifiers) {
            effect.addAttributeModifier(entry.attribute, entry.id, entry.operation, entry.curve);
        }
        return effect;
    }

    private record AttributeModifierEntry(Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
    }

    private record AttributeCurveEntry(Holder<Attribute> attribute, ResourceLocation id, AttributeModifier.Operation operation, Int2DoubleFunction curve) {
    }
}