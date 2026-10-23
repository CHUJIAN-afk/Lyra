package first.lyra.client.render;

import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class CameraShake {

    private static float strength;
    private static float previousStrength;

    private CameraShake() {
    }

    public static void add(float value) {
        strength = Math.clamp(strength + value, 0f, 1f);
    }

    public static void tick() {
        previousStrength = strength;
        strength *= 0.85f;
        if (strength < 0.001f) {
            strength = 0f;
        }
    }

    public static void apply(ViewportEvent.ComputeCameraAngles event) {
        if (!event.getCamera().isDetached() && (strength > 0f || previousStrength > 0f)) {
            float value = Mth.lerp((float) event.getPartialTick(), previousStrength, strength);
            double time = event.getCamera().getEntity().tickCount + event.getPartialTick();
            event.setPitch(event.getPitch() + (float) Math.sin(time * 2.1) * value * 3f);
            event.setRoll(event.getRoll() + (float) Math.sin(time * 1.7 + 1.2) * value * 5f);
        }
    }

    public static void clear() {
        strength = 0f;
        previousStrength = 0f;
    }
}
