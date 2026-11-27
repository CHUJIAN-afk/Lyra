package first.lyra.client.render;

import net.neoforged.fml.ModList;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.blending.DepthColorStorage;
import net.irisshaders.iris.shadows.ShadowRenderingState;

public final class IrisCompat {

    private static final boolean IrisLoaded = ModList.get().isLoaded("iris");

    public static boolean isShadowPass() {
        return IrisLoaded && ShadowRenderingState.areShadowsCurrentlyBeingRendered();
    }

    public static boolean isUsingShaderPack() {
        return IrisLoaded && Iris.isPackInUseQuick();
    }

    public static void unlockDepthColor() {
        if (IrisLoaded && DepthColorStorage.isDepthColorLocked()) {
            DepthColorStorage.unlockDepthColor();
        }
    }
}
