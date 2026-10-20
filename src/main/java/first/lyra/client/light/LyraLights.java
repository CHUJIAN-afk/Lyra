package first.lyra.client.light;

import com.lowdragmc.photon.client.light.DynamicLight;
import com.lowdragmc.photon.client.light.LightProvider;
import com.lowdragmc.photon.client.light.LightSink;
import com.lowdragmc.photon.client.light.PhotonLights;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.AttachmentEntityType;
import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

public final class LyraLights {

    private static final Map<AttachmentEntityType<?>, Emitter<AttachmentEntity>> EMITTERS = new IdentityHashMap<>();
    private static final ArrayList<DynamicLight> LIGHTS = new ArrayList<>();
    private static final LightProvider PROVIDER = LyraLights::submit;
    private static ClientLevel level;
    private static int count;

    private LyraLights() {
    }

    @FunctionalInterface
    public interface Emitter<T extends AttachmentEntity> {
        void submit(T entity, LightSink sink, float partialTick);
    }

    public static void initialize() {
        PhotonLights.addProvider(PROVIDER);
    }

    @SuppressWarnings("unchecked")
    public static <T extends AttachmentEntity> void register(AttachmentEntityType<T> type, Emitter<T> emitter) {
        EMITTERS.put(type, (Emitter<AttachmentEntity>) emitter);
    }

    public static void unregister(AttachmentEntityType<?> type) {
        EMITTERS.remove(type);
    }

    private static void submit(LightSink sink, float partialTick) {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != null) {
            if (current == level) {
                for (int i = 0; i < count; i++) {
                    sink.next().set(LIGHTS.get(i));
                }
            }
            for (var player : current.players()) {
                for (AttachmentEntity entity : player.getData(LyraAttachmentRegister.EntityData).getRenderCache()) {
                    Emitter<AttachmentEntity> emitter = EMITTERS.get(entity.getType());
                    if (emitter != null && !entity.isRemove()) {
                        emitter.submit(entity, sink, partialTick);
                    }
                }
            }
        }
        count = 0;
        level = current;
    }

    public static void point(Vec3 position, int rgb, float intensity, float range) {
        if (intensity > 0 && range > 0 && PhotonLights.isEnabled()) {
            next().at(position.x, position.y, position.z)
                    .color((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f)
                    .intensity(intensity).range(range).shadows(false);
        }
    }

    /** intensity 为每格线长的强度，端点半权重，最多 256 个点。spacing 由调用方保证大于 0。 */
    public static void line(LightSink sink, Vec3 start, Vec3 end, DynamicLight template, double spacing) {
        if (template.intensity > 0 && template.range > 0) {
            double length = start.distanceTo(end);
            if (length == 0) {
                sink.next().set(template).point().at(start.x, start.y, start.z);
            } else {
                int segments = Math.clamp((int) Math.ceil(length / spacing), 1, 255);
                double step = length / segments;
                for (int i = 0; i <= segments; i++) {
                    double t = (double) i / segments;
                    float intensity = (float) (template.intensity * step * (i == 0 || i == segments ? 0.5 : 1));
                    sink.next().set(template).point().intensity(intensity).at(
                            start.x + (end.x - start.x) * t,
                            start.y + (end.y - start.y) * t,
                            start.z + (end.z - start.z) * t);
                }
            }
        }
    }

    public static void line(Vec3 start, Vec3 end, int rgb, float intensity, float range, double spacing) {
        if (PhotonLights.isEnabled()) {
            DynamicLight template = new DynamicLight()
                    .color((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f)
                    .intensity(intensity).range(range).shadows(false);
            line(LyraLights::next, start, end, template, spacing);
        }
    }

    private static DynamicLight next() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != level) {
            count = 0;
            level = current;
        }
        if (count == LIGHTS.size()) {
            LIGHTS.add(new DynamicLight());
        }
        return LIGHTS.get(count++).reset();
    }

    public static void clear() {
        count = 0;
        level = null;
    }
}
