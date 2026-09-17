package first.lyra.common.dataComponent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import first.lyra.common.minion.MinionSlotType;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mesdag.portlib.network.PortRegistryFriendlyByteBuf;
import org.mesdag.portlib.network.codec.PortByteBufCodecs;
import org.mesdag.portlib.network.codec.PortStreamCodec;

import java.util.Optional;

public record MinionWeapon(float damage, float knockback, float armorPierce, @NotNull MinionSlotType type, @Nullable Holder<SoundEvent> soundEvent) {

    public static final MinionWeapon Empty = new MinionWeapon(0, 0, 0, MinionSlotType.Minion, null);

    public static final Codec<MinionWeapon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("damage").forGetter(MinionWeapon::damage),
            Codec.FLOAT.fieldOf("knockback").forGetter(MinionWeapon::knockback),
            Codec.FLOAT.fieldOf("armorPierce").forGetter(MinionWeapon::armorPierce),
            Codec.STRING.xmap(MinionSlotType::valueOf, MinionSlotType::name).fieldOf("type").forGetter(MinionWeapon::type),
            SoundEvent.CODEC.optionalFieldOf("soundEvent").forGetter(mw -> Optional.ofNullable(mw.soundEvent()))
    ).apply(instance, (damage, knockback, armorPierce, type, soundEvent) -> new MinionWeapon(damage, knockback, armorPierce, type, soundEvent.orElse(null))));

    public static final PortStreamCodec<PortRegistryFriendlyByteBuf, MinionWeapon> STREAM_CODEC = PortByteBufCodecs.fromCodecWithRegistries(CODEC);

    public boolean isEmpty() {
        return this == Empty;
    }
}
