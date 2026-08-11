package first.lyra.common.damageInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import first.lyra.common.attachment.DamageInfoData;
import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Map;

/**
 * 伤害数字渲染调度器。
 * <p>
 * 按贴图分组提交自定义几何，逐条委托 {@link DamageInfo#render} 完成渲染。
 * 相机朝向（{@code cameraOrientation × XN(180)}）每帧预计算一次，所有数字共享。
 * </p>
 * <p>
 * 26.2: MultiBufferSource 移除,渲染走 {@link SubmitNodeCollector#submitCustomGeometry}。
 * </p>
 */
public class DamageInfoRenderDispatcher {

    public static void render(Level level, Vec3 camPos, SubmitNodeCollector collector, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        DamageInfoData data = level.getData(LyraAttachmentRegister.DamageInfoData);
        Map<Identifier, List<DamageInfo>> infos = data.getActiveInfos();
        Camera camera = minecraft.getEntityRenderDispatcher().camera;
        if (!infos.isEmpty() && camera != null) {
            // 26.2: cameraOrientation() 移除,相机朝向从 dispatcher.camera.rotation() 取
            Quaternionf baseRotation = new Quaternionf(camera.rotation()).mul(Axis.XN.rotationDegrees(180), new Quaternionf());
            for (Map.Entry<Identifier, List<DamageInfo>> group : infos.entrySet()) {
                RenderType renderType = RenderTypes.entityTranslucent(group.getKey());
                collector.submitCustomGeometry(new PoseStack(), renderType, (pose, consumer) -> {
                    for (DamageInfo info : group.getValue()) {
                        info.render(consumer, baseRotation, camPos, partialTick);
                    }
                });
            }
        }
    }
}
