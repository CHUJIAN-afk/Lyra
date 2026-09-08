package first.lyra.common.dataComponent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import first.lyra.common.attachment.AttachmentEntityData;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record MinionWeapon(float damage, float knockback, float armorPierce, @NotNull AttachmentEntityData.Type type, @Nullable Holder<SoundEvent> soundEvent) {

    public static final MinionWeapon Empty = new MinionWeapon(0, 0, 0, AttachmentEntityData.Type.Minion, null);

    public static final Codec<MinionWeapon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("damage").forGetter(MinionWeapon::damage),
            Codec.FLOAT.fieldOf("knockback").forGetter(MinionWeapon::knockback),
            Codec.FLOAT.fieldOf("armorPierce").forGetter(MinionWeapon::armorPierce),
            Codec.STRING.xmap(AttachmentEntityData.Type::valueOf, AttachmentEntityData.Type::name).fieldOf("type").forGetter(MinionWeapon::type),
            SoundEvent.CODEC.optionalFieldOf("soundEvent").forGetter(mw -> Optional.ofNullable(mw.soundEvent()))
    ).apply(instance, (damage, knockback, armorPierce, type, soundEvent) -> new MinionWeapon(damage, knockback, armorPierce, type, soundEvent.orElse(null))));

    public static final StreamCodec<RegistryFriendlyByteBuf, MinionWeapon> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public boolean isEmpty() {
        return this == Empty;
    }
}
