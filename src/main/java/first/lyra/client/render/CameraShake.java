package first.lyra.client.render;

import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.ArrayList;
import java.util.List;

/** 多个震动各自独立衰减，渲染时叠加合成镜头角度。 */
public final class CameraShake {

    private static final List<Shake> SHAKES = new ArrayList<>();

    private CameraShake() {
    }

    public static void add(float value) {
        if (value > 0f) {
            SHAKES.add(new Shake(Math.min(value, 1f), (float) (Math.random() * Math.PI * 2.0)));
        }
    }

    public static void tick() {
        for (Shake shake : SHAKES) {
            shake.previous = shake.strength;
            shake.strength *= 0.85f;
            if (shake.strength < 0.001f) {
                shake.strength = 0f;
            }
        }
        // 留一帧插值尾程再移除，避免角度跳变。
        SHAKES.removeIf(shake -> shake.previous <= 0f && shake.strength <= 0f);
    }

    public static void apply(ViewportEvent.ComputeCameraAngles event) {
        if (!event.getCamera().isDetached() && !SHAKES.isEmpty()) {
            float partialTick = (float) event.getPartialTick();
            double time = event.getCamera().getEntity().tickCount + event.getPartialTick();
            float pitch = 0f;
            float roll = 0f;
            for (Shake shake : SHAKES) {
                if (shake != null) {
                    float value = Mth.lerp(partialTick, shake.previous, shake.strength);
                    pitch += (float) Math.sin(time * 2.1 + shake.phase) * value * 3f;
                    roll += (float) Math.sin(time * 1.7 + 1.2 + shake.phase) * value * 5f;
                }
            }
            event.setPitch(event.getPitch() + pitch);
            event.setRoll(event.getRoll() + roll);
        }
    }

    public static void clear() {
        SHAKES.clear();
    }

    private static final class Shake {

        private final float phase;
        private float strength;
        private float previous;

        private Shake(float strength, float phase) {
            this.strength = strength;
            this.phase = phase;
        }
    }
}
