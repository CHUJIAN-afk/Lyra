package first.lyra.client.render.model.virtual;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.Entity;

import java.util.function.Consumer;

public final class VirtualEntityPose {

    private int ageTicks;
    private float bodyYawDegrees;
    private float headYawDegrees = Float.NaN;
    private float headPitchDegrees;
    private float limbSwingPosition;
    private float limbSwingAmount;
    private float attackProgress;
    private int hurtTime;
    private int deathTime;
    private float scale = 1;
    private int color = -1;
    private float alpha = 1;
    private int packedLight = LightTexture.FULL_BRIGHT;
    private boolean crouching;
    private boolean swimming;
    private Consumer<Entity> customizer;

    private VirtualEntityPose() {
    }

    public static VirtualEntityPose create() {
        return new VirtualEntityPose();
    }

    public VirtualEntityPose ageTicks(int ageTicks) {
        this.ageTicks = ageTicks;
        return this;
    }

    public VirtualEntityPose bodyYaw(float degrees) {
        this.bodyYawDegrees = degrees;
        return this;
    }

    public VirtualEntityPose headYaw(float degrees) {
        this.headYawDegrees = degrees;
        return this;
    }

    public VirtualEntityPose look(float bodyYawDegrees, float headYawDegrees, float headPitchDegrees) {
        this.bodyYawDegrees = bodyYawDegrees;
        this.headYawDegrees = headYawDegrees;
        this.headPitchDegrees = headPitchDegrees;
        return this;
    }

    public VirtualEntityPose walk(float limbSwingPosition, float limbSwingAmount) {
        this.limbSwingPosition = limbSwingPosition;
        this.limbSwingAmount = limbSwingAmount;
        return this;
    }

    public VirtualEntityPose attack(float progress) {
        this.attackProgress = progress;
        return this;
    }

    public VirtualEntityPose hurt(int hurtTime) {
        this.hurtTime = hurtTime;
        return this;
    }

    public VirtualEntityPose death(int deathTime) {
        this.deathTime = deathTime;
        return this;
    }

    public VirtualEntityPose crouch(boolean crouching) {
        this.crouching = crouching;
        return this;
    }

    public VirtualEntityPose swim(boolean swimming) {
        this.swimming = swimming;
        return this;
    }

    public VirtualEntityPose scale(float scale) {
        this.scale = scale;
        return this;
    }

    public VirtualEntityPose color(int argb) {
        this.color = argb;
        return this;
    }

    public VirtualEntityPose alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public VirtualEntityPose packedLight(int packedLight) {
        this.packedLight = packedLight;
        return this;
    }

    public VirtualEntityPose customize(Consumer<Entity> customizer) {
        this.customizer = customizer;
        return this;
    }

    int ageTicks() {
        return ageTicks;
    }

    float bodyYawDegrees() {
        return bodyYawDegrees;
    }

    float headYawDegrees() {
        return Float.isNaN(headYawDegrees) ? bodyYawDegrees : headYawDegrees;
    }

    float headPitchDegrees() {
        return headPitchDegrees;
    }

    float limbSwingPosition() {
        return limbSwingPosition;
    }

    float limbSwingAmount() {
        return limbSwingAmount;
    }

    float attackProgress() {
        return attackProgress;
    }

    int hurtTime() {
        return hurtTime;
    }

    int deathTime() {
        return deathTime;
    }

    float scale() {
        return scale;
    }

    int color() {
        return color;
    }

    float alpha() {
        return alpha;
    }

    int packedLight() {
        return packedLight;
    }

    boolean crouching() {
        return crouching;
    }

    boolean swimming() {
        return swimming;
    }

    Consumer<Entity> customizer() {
        return customizer;
    }
}
