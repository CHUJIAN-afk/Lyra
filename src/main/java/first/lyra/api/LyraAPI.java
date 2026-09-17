package first.lyra.api;

import first.lyra.client.dynamicLight.DynamicLightDispatcher;
import first.lyra.common.attachment.DamageInfoData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class LyraAPI {

    private LyraAPI() {
    }

    public static DamageInfoData.DamageInfoBuilder damageInfo(Level level) {
        return DamageInfoData.build(level);
    }

    public static void dynamicLight(Vec3 pos, int light) {
        DynamicLightDispatcher.addLightSources(pos, light);
    }
}
