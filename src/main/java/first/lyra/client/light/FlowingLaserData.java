package first.lyra.client.light;

import com.lowdragmc.photon.client.light.DynamicLight;
import com.lowdragmc.photon.client.light.DynamicLightRenderer;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public final class FlowingLaserData {

    private static FloatBuffer data;
    private static int texture = -1;
    private static boolean dirty;

    private FlowingLaserData() {
    }

    public interface Access {
        FlowingLaserProfile lyra$getFlowingLaser();

        void lyra$setFlowingLaser(FlowingLaserProfile profile);
    }

    public static void begin() {
        dirty = false;
    }

    public static void pack(DynamicLight light, int index, FloatBuffer nativeData) {
        FlowingLaserProfile profile = ((Access) light).lyra$getFlowingLaser();
        if (profile != null && light.type == DynamicLight.Type.SPOT) {
            if (data == null) {
                data = MemoryUtil.memCallocFloat(DynamicLightRenderer.MAX_LIGHTS * 16);
            }
            profile.write(data, index * 16);
            // Photon 的 0/1 仍为点光/聚光灯；2 起是流动激光在本帧数据中的索引。
            nativeData.put(index * 16 + 7, index + 2);
            dirty = true;
        }
    }

    public static void upload(int count) {
        if (dirty) {
            if (texture == -1) {
                texture = TextureUtil.generateTextureId();
                GlStateManager._bindTexture(texture);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, 1024, 4, 0, GL11.GL_RGBA, GL11.GL_FLOAT, (FloatBuffer) null);
            }
            int rows = (count + 255) / 256;
            data.position(0).limit(rows * 4096);
            GlStateManager._bindTexture(texture);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 1024, rows, GL11.GL_RGBA, GL11.GL_FLOAT, data);
            GlStateManager._bindTexture(0);
            data.clear();
        }
    }

    public static FloatBuffer volumes(FloatBuffer volumes, FloatBuffer lights, int[] indices, int count) {
        if (dirty) {
            for (int i = 0; i < count; i++) {
                float type = lights.get(i * 16 + 7);
                if (type > 1.5f && indices[i] >= 0) {
                    volumes.put(indices[i] * 12 + 7, type);
                }
            }
        }
        return volumes;
    }

    public static int texture() {
        return Math.max(texture, 0);
    }

    public static void close() {
        if (texture != -1) {
            GlStateManager._deleteTexture(texture);
            texture = -1;
        }
        if (data != null) {
            MemoryUtil.memFree(data);
            data = null;
        }
        dirty = false;
    }
}
