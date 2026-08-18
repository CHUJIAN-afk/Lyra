package first.lyra.common.builder;

import first.lyra.api.LyraHelper;
import first.lyra.common.attachment.AttachmentEntityData;
import first.lyra.common.entity.AttachmentEntityType;
import first.lyra.common.entity.PathNode;
import first.lyra.common.item.IMinionWeaponItem;
import first.lyra.common.minion.Minion;
import first.lyra.common.sound.Playable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 仆从武器构建器，通过链式配置创建武器物品。
 */
public class MinionWeaponItemBuilder<T extends Minion> {

    public static void handler(PlayerInteractEvent.RightClickItem event) {
        ItemStack itemStack = event.getItemStack();
        Player player = event.getEntity();
        Level level = player.level();
        ItemCooldowns cooldowns = player.getCooldowns();
        if (event.getHand() == InteractionHand.MAIN_HAND && !cooldowns.isOnCooldown(itemStack.getItem()) && itemStack.getItem() instanceof IMinionWeaponItem<?> iMinionWeaponItem) {
            cooldowns.addCooldown(itemStack.getItem(), 4);
            player.swing(InteractionHand.MAIN_HAND, true);
            if (!level.isClientSide()) {
                if (!player.isShiftKeyDown()) {
                    iMinionWeaponItem.summon(player, itemStack);
                } else {
                    iMinionWeaponItem.remove(player);
                }
                Playable.play(iMinionWeaponItem.getSoundEvent(), level, player.position(), player.getSoundSource());
            }
        }
    }

    private final Supplier<AttachmentEntityType<T>> typeSupplier;
    private boolean sentry = false;
    private float damage = 0;
    private float knockback = 0;
    private float armorPierce = 0;
    private Supplier<SoundEvent> soundEventSupplier = () -> null;
    private SummonTooltip<T> summonTooltip = null;
    private TriConsumer<@NotNull IMinionWeaponItem<T>, @NotNull Player, @Nullable ItemStack> summonAction = (weapon, player, itemStack) -> {
        T minion = weapon.createMinion(player, itemStack);
        LyraHelper lyraHelper = LyraHelper.get(player);
        if (lyraHelper.canSummon(AttachmentEntityData.Type.Minion, 1)) {
            AABB box = player.getBoundingBox();
            Vec3 pos = box.getCenter();
            minion.init(new PathNode(pos.offsetRandom(player.getRandom(), 2), 0, 0, 0));
            lyraHelper.add(AttachmentEntityData.Type.Minion, minion);
        }
    };
    private Consumer<Player> onRemove = null;
    private Consumer<Item.Properties> properties = null;

    public MinionWeaponItemBuilder(@NotNull Supplier<AttachmentEntityType<T>> typeSupplier) {
        this.typeSupplier = typeSupplier;
    }

    /**
     * 设置为哨兵。
     */
    public MinionWeaponItemBuilder<T> sentry() {
        this.sentry = true;
        return this;
    }

    /**
     * 设置召唤伤害值。
     */
    public MinionWeaponItemBuilder<T> damage(float damage) {
        this.damage = damage;
        return this;
    }

    /**
     * 设置召唤击退力度。
     */
    public MinionWeaponItemBuilder<T> knockback(float knockback) {
        this.knockback = knockback;
        return this;
    }

    /**
     * 设置召唤护甲穿透。
     */
    public MinionWeaponItemBuilder<T> armorPierce(float armorPierce) {
        this.armorPierce = armorPierce;
        return this;
    }

    /**
     * 设置召唤时播放的音效。
     */
    public MinionWeaponItemBuilder<T> sound(Supplier<SoundEvent> soundEventSupplier) {
        this.soundEventSupplier = soundEventSupplier;
        return this;
    }

    /**
     * 完整重写召唤逻辑，weapon 可调用 createMinion 构建实例。
     */
    public MinionWeaponItemBuilder<T> summon(TriConsumer<IMinionWeaponItem<T>, Player, ItemStack> action) {
        this.summonAction = action;
        return this;
    }

    /**
     * 设置仆从移除回调。
     */
    public MinionWeaponItemBuilder<T> onRemove(Consumer<Player> action) {
        this.onRemove = action;
        return this;
    }

    public MinionWeaponItemBuilder<T> properties(Consumer<Item.Properties> properties) {
        this.properties = properties;
        return this;
    }

    /**
     * 自定义召唤 tooltip 中"召唤目标"文本(覆写 {@link IMinionWeaponItem#getSummonTooltip} 默认行为)。
     * <p>
     * 默认显示仆从类型名翻译;剑鞘类武器可借此显示存放的物品名。
     * </p>
     */
    public MinionWeaponItemBuilder<T> summonTooltip(SummonTooltip<T> summonTooltip) {
        this.summonTooltip = summonTooltip;
        return this;
    }

    /**
     * 构建武器物品。
     */
    public MinionWeaponItem build() {
        Item.Properties proper = new Item.Properties().stacksTo(1);
        if (properties != null) {
            properties.accept(proper);
        }
        return new MinionWeaponItem(proper);
    }

    public class MinionWeaponItem extends Item implements IMinionWeaponItem<T> {

        public MinionWeaponItem(Properties properties) {
            super(properties);
        }

        @Override
        public @NotNull AttachmentEntityType<T> getType() {
            return typeSupplier.get();
        }

        @Override
        public boolean isSentry() {
            return sentry;
        }

        @Override
        public SoundEvent getSoundEvent() {
            return soundEventSupplier.get();
        }

        @Override
        public void summon(@NotNull Player player, @Nullable ItemStack itemStack) {
            summonAction.accept(this, player, itemStack);
        }

        @Override
        public float getSummonDamage(@Nullable Player player,@Nullable ItemStack itemStack) {
            return damage;
        }

        @Override
        public float getSummonKnockback(@Nullable Player player,@Nullable ItemStack itemStack) {
            return knockback;
        }

        @Override
        public float getSummonArmorPierce(@Nullable Player player, @Nullable ItemStack itemStack) {
            return armorPierce;
        }

        @Override
        public Component getSummonTooltip(ItemStack itemStack, AttachmentEntityType<?> type, ResourceLocation location, Player player) {
            if (summonTooltip != null) {
                return summonTooltip.apply(this, itemStack, type, location, player);
            }
            return IMinionWeaponItem.super.getSummonTooltip(itemStack, type, location, player);
        }

        @Override
        public void remove(@NotNull Player player) {
            if (onRemove != null) {
                onRemove.accept(player);
            } else {
                IMinionWeaponItem.super.remove(player);
            }
        }
    }

    /** 召唤 tooltip 自定义器:返回"召唤目标"文本。 */
    @FunctionalInterface
    public interface SummonTooltip<T extends Minion> {
        Component apply(IMinionWeaponItem<T> weapon, ItemStack itemStack, AttachmentEntityType<?> type, ResourceLocation location, Player player);
    }
}
