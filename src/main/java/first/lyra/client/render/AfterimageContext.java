package first.lyra.client.render;

import software.bernie.geckolib.util.Color;

public class AfterimageContext {

    /** 叠乘模型原有颜色。 */
    public Color color = Color.WHITE;
    public float alpha = 0.35f;
    public int lifetime = 10;

    public AfterimageContext color(Color color) {
        this.color = color;
        return this;
    }

    public AfterimageContext alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    /** 存在时间，单位为 tick。 */
    public AfterimageContext lifetime(int lifetime) {
        this.lifetime = lifetime;
        return this;
    }
}
