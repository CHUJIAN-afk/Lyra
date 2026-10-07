package first.lyra.api;

import first.lyra.common.dataComponent.MinionWeapon;
import first.lyra.common.minion.MinionSlotType;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MinionWeaponModifyEvent extends Event {

    public final ItemLike item;
    public float damage;
    public float knockback;
    public float armorPierce;
    public @NotNull MinionSlotType type;
    public @Nullable Holder<SoundEvent> soundEvent;

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
