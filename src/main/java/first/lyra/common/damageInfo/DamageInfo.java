package first.lyra.common.damageInfo;

import com.mojang.blaze3d.vertex.VertexConsumer;
import first.lyra.utils.EasingCurve;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DamageInfo {

    private final DamageInfoStyle style;
    private final float drag;
    private final boolean critical;
    private final float roll;
    private final String text;
    private int lastLife;
    private int life;
    private Vec3 lastPos;
    private Vec3 pos;
    private Vec3 velocity;

    public DamageInfo(DamageInfoStyle style, float damageAmount, Vec3 pos, Vec3 velocity, boolean critical) {
        this.style = style;
        this.pos = pos;
        this.lastPos = pos;
        this.velocity = velocity;
        this.drag = 0.75f;
        this.critical = critical;
        this.roll = RandomSource.create(velocity.hashCode()).nextInt(-30, 30);
        this.text = formatDamage(damageAmount);
    }

    private static String formatDamage(float damageAmount) {
        if (damageAmount < 1f) {
            return String.format("%.2f", damageAmount);
        }
        if (damageAmount < 10f) {
            return String.format("%.1f", damageAmount);
        }
        return String.valueOf((int) damageAmount);
    }

    private static int glyphIndex(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c == '.') {
            return 10;
        }
        throw new IllegalArgumentException("Unexpected damage glyph: " + c);
    }

    public boolean tick() {
        lastLife = life;
        life++;
        lastPos = pos;
        velocity = velocity.scale(drag);
        pos = pos.add(velocity);
        return isRemove();
    }

    public ResourceLocation getTexture() {
        return style.texture();
    }

    public void render(VertexConsumer consumer, Quaternionf baseRotation, Vec3 camPos, float partialTick) {
        Vec3 renderPos = getRenderPos(partialTick);
        float scale = getRenderScale(partialTick);
        int color = getRenderColor(partialTick);
        float roll = getRenderRoll(partialTick);

        float size = style.renderSize() * scale;
        float spacingWorld = style.glyphSpacing() * style.renderSize() / style.glyphPixelWidth();
        float step = size + spacingWorld * scale;
        float halfSize = size * 0.5f;
        float totalWidth = text.isEmpty() ? 0f : (text.length() - 1) * step + size;
        float halfWidth = totalWidth / 2f;
        int overlay = OverlayTexture.NO_OVERLAY;
        int light = LightTexture.FULL_BRIGHT;

        Matrix4f matrix = new Matrix4f()
                .rotate(baseRotation.rotateZ(roll * Mth.DEG_TO_RAD, new Quaternionf()))
                .setTranslation((float) (renderPos.x() - camPos.x()), (float) (renderPos.y() - camPos.y()), (float) (renderPos.z() - camPos.z()));

        int glyphPixelWidth = style.glyphPixelWidth();
        int length = text.length();
        Vector3f v = new Vector3f();
        for (int i = 0; i < length; i++) {
            int glyph = glyphIndex(text.charAt(i));
            float u0 = (float) (glyph * glyphPixelWidth) / style.textureWidth();
            float u1 = (float) ((glyph + 1) * glyphPixelWidth) / style.textureWidth();
            float x0 = i * step - halfWidth;
            float x1 = x0 + size;
            matrix.transformPosition(x0, -halfSize, 0f, v);
            consumer.addVertex(v.x, v.y, v.z).setColor(color).setUv(u0, 0f).setOverlay(overlay).setLight(light).setNormal(0f, 0f, 1f).endVertex();
            matrix.transformPosition(x0, halfSize, 0f, v);
            consumer.addVertex(v.x, v.y, v.z).setColor(color).setUv(u0, 1f).setOverlay(overlay).setLight(light).setNormal(0f, 0f, 1f).endVertex();
            matrix.transformPosition(x1, halfSize, 0f, v);
            consumer.addVertex(v.x, v.y, v.z).setColor(color).setUv(u1, 1f).setOverlay(overlay).setLight(light).setNormal(0f, 0f, 1f).endVertex();
            matrix.transformPosition(x1, -halfSize, 0f, v);
            consumer.addVertex(v.x, v.y, v.z).setColor(color).setUv(u1, 0f).setOverlay(overlay).setLight(light).setNormal(0f, 0f, 1f).endVertex();
        }
    }

    private int getRenderColor(float partialTick) {
        float progress = Mth.lerp(partialTick, lastLife, life) / style.maxLife();
        int baseColor = critical ? style.criticalColor() : style.color();
        float flicker = (Mth.sin(progress * style.maxLife() * Mth.PI / 5f) + 1f) * 0.5f;
        float weight = flicker * (1f - progress);
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;
        r = Mth.lerpInt(weight, r, Math.min(255, (int) (r * 1.5f)));
        g = Mth.lerpInt(weight, g, Math.min(255, (int) (g * 1.5f)));
        b = Mth.lerpInt(weight, b, Math.min(255, (int) (b * 1.5f)));

        float easedProgress = EasingCurve.EASE_IN_OUT_QUAD.apply(progress);
        int a;
        if (easedProgress < 0.2f) {
            a = Mth.lerpInt(easedProgress / 0.2f, 51, 255);
        } else if (easedProgress > 0.9f) {
            a = Mth.lerpInt(Math.min(1, (easedProgress - 0.9f) / 0.05f), 255, 0);
        } else {
            a = 255;
        }
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private float getRenderRoll(float partialTick) {
        float progress = Mth.lerp(partialTick, lastLife, life) / style.maxLife();
        return Mth.lerp(progress, roll, 0);
    }

    private float getRenderScale(float partialTick) {
        float progress = Mth.lerp(partialTick, lastLife, life) / style.maxLife();
        progress = EasingCurve.EASE_IN_OUT_QUAD.apply(progress);
        if (progress < 0.1) {
            return Mth.lerp(Math.min(progress / 0.05f, 1), 0.5f, 1.0f);
        }
        if (progress > 0.9f) {
            return Mth.lerp((progress - 0.9f) / 0.1f, 1.0f, 0f);
        }
        return 1;
    }

    public Vec3 getRenderPos(float partialTick) {
        return lastPos.lerp(pos, partialTick);
    }

    public boolean isRemove() {
        return life >= style.maxLife() - 1;
    }
}
