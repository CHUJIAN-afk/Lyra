package first.lyra.api;

import first.lyra.client.dynamicLight.DynamicLightDispatcher;
import first.lyra.common.attachment.DamageInfoData;
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

    static DamageInfoData.DamageInfoBuilder damageInfo(Level level) {
        return DamageInfoData.build(level);
    }

    static void light(Vec3 pos, int light) {
        DynamicLightDispatcher.addLightSources(pos, light);
    }
}
