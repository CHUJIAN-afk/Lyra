package first.lyra.common.attachmentEntity;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Ellipse {

    private final Vec3 pointA;
    private final Vec3 pointB;
    private final Vec3 planeNormal;
    private final float curvature;

    private final Vec3 center;
    private final Vec3 major;
    private final Vec3 minor;
    private final Vec3 majorDir;
    private final Vec3 minorDir;

    public Ellipse(Vec3 pointA, Vec3 pointB, Vec3 planeNormal, float curvature) {
        this.pointA = pointA;
        this.pointB = pointB;
        this.planeNormal = planeNormal;
        this.curvature = curvature;

        this.major = pointB.subtract(pointA).scale(0.5);
        this.center = pointA.add(major);
        this.majorDir = major.normalize();

        Vec3 mDir = planeNormal.cross(majorDir).normalize();
        if (mDir.lengthSqr() < 1e-5) mDir = majorDir.cross(new Vec3(0, 1, 0)).normalize();
        this.minorDir = mDir;
        this.minor = minorDir.scale(major.length() * curvature);
    }

    public static @NotNull Vec3 randomPlaneNormal(RandomSource random, Vec3 pointA, Vec3 pointB) {
        Vec3 majorDir = pointB.subtract(pointA).normalize();

        float angle = random.nextFloat() * Mth.TWO_PI;
        Quaternionf q = new Quaternionf().rotateAxis(angle, (float) majorDir.x, (float) majorDir.y, (float) majorDir.z);

        Vec3 initNormal = majorDir.cross(new Vec3(0, 1, 0)).normalize();
        if (initNormal.lengthSqr() < 1e-5) {
            initNormal = majorDir.cross(new Vec3(1, 0, 0)).normalize();
        }
        Vector3f rotated = new Vector3f((float) initNormal.x, (float) initNormal.y, (float) initNormal.z).rotate(q);
        return new Vec3(rotated.x(), rotated.y(), rotated.z()).normalize();
    }

    public Vec3 getPoint(float progress) {
        float biasedT = progress - 0.08f * Mth.sin(progress * Mth.TWO_PI);
        float theta = biasedT * Mth.TWO_PI;
        return center.add(major.scale(Math.cos(theta))).add(minor.scale(Math.sin(theta)));
    }

    public Vec3 getTangent(float progress) {
        float biasedT = progress - 0.08f * Mth.sin(progress * Mth.TWO_PI);
        float theta = biasedT * Mth.TWO_PI;
        return major.scale(-Math.sin(theta)).add(minor.scale(Math.cos(theta))).normalize();
    }

    public Vec3 getCenter() {
        return center;
    }

    public Vec3 getMajorDir() {
        return majorDir;
    }

    public Vec3 getMinorDir() {
        return minorDir;
    }

    public Vec3 getPlaneNormal() {
        return planeNormal;
    }

    public float getCurvature() {
        return curvature;
    }

    public Vec3 getPointA() {
        return pointA;
    }

    public Vec3 getPointB() {
        return pointB;
    }

    public Vec3 getFocusNearA() {
        double c = Math.sqrt(Math.max(0, major.lengthSqr() - minor.lengthSqr()));
        return center.add(majorDir.scale(-c));
    }

    public Vec3 getFocusNearB() {
        double c = Math.sqrt(Math.max(0, major.lengthSqr() - minor.lengthSqr()));
        return center.add(majorDir.scale(c));
    }
}
