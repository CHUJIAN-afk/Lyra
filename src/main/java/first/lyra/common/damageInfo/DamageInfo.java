package first.lyra.common.damageInfo;

import com.mojang.blaze3d.vertex.VertexConsumer;
import first.lyra.utils.EasingCurve;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 伤害数字渲染数据。
 * <p>
 * 渲染参数由 {@link DamageInfoStyle} 驱动，贴图布局为 0-9 顺序排列后追加一个小数点贴图
 * （索引 10），UV = glyphIndex / GLYPH_COUNT。闪烁为纯亮度脉冲，每 10 tick（0.5 秒）一次。
 * </p>
 * <p>
 * 小数显示规则：低于 1 保留两位小数，低于 10 保留一位小数，不低于 10 不保留小数。
 * </p>
 */
public class DamageInfo {

    private final DamageInfoStyle style;
    private int lastLife;
    private int life;
    private Vec3 lastPos;
    private Vec3 pos;
    private Vec3 velocity;
    private final float drag;
    private final boolean critical;
    private final float roll;

    /** 缓存：格式化后的伤害值字符串（含小数点） */
    private final String text;

    public DamageInfo(DamageInfoStyle style, float damageAmount, Vec3 pos, Vec3 velocity, boolean critical) {
        this.style = style;
        this.lastLife = 0;
        this.life = 0;
        this.pos = pos;
        this.lastPos = pos;
        this.velocity = velocity;
        this.drag = 0.75f;
        this.critical = critical;
        this.roll = RandomSource.create(velocity.hashCode()).nextInt(-30, 30);
        this.text = formatDamage(damageAmount);
    }

    /**
     * 根据伤害值大小格式化显示文本：
     * <ul>
     *   <li>低于 1：保留两位小数</li>
     *   <li>低于 10：保留一位小数</li>
     *   <li>不低于 10：不保留小数</li>
     * </ul>
     */
    private static String formatDamage(float damageAmount) {
        if (damageAmount < 1f) {
            return String.format("%.2f", damageAmount);
        } else if (damageAmount < 10f) {
            return String.format("%.1f", damageAmount);
        } else {
            return String.valueOf((int) damageAmount);
        }
    }

    /**
     * 将文本字符映射为贴图字形索引：'0'-'9' → 0-9，'.' → 10。
     *
     * @throws IllegalArgumentException 遇到非数字、非小数点字符
     */
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

    /** 获取伤害数字贴图 */
    public Identifier getTexture() {
        return style.texture();
    }

    // ===================== 渲染 =====================

    /**
     * 完整渲染本条伤害数字：直接构造 Matrix4f + 顶点写入。
     * <p>
     * 绕过 PoseStack 以避免每条数字 3 次 mulPose（矩阵乘法 + 栈拷贝）。
     * 相机朝向（{@code cameraOrientation × XN(180)}）由调用方每帧预计算一次传入 {@code baseRotation}，
     * 本条数字只需在此基础上叠加 roll 旋转与平移。
     * </p>
     *
     * @param consumer      已绑定贴图的 VertexConsumer
     * @param baseRotation  相机朝向 + XN(180) 预乘旋转（每帧一次，所有数字共享）
     * @param camPos        相机世界坐标
     * @param partialTick   插值因子
     */
    public void render(VertexConsumer consumer, Quaternionf baseRotation, Vec3 camPos, float partialTick) {
        Vec3 renderPos = getRenderPos(partialTick);

        // 预计算渲染参数
        float scale = getRenderScale(partialTick);
        int color = getRenderColor(partialTick);
        float roll = getRenderRoll(partialTick);

        float size = style.renderSize() * scale;
        float spacingWorld = style.glyphSpacing() * style.renderSize() / style.glyphPixelWidth();
        float step = size + spacingWorld * scale;
        float halfSize = size * 0.5f;

        // 居中偏移：基于真实顶点跨度（首字符左缘到末字符右缘），缩放中心位于字符串几何中心
        // 顶点本地 x 减 totalWidth/2，使几何中心落在本地原点，再经矩阵旋转+平移
        float totalWidth = !text.isEmpty() ? (text.length() - 1) * step + size : 0f;
        float halfWidth = totalWidth / 2f;

        // 构造变换矩阵：baseRotation × ZN(roll) 旋转 + 平移到渲染位置（相对相机）
        // 平移在旋转之后（世界空间），故 offsetX 不应进平移列，而应在本地坐标里居中
        Matrix4f matrix = new Matrix4f()
                .rotate(baseRotation.rotateZ(roll * Mth.DEG_TO_RAD, new Quaternionf()))
                .setTranslation((float) (renderPos.x() - camPos.x()),
                                (float) (renderPos.y() - camPos.y()),
                                (float) (renderPos.z() - camPos.z()));

        int glyphPixelWidth = style.glyphPixelWidth();
        int length = text.length();
        Vector3f v = new Vector3f();
        for (int i = 0; i < length; i++) {
            int glyph = glyphIndex(text.charAt(i));
            float u0 = (float) (glyph * glyphPixelWidth) / style.textureWidth();
            float u1 = (float) ((glyph + 1) * glyphPixelWidth) / style.textureWidth();

            // 本地坐标先减 halfWidth 居中，再变换
            float x0 = i * step - halfWidth;
            float x1 = x0 + size;

            // 4 个顶点，逐个变换后走 10 参数 fast path
            matrix.transformPosition(x0, -halfSize, 0, v);
            consumer.addVertex(v.x, v.y, v.z, color, u0, 0f, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, 0, 0, 1);
            matrix.transformPosition(x0, halfSize, 0, v);
            consumer.addVertex(v.x, v.y, v.z, color, u0, 1f, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, 0, 0, 1);
            matrix.transformPosition(x1, halfSize, 0, v);
            consumer.addVertex(v.x, v.y, v.z, color, u1, 1f, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, 0, 0, 1);
            matrix.transformPosition(x1, -halfSize, 0, v);
            consumer.addVertex(v.x, v.y, v.z, color, u1, 0f, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, 0, 0, 1);
        }
    }

    /**
     * 获取渲染颜色（ARGB）。
     * <p>
     * 暴击使用 criticalColor，普通使用 color。
     * 闪烁为纯亮度脉冲：每 10 tick（0.5 秒）一次 sin 波，随 progress 衰减。
     * 透明度由 EASE_IN_OUT_QUAD 缓动曲线驱动淡入淡出。
     * </p>
     */
    private int getRenderColor(float partialTick) {
        float progress = Mth.lerp(partialTick, lastLife, life) / style.maxLife();
        int baseColor = critical ? style.criticalColor() : style.color();

        // 亮度脉冲：每 10 tick 一个完整周期
        float flicker = (Mth.sin(progress * style.maxLife() * Mth.PI / 5f) + 1f) * 0.5f;
        float weight = flicker * (1f - progress);

        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;

        // 亮度叠加：向 255 方向提亮
        r = Mth.lerpInt(weight, r, Math.min(255, (int) (r * 1.5f)));
        g = Mth.lerpInt(weight, g, Math.min(255, (int) (g * 1.5f)));
        b = Mth.lerpInt(weight, b, Math.min(255, (int) (b * 1.5f)));

        // 透明度：缓动淡入淡出
        float easedProgress = EasingCurve.EASE_IN_OUT_QUAD.apply(progress);
        int a;
        if (easedProgress < 0.2f) {
            a = Mth.lerpInt(easedProgress / 0.2f, 51, 255); // 0.2*255=51
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
