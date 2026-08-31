package first.lyra.common.damageInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import first.lyra.client.render.LyraRenderTypes;
import first.lyra.client.render.RenderUtil;
import first.lyra.common.attachment.DamageInfoData;
import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Map;

/**
 * 伤害数字渲染调度器。
 */
public class DamageInfoRenderDispatcher {

    /** 顶点组装缓冲（每顶点 5 float：x,y,z,u,v）与颜色缓冲，渲染线程复用。 */
    private static float[] xyzuvData = new float[1024 * 5];
    private static int[] colorData = new int[1024];

    public static void render(Level level, Vec3 camPos, SubmitNodeCollector collector, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        DamageInfoData data = level.getData(LyraAttachmentRegister.DamageInfoData);
        Map<Identifier, List<DamageInfo>> infos = data.getActiveInfos();
        Camera camera = minecraft.getEntityRenderDispatcher().camera;
        if (!infos.isEmpty() && camera != null) {
            Quaternionf baseRotation = new Quaternionf(camera.rotation()).mul(Axis.XN.rotationDegrees(180), new Quaternionf());
            for (Map.Entry<Identifier, List<DamageInfo>> group : infos.entrySet()) {
                RenderType renderType = LyraRenderTypes.texture(group.getKey(), false);
                int totalVertices = 0;
                for (DamageInfo info : group.getValue()) {
                    totalVertices += info.vertexCount();
                }
                if (totalVertices == 0) {
                    continue;
                }
                if (totalVertices * 5 > xyzuvData.length) {
                    int newVertexCapacity = Math.max(totalVertices, xyzuvData.length / 5 * 2);
                    xyzuvData = new float[newVertexCapacity * 5];
                    colorData = new int[newVertexCapacity];
                }
                collector.submitCustomGeometry(new PoseStack(), renderType, (pose, consumer) -> {
                    int vertexIndex = 0;
                    for (DamageInfo info : group.getValue()) {
                        info.render(xyzuvData, colorData, vertexIndex, baseRotation, camPos, partialTick);
                        vertexIndex += info.vertexCount();
                    }
                    RenderUtil.writeVertices(consumer, xyzuvData, colorData, RenderUtil.FULL_LIGHT, vertexIndex);
                });
            }
        }
    }
}
