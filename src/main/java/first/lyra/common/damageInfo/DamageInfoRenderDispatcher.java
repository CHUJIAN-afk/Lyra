package first.lyra.common.damageInfo;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import first.lyra.common.attachment.DamageInfoData;
import first.lyra.register.LyraAttachmentRegister;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Map;

/**
 * 伤害数字渲染调度器。
 * <p>
 * 按贴图分组获取 VertexConsumer，逐条委托 {@link DamageInfo#render} 完成渲染。
 * 相机朝向（{@code cameraOrientation × XN(180)}）每帧预计算一次，所有数字共享。
 * </p>
 */
public class DamageInfoRenderDispatcher {

    public static void render(Level level, Camera camera, MultiBufferSource bufferSource, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        DamageInfoData data = level.getData(LyraAttachmentRegister.DamageInfoData);
        Map<ResourceLocation, List<DamageInfo>> infos = data.getActiveInfos();
        if (!infos.isEmpty()) {
            Vec3 camPos = camera.getPosition();
            EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
            Quaternionf baseRotation = dispatcher.cameraOrientation().mul(Axis.XN.rotationDegrees(180), new Quaternionf());
            for (Map.Entry<ResourceLocation, List<DamageInfo>> group : infos.entrySet()) {
                VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(group.getKey(), false));
                for (DamageInfo info : group.getValue()) {
                    info.render(consumer, baseRotation, camPos, partialTick);
                }
            }
        }
    }
}
