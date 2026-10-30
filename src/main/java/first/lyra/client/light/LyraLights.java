package first.lyra.client.light;

import com.lowdragmc.photon.client.light.DynamicLight;
import com.lowdragmc.photon.client.light.LightProvider;
import com.lowdragmc.photon.client.light.LightSink;
import com.lowdragmc.photon.client.light.PhotonLights;
import com.lowdragmc.photon.client.compat.iris.IrisCompat;
import first.lyra.common.attachmentEntity.AttachmentEntity;
import first.lyra.common.attachmentEntity.AttachmentEntityType;
import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

public final class LyraLights {

    private static final ArrayList<DynamicLight> LIGHTS = new ArrayList<>();
    private static final DynamicLight IGNORED_LIGHT = new DynamicLight();
    private static final LightProvider PROVIDER = LyraLights::submit;
    private static ClientLevel level;
    private static int count;

    private LyraLights() {
    }

    public static void initialize() {
        PhotonLights.addProvider(PROVIDER);
    }

    private static void submit(LightSink sink, float partialTick) {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != null) {
            if (current == level) {
                for (int i = 0; i < count; i++) {
                    sink.next().set(LIGHTS.get(i));
                }
            }
        }
        count = 0;
        level = current;
    }

    public static DynamicLight next() {
        if (IrisCompat.isShadowPass()) {
            return IGNORED_LIGHT.reset();
        }
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
