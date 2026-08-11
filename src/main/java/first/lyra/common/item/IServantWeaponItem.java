package first.lyra.common.item;

import first.lyra.api.LyraHelper;
import first.lyra.common.attachment.AttachmentEntityData;
import first.lyra.common.entity.AttachmentEntityType;
import first.lyra.register.LyraRegistries;
import first.lyra.common.servant.Servant;
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
public interface IServantWeaponItem<T extends Servant> {

    /**
     * 获取此武器对应的仆从类型。
     */
    @NotNull AttachmentEntityType<T> getType();

    /**
     * 是否是哨兵。
     */
    boolean isSentryServant();

    /**
     * 处理仆从召唤逻辑。
     */
    void summon(@NotNull Player player, @Nullable ItemStack itemStack);

    /**
     * 获取仆从伤害值。
     */
    float getServantDamage(@Nullable Player player, @Nullable ItemStack itemStack);

    /**
     * 获取仆从击退力度。
     */
    float getServantKnockback(@Nullable Player player, @Nullable ItemStack itemStack);

    /**
     * 获取仆从护甲穿透。
     */
    float getServantArmorPierce(@Nullable Player player, @Nullable ItemStack itemStack);

    /**
     * 获取召唤 tooltip 中的召唤目标文本。
     * <p>
     * 默认显示仆从类型名翻译；武器可覆写此方法以自定义目标（如剑鞘类武器显示存放的物品名）。
     * </p>
     */
    default Component getSummonTooltip(ItemStack itemStack, AttachmentEntityType<?> type, Identifier location, Player player) {
        String key = "servant." + location.getNamespace() + "." + location.getPath();
        return Component.translatable(key).withStyle(ChatFormatting.BLUE);
    }

    default List<Component> getTooltips(ItemStack itemStack, Player player) {
        List<Component> toolTips = new ArrayList<>();
        AttachmentEntityType<?> type = getType();
        Identifier location = LyraRegistries.ATTACHMENT_ENTITY_TYPES.getKey(type);
        if (location != null) {
            float damage = getServantDamage(player, itemStack);
            if (damage > 0) {
                AttributeInstance attribute = player.getAttribute(LyraAttributeRegister.ServantDamage);
                damage = attribute != null ? (float) (damage * attribute.getValue()) : damage;
                toolTips.add(Component.literal(String.format("%.1f ", damage)).withStyle(ChatFormatting.BLUE).append(Component.translatable("item.lyra.tooltip.damage").withStyle(ChatFormatting.GRAY)));
            }
            float knockback = getServantKnockback(player, itemStack);
            if (knockback > 0) {
                AttributeInstance attribute = player.getAttribute(LyraAttributeRegister.ServantKnockback);
                knockback = attribute != null ? (float) (knockback * attribute.getValue()) : knockback;
                toolTips.add(Component.literal(String.format("%.1f ", knockback)).withStyle(ChatFormatting.BLUE).append(Component.translatable("item.lyra.tooltip.knockback").withStyle(ChatFormatting.GRAY)));
            }
            float armor_pierce = getServantArmorPierce(player, itemStack);
            if (armor_pierce > 0) {
                toolTips.add(Component.literal(String.format("%.1f ", armor_pierce)).withStyle(ChatFormatting.BLUE).append(Component.translatable("item.lyra.tooltip.armor_pierce").withStyle(ChatFormatting.GRAY)));
            }
            toolTips.add(Component.translatable("item.lyra.tooltip.summon", getSummonTooltip(itemStack, type, location, player)).withStyle(ChatFormatting.GRAY));
            LyraHelper lyraHelper = LyraHelper.get(player);
            if (!isSentryServant()){
                toolTips.add(Component.translatable("item.lyra.tooltip.servant_slots", Component.literal(String.valueOf(lyraHelper.getUsedSlots(AttachmentEntityData.Type.Servant))).withStyle(ChatFormatting.BLUE), Component.literal(String.valueOf(lyraHelper.getMaxCount(AttachmentEntityData.Type.Servant))).withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GRAY));
            } else {
                toolTips.add(Component.translatable("item.lyra.tooltip.sentry_servant_slots", Component.literal(String.valueOf(lyraHelper.getUsedSlots(AttachmentEntityData.Type.SentryServant))).withStyle(ChatFormatting.BLUE), Component.literal(String.valueOf(lyraHelper.getMaxCount(AttachmentEntityData.Type.SentryServant))).withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GRAY));
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
    default T createServant(@NotNull Player player, @Nullable ItemStack itemStack) {
        T servant = getType().factory().get();
        servant.setOwner(player);
        servant.setDamage(getServantDamage(player, itemStack));
        servant.setKnockback(getServantKnockback(player, itemStack));
        servant.setArmorPierce(getServantArmorPierce(player, itemStack));
        return servant;
    }

    /**
     * 移除玩家拥有的此类型仆从。
     */
    default void remove(@NotNull Player player) {
        AttachmentEntityData attachmentEntityData = LyraHelper.get(player).getEntityData();
        attachmentEntityData.remove(isSentryServant() ? AttachmentEntityData.Type.SentryServant : AttachmentEntityData.Type.Servant, getType());
    }
}
