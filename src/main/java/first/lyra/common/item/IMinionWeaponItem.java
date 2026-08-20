package first.lyra.common.item;

import first.lyra.api.LyraHelper;
import first.lyra.common.attachment.AttachmentEntityData;
import first.lyra.common.entity.AttachmentEntityType;
import first.lyra.register.LyraRegistries;
import first.lyra.common.minion.Minion;
import first.lyra.register.LyraAttributeRegister;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 仆从武器接口，定义可召唤仆从的武器物品行为。
 */
public interface IMinionWeaponItem<T extends Minion> {

    /**
     * 获取此武器对应的仆从类型。
     */
    @Nullable AttachmentEntityType<T> getType();

    /**
     * 是否是哨兵。
     */
    boolean isSentry();

    /**
     * 处理仆从召唤逻辑。
     */
    void summon(@NotNull Player player, @Nullable ItemStack itemStack);

    /**
     * 获取召唤伤害值。
     */
    float getSummonDamage(@Nullable Player player, @Nullable ItemStack itemStack);

    /**
     * 获取召唤击退力度。
     */
    float getSummonKnockback(@Nullable Player player, @Nullable ItemStack itemStack);

    /**
     * 获取召唤护甲穿透。
     */
    float getSummonArmorPierce(@Nullable Player player, @Nullable ItemStack itemStack);

    /**
     * 获取召唤 tooltip 中的召唤目标文本。
     * <p>
     * 默认显示仆从类型名翻译；武器可覆写此方法以自定义目标（如剑鞘类武器显示存放的物品名）。
     * </p>
     */
    default Component getSummonTooltip(ItemStack itemStack, AttachmentEntityType<?> type, Identifier location, Player player) {
        String key = "summon." + location.getNamespace() + "." + location.getPath();
        return Component.translatable(key).withStyle(ChatFormatting.BLUE);
    }

    default List<Component> getTooltips(ItemStack itemStack, Player player) {
        List<Component> toolTips = new ArrayList<>();
        AttachmentEntityType<?> type = getType();
        Identifier location = type != null ? LyraRegistries.ATTACHMENT_ENTITY_TYPES.getKey(type) : null;
        if (location != null) {
            float damage = getSummonDamage(player, itemStack);
            if (damage > 0) {
                AttributeInstance attribute = player.getAttribute(LyraAttributeRegister.SummonDamage);
                damage = attribute != null ? (float) (damage * attribute.getValue()) : damage;
                toolTips.add(Component.literal(String.format("%.1f ", damage)).withStyle(ChatFormatting.BLUE).append(Component.translatable("item.lyra.tooltip.damage").withStyle(ChatFormatting.GRAY)));
            }
            float knockback = getSummonKnockback(player, itemStack);
            if (knockback > 0) {
                AttributeInstance attribute = player.getAttribute(LyraAttributeRegister.SummonKnockback);
                knockback = attribute != null ? (float) (knockback * attribute.getValue()) : knockback;
                toolTips.add(Component.literal(String.format("%.1f ", knockback)).withStyle(ChatFormatting.BLUE).append(Component.translatable("item.lyra.tooltip.knockback").withStyle(ChatFormatting.GRAY)));
            }
            float armor_pierce = getSummonArmorPierce(player, itemStack);
            if (armor_pierce > 0) {
                toolTips.add(Component.literal(String.format("%.1f ", armor_pierce)).withStyle(ChatFormatting.BLUE).append(Component.translatable("item.lyra.tooltip.armor_pierce").withStyle(ChatFormatting.GRAY)));
            }
            toolTips.add(Component.translatable("item.lyra.tooltip.summon", getSummonTooltip(itemStack, type, location, player)).withStyle(ChatFormatting.GRAY));
            LyraHelper lyraHelper = LyraHelper.get(player);
            if (!isSentry()){
                toolTips.add(Component.translatable("item.lyra.tooltip.minion_slots", Component.literal(String.valueOf(lyraHelper.getUsedSlots(AttachmentEntityData.Type.Minion))).withStyle(ChatFormatting.BLUE), Component.literal(String.valueOf(lyraHelper.getMaxCount(AttachmentEntityData.Type.Minion))).withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GRAY));
            } else {
                toolTips.add(Component.translatable("item.lyra.tooltip.sentry_slots", Component.literal(String.valueOf(lyraHelper.getUsedSlots(AttachmentEntityData.Type.Sentry))).withStyle(ChatFormatting.BLUE), Component.literal(String.valueOf(lyraHelper.getMaxCount(AttachmentEntityData.Type.Sentry))).withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GRAY));
            }
            toolTips.add(Component.translatable("item.lyra.tooltip.remove_all").withStyle(ChatFormatting.GRAY));
        }
        return toolTips;
    }

    /** 获取召唤时播放的音效。 */
    default SoundEvent getSoundEvent() {
        return null;
    }

    /**
     * 构建一个已初始化属性的仆从实例。
     */
    default T createMinion(@NotNull Player player, @Nullable ItemStack itemStack) {
        AttachmentEntityType<T> type = getType();
        if (type != null) {
            T minion = type.factory().get();
            minion.setOwner(player);
            minion.setDamage(getSummonDamage(player, itemStack));
            minion.setKnockback(getSummonKnockback(player, itemStack));
            minion.setArmorPierce(getSummonArmorPierce(player, itemStack));
            return minion;
        }
        return null;
    }

    /**
     * 移除玩家拥有的此类型仆从。
     */
    default void remove(@NotNull Player player) {
        AttachmentEntityData attachmentEntityData = LyraHelper.get(player).getEntityData();
        attachmentEntityData.remove(isSentry() ? AttachmentEntityData.Type.Sentry : AttachmentEntityData.Type.Minion, getType());
    }
}
