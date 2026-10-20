package first.lyra.client.render.rendererHelper;

import com.lowdragmc.photon.client.compat.iris.IrisCompat;
import com.lowdragmc.photon.client.light.DynamicLight;
import com.lowdragmc.photon.client.light.LightSink;
import com.lowdragmc.photon.client.light.PhotonLights;
import first.lyra.client.light.FlowingLaserData;
import first.lyra.client.light.FlowingLaserProfile;
import first.lyra.client.light.LyraLights;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class FlowingLaserRendererHelper {

    private int innerColor = 0xFFFFFF;
    private int edgeColor = 0xFF3355;
    private float innerRadius = 0.6f;
    private float edgeRadius = 1f;
    private Vec3 start = Vec3.ZERO;
    private Vec3 direction = new Vec3(0, 0, 1);
    private float length = 16;
    private float expansion = 6;
    private float contraction = 6;
    private float startRadius = 0;
    private float processRadius = 2;
    private float endRadius = 0;
    private float flowSpeed = 8;
    private DynamicLight light = new DynamicLight().intensity(32).volumetric(1);

    private FlowingLaserRendererHelper() {
    }

    public static FlowingLaserRendererHelper builder() {
        return new FlowingLaserRendererHelper();
    }

    public FlowingLaserRendererHelper inner(int color, float radius) {
        innerColor = color;
        innerRadius = radius;
        return this;
    }

    public FlowingLaserRendererHelper edge(int color, float radius) {
        edgeColor = color;
        edgeRadius = radius;
        return this;
    }

    public FlowingLaserRendererHelper from(Vec3 start) {
        this.start = start;
        return this;
    }

    public FlowingLaserRendererHelper direction(Vec3 direction) {
        this.direction = direction;
        return this;
    }

    public FlowingLaserRendererHelper length(float length) {
        this.length = length;
        return this;
    }

    public FlowingLaserRendererHelper spread(float expansion, float contraction) {
        this.expansion = expansion;
        this.contraction = contraction;
        return this;
    }

    public FlowingLaserRendererHelper radius(float start, float process, float end) {
        startRadius = start;
        processRadius = process;
        endRadius = end;
        return this;
    }

    public FlowingLaserRendererHelper flowSpeed(float flowSpeed) {
        this.flowSpeed = flowSpeed;
        return this;
    }

    public FlowingLaserRendererHelper light(DynamicLight light) {
        this.light = light;
        return this;
    }

    public void render() {
        render(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
    }

    public void render(float partialTick) {
        submit(LyraLights::next, partialTick);
    }

    public void submit(LightSink sink, float partialTick) {
        var level = Minecraft.getInstance().level;
        float radius = Math.max(startRadius, Math.max(processRadius, endRadius)) * edgeRadius;
        if (level != null && length > 0 && radius > 0 && direction.lengthSqr() > 0 && PhotonLights.isEnabled() && !IrisCompat.isShadowPass()) {
            Vec3 axis = direction.normalize();
            DynamicLight value = sink.next().set(light).at(start.x, start.y, start.z)
                    .direction((float) axis.x, (float) axis.y, (float) axis.z)
                    .spot(0, 89.9f).range((float) Math.sqrt((double) length * length + (double) radius * radius));
            float time = (level.getGameTime() % 240000 + partialTick) / 20f;
            ((FlowingLaserData.Access) value).lyra$setFlowingLaser(new FlowingLaserProfile(innerColor, edgeColor,
                    innerRadius, edgeRadius, length, startRadius, processRadius, endRadius, expansion, contraction, flowSpeed, time));
        }
    }
}
