package first.lyra.api;

import com.light.api.DynamicLightAPI;
import first.lyra.utils.ParticleHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface LyraAPI {

    static LyraHelper entity(Player player) {
        return LyraHelper.get(player);
    }

    static ParticleHelper particle(Level level) {
        return ParticleHelper.create(level);
    }

    static void light(Vec3 pos, int level) {
        DynamicLightAPI.light(pos, level);
    }

    static void light(Vec3 pos, float strength) {
        DynamicLightAPI.light(pos, strength);
    }
}
