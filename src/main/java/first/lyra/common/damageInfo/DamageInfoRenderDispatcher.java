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
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Map;

/**
 * 伤害数字渲染调度器。
 * <p>
 * 按贴图分组提交自定义几何：组内所有数字的顶点先组装到连续缓冲
 * （{@link DamageInfo#render} 组装），回调末尾经 {@link RenderUtil#writeVertices}
 * MemorySegment 批量直写（BLOCK 格式）。
 * 相机朝向（{@code cameraOrientation × XN(180)}）每帧预计算一次，所有数字共享。
 * </p>
 * <p>
 * 26.2: MultiBufferSource 移除,渲染走 {@link SubmitNodeCollector#submitCustomGeometry}。
 * </p>
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
            // 26.2: cameraOrientation() 移除,相机朝向从 dispatcher.camera.rotation() 取
            Quaternionf baseRotation = new Quaternionf(camera.rotation()).mul(Axis.XN.rotationDegrees(180), new Quaternionf());
            for (Map.Entry<Identifier, List<DamageInfo>> group : infos.entrySet()) {
                RenderType renderType = LyraRenderTypes.textureTranslucent(group.getKey());
                int totalVertices = 0;
                for (DamageInfo info : group.getValue()) {
                    totalVertices += info.vertexCount();
                }
                if (totalVertices == 0) {
                    continue;
                }
                ensureCapacity(totalVertices);
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

    private static void ensureCapacity(int requiredVertexCount) {
        if (requiredVertexCount * 5 > xyzuvData.length) {
            int newVertexCapacity = Math.max(requiredVertexCount, xyzuvData.length / 5 * 2);
            xyzuvData = new float[newVertexCapacity * 5];
            colorData = new int[newVertexCapacity];
        }
    }
}
