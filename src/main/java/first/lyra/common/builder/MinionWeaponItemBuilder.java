package first.lyra.common.builder;

import first.lyra.common.attachmentEntity.AttachmentEntityType;
import first.lyra.common.dataComponent.MinionWeapon;
import first.lyra.common.item.SummonerWeaponItem;
import first.lyra.common.minion.Minion;
import first.lyra.common.minion.MinionSlotType;
import first.lyra.register.LyraDataComponentRegister;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * 仆从武器构建器，通过链式配置创建武器物品。
 */
public class MinionWeaponItemBuilder<T extends Minion> {

    private final Supplier<AttachmentEntityType<T>> typeSupplier;
    private float damage = 0;
    private float knockback = 0;
    private float armorPierce = 0;
    private MinionSlotType slotType = MinionSlotType.Minion;
    private Holder<SoundEvent> soundEventHolder = null;
    private TriConsumer<@NotNull SummonerWeaponItem<T>, @NotNull Player, @NotNull ItemStack> summonConsumer = null;
    private TriConsumer<@NotNull SummonerWeaponItem<T>, @NotNull Player, @NotNull ItemStack> removeConsumer = null;

    public MinionWeaponItemBuilder(ResourceLocation identifier, @NotNull Supplier<AttachmentEntityType<T>> typeSupplier) {
        this.typeSupplier = typeSupplier;
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
     * 设置为哨兵。
     */
    public MinionWeaponItemBuilder<T> slotType(MinionSlotType slotType) {
        this.slotType = slotType;
        return this;
    }

    /**
     * 设置召唤时播放的音效。
     */
    public MinionWeaponItemBuilder<T> sound(Holder<SoundEvent> soundEventHolder) {
        this.soundEventHolder = soundEventHolder;
        return this;
    }

    /**
     * 完整重写召唤逻辑，weapon 可调用 createMinion 构建实例。
     */
    public MinionWeaponItemBuilder<T> summon(TriConsumer<SummonerWeaponItem<T>, Player, ItemStack> summonConsumer) {
        this.summonConsumer = summonConsumer;
        return this;
    }

    /**
     * 设置仆从移除回调。
     */
    public MinionWeaponItemBuilder<T> remove(TriConsumer<SummonerWeaponItem<T>, Player, ItemStack> removeConsumer) {
        this.removeConsumer = removeConsumer;
        return this;
    }

    /**
     * 构建武器物品，物品属性由 Registrate 的 ItemBuilder.properties 提供。
     */
    public SummonerWeaponItem<T> build(Item.Properties properties) {
        properties.stacksTo(1);
        properties.component(LyraDataComponentRegister.MINION_WEAPON, new MinionWeapon(damage, knockback, armorPierce, slotType, soundEventHolder));
        return new SummonerWeaponItem<>(properties, typeSupplier, summonConsumer, removeConsumer);
    }
}
