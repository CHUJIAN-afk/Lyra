package first.lyra.api;

import first.lyra.common.dataComponent.MinionWeapon;
import first.lyra.common.minion.MinionSlotType;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MinionWeaponModifyEvent extends Event {

    public final ItemLike item;//物品
    public float damage;//伤害
    public float knockback;//击退
    public float armorPierce;//护甲穿透
    public @NotNull MinionSlotType type;//占用的槽位类型枚举
    public @Nullable Holder<SoundEvent> soundEvent;//召唤时发出的声音

    public MinionWeaponModifyEvent(ItemLike item, MinionWeapon weapon) {
        this.item = item;
        this.damage = weapon.damage();
        this.knockback = weapon.knockback();
        this.armorPierce = weapon.armorPierce();
        this.type = weapon.type();
        this.soundEvent = weapon.soundEvent();
    }

    public MinionWeapon toMinionWeapon() {
        return new MinionWeapon(damage, knockback, armorPierce, type, soundEvent);
    }
}
