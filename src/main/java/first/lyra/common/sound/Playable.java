package first.lyra.common.sound;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class Playable {

    public static void play(Holder<SoundEvent> soundEvent, Level level, Vec3 pos, SoundSource source) {
        if (soundEvent != null) {
            play(soundEvent.value(), level, pos, source);
        }
    }

    public static void play(SoundEvent soundEvent, Level level, Vec3 pos, SoundSource source) {
        if (soundEvent != null) {
            RandomSource random = level.getRandom();
            level.playSound(null, pos.x(), pos.y(), pos.z(), soundEvent, source, 0.9f + random.nextFloat() * 0.2f, 0.9f + random.nextFloat() * 0.2f);
        }
    }
}
