package first.lyra.client.render;

import net.neoforged.fml.ModList;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.blending.DepthColorStorage;
import net.irisshaders.iris.shadows.ShadowRenderingState;

public final class IrisCompat {

    public static boolean isShadowPass() {
        return ModList.get().isLoaded("iris") && ShadowRenderingState.areShadowsCurrentlyBeingRendered();
    }

    public static boolean isUsingShaderPack() {
        return ModList.get().isLoaded("iris") && Iris.isPackInUseQuick();
    }

    public static void unlockDepthColor() {
        if (ModList.get().isLoaded("iris") && DepthColorStorage.isDepthColorLocked()) {
            DepthColorStorage.unlockDepthColor();
        }
    }
}
