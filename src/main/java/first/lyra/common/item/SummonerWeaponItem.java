package first.lyra.common.item;

import first.lyra.api.LyraHelper;
import first.lyra.common.attachment.AttachmentEntityData;
import first.lyra.common.dataComponent.MinionWeapon;
import first.lyra.common.entity.AttachmentEntityType;
import first.lyra.common.entity.PathNode;
import first.lyra.common.sound.Playable;
import first.lyra.register.LyraDataComponentRegister;
import first.lyra.common.minion.Minion;
import first.lyra.register.LyraAttributeRegister;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 仆从武器接口，定义可召唤仆从的武器物品行为。
 */
public class SummonerWeaponItem<T extends Minion> extends Item {

    private final Supplier<AttachmentEntityType<T>> typeSupplier;
    private final TriConsumer<@NotNull SummonerWeaponItem<T>, @NotNull Player, @NotNull ItemStack> summonConsumer;
    private final TriConsumer<@NotNull SummonerWeaponItem<T>, @NotNull Player, @NotNull ItemStack> removeConsumer;

    public SummonerWeaponItem(Item.Properties properties, Supplier<AttachmentEntityType<T>> typeSupplier, @Nullable TriConsumer<@NotNull SummonerWeaponItem<T>, @NotNull Player, @NotNull ItemStack> summonAction, TriConsumer<@NotNull SummonerWeaponItem<T>, @NotNull Player, @NotNull ItemStack> removeConsumer) {
        super(properties);
        this.typeSupplier = typeSupplier;
        this.summonConsumer = summonAction != null ? summonAction : (weapon, player, itemStack) -> {
            T minion = weapon.createMinion(player, itemStack);
            LyraHelper lyraHelper = LyraHelper.get(player);
            AttachmentEntityData.Type slotType = weapon.getSlotType(itemStack);
            if (lyraHelper.canSummon(slotType, minion.getSlotCost())) {
                AABB box = player.getBoundingBox();
                Vec3 pos = box.getCenter();
                minion.init(new PathNode(pos.offsetRandom(player.getRandom(), 2), 0, 0, 0));
                lyraHelper.add(slotType, minion);
            }
        };
        this.removeConsumer = removeConsumer != null ? removeConsumer : (weapon, player, itemStack) -> {
            AttachmentEntityData attachmentEntityData = LyraHelper.get(player).getEntityData();
            attachmentEntityData.remove(weapon.getSlotType(itemStack), weapon.getEntityType());
        };
    }

    @Override
    public @NonNull InteractionResult use(@NonNull Level level, @NonNull Player player, @NonNull InteractionHand hand) {
        ItemCooldowns cooldowns = player.getCooldowns();
        ItemStack itemStack = player.getItemInHand(hand);
        if (!cooldowns.isOnCooldown(itemStack) && !level.isClientSide()) {
            cooldowns.addCooldown(itemStack, 4);
            player.swing(hand, true);
            if (!player.isShiftKeyDown()) {
                summon(player, itemStack);
            } else {
                remove(player, itemStack);
            }
            Playable.play(getSoundEvent(itemStack), level, player.position(), player.getSoundSource());
        }
        return super.use(level, player, hand);
    }

    @NotNull
    public AttachmentEntityType<T> getEntityType() {
        return typeSupplier.get();
    }

    @NotNull
    public AttachmentEntityData.Type getSlotType(ItemStack itemStack) {
        return itemStack.getOrDefault(LyraDataComponentRegister.MINION_WEAPON, MinionWeapon.Empty).type();
    }

    public float getSummonDamage(@Nullable Player player, @NotNull ItemStack itemStack) {
        float damage = itemStack.getOrDefault(LyraDataComponentRegister.MINION_WEAPON, MinionWeapon.Empty).damage();
        if (player != null) {
            AttributeInstance attribute = player.getAttribute(LyraAttributeRegister.SummonDamage);
            if (attribute != null) {
                damage = (float) (damage * attribute.getValue());
            }
        }
        return damage;
    }

    public float getSummonKnockback(@Nullable Player player, @NotNull ItemStack itemStack) {
        float knockback = itemStack.getOrDefault(LyraDataComponentRegister.MINION_WEAPON, MinionWeapon.Empty).knockback();
        if (player != null) {
            AttributeInstance attribute = player.getAttribute(LyraAttributeRegister.SummonKnockback);
            if (attribute != null) {
                knockback = (float) (knockback * attribute.getValue());
            }
        }
        return knockback;
    }

    public float getSummonArmorPierce(@Nullable Player player, @NotNull ItemStack itemStack) {
        float armorPierce = itemStack.getOrDefault(LyraDataComponentRegister.MINION_WEAPON, MinionWeapon.Empty).armorPierce();
        if (player != null) {
            AttributeInstance attribute = player.getAttribute(LyraAttributeRegister.SummonArmorPierce);
            if (attribute != null) {
                armorPierce = (float) (armorPierce + attribute.getValue());
            }
        }
        return armorPierce;
    }
    @Nullable
    public SoundEvent getSoundEvent(ItemStack itemStack) {
        Holder<SoundEvent> event = itemStack.getOrDefault(LyraDataComponentRegister.MINION_WEAPON, MinionWeapon.Empty).soundEvent();
        if (event != null) {
            return event.value();
        }
        return null;
    }

    @NotNull
    public T createMinion(@NotNull Player player, @NotNull ItemStack itemStack) {
        AttachmentEntityType<T> type = getEntityType();
        T minion = type.factory().get();
        minion.setOwner(player);
        minion.setDamage(getSummonDamage(player, itemStack));
        minion.setKnockback(getSummonKnockback(player, itemStack));
        minion.setArmorPierce(getSummonArmorPierce(player, itemStack));
        return minion;
    }

    /**
     * 处理仆从召唤逻辑。
     */
    public void summon(@NotNull Player player, @NotNull ItemStack itemStack) {
        summonConsumer.accept(this, player, itemStack);
    }

    /**
     * 移除玩家拥有的此类型仆从。
     */
    public void remove(@NotNull Player player, @NotNull ItemStack itemStack) {
        removeConsumer.accept(this, player, itemStack);
    }

    @NotNull
    public List<Component> getTooltips(ItemStack itemStack, Player player) {
        List<Component> toolTips = new ArrayList<>();
        AttachmentEntityType<?> type = getEntityType();
        Identifier location = type.identifier();
        float damage = getSummonDamage(player, itemStack);
        if (damage > 0) {
            toolTips.add(Component.literal(String.format("%.1f ", damage)).withStyle(ChatFormatting.BLUE).append(Component.translatable("item.lyra.tooltip.damage").withStyle(ChatFormatting.GRAY)));
        }
        float knockback = getSummonKnockback(player, itemStack);
        if (knockback > 0) {
            toolTips.add(Component.literal(String.format("%.1f ", knockback)).withStyle(ChatFormatting.BLUE).append(Component.translatable("item.lyra.tooltip.knockback").withStyle(ChatFormatting.GRAY)));
        }
        float armor_pierce = getSummonArmorPierce(player, itemStack);
        if (armor_pierce > 0) {
            toolTips.add(Component.literal(String.format("%.1f ", armor_pierce)).withStyle(ChatFormatting.BLUE).append(Component.translatable("item.lyra.tooltip.armor_pierce").withStyle(ChatFormatting.GRAY)));
        }
        toolTips.add(Component.translatable("item.lyra.tooltip.summon", type.getDisplayName()).withStyle(ChatFormatting.GRAY));
        LyraHelper lyraHelper = LyraHelper.get(player);
        AttachmentEntityData.Type slotType = getSlotType(itemStack);
        switch (slotType) {
            case Minion ->
                    toolTips.add(Component.translatable("item.lyra.tooltip.minion_slots", Component.literal(String.valueOf(lyraHelper.getUsedSlots(AttachmentEntityData.Type.Minion))).withStyle(ChatFormatting.BLUE), Component.literal(String.valueOf(lyraHelper.getMaxCount(AttachmentEntityData.Type.Minion))).withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GRAY));
            case Sentry ->
                    toolTips.add(Component.translatable("item.lyra.tooltip.sentry_slots", Component.literal(String.valueOf(lyraHelper.getUsedSlots(AttachmentEntityData.Type.Sentry))).withStyle(ChatFormatting.BLUE), Component.literal(String.valueOf(lyraHelper.getMaxCount(AttachmentEntityData.Type.Sentry))).withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GRAY));
            default -> {
            }
        }
        toolTips.add(Component.translatable("item.lyra.tooltip.remove_all").withStyle(ChatFormatting.GRAY));
        return toolTips;
    }
}
