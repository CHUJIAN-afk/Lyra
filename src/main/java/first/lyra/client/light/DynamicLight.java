package first.lyra.client.light;

import org.joml.Vector3d;
import org.joml.Vector3f;

public class DynamicLight {

    public final Vector3d position = new Vector3d();
    public final Vector3f color = new Vector3f(1);
    public final Vector3f direction = new Vector3f(0, -1, 0);
    public float intensity = 1;
    public float range = 8;
    public float innerAngle = 25;
    public float outerAngle = 40;
    public float sourceRadius = 0.1f;
    public float volumetric;
    public boolean castShadows;
    public boolean enabled = true;
    public boolean spotlight;

    public DynamicLight at(double x, double y, double z) {
        position.set(x, y, z);
        return this;
    }

    public DynamicLight color(float red, float green, float blue) {
        color.set(red, green, blue);
        return this;
    }

    public DynamicLight color(int red, int green, int blue) {
        return color(red / 255f, green / 255f, blue / 255f);
    }

    public DynamicLight intensity(float value) {
        intensity = value;
        return this;
    }

    public DynamicLight range(float value) {
        range = value;
        return this;
    }

    public DynamicLight point() {
        spotlight = false;
        return this;
    }

    public DynamicLight spot(float inner, float outer) {
        spotlight = true;
        innerAngle = inner;
        outerAngle = outer;
        return this;
    }

    public DynamicLight direction(float x, float y, float z) {
        direction.set(x, y, z).normalize();
        return this;
    }

    public DynamicLight shadows(boolean value) {
        castShadows = value;
        return this;
    }

    public DynamicLight sourceRadius(float value) {
        sourceRadius = value;
        return this;
    }

    public DynamicLight volumetric(float value) {
        volumetric = value;
        return this;
    }

    public DynamicLight reset() {
        position.zero();
        color.set(1);
        direction.set(0, -1, 0);
        intensity = 1;
        range = 8;
        innerAngle = 25;
        outerAngle = 40;
        sourceRadius = 0.1f;
        volumetric = 0;
        castShadows = false;
        enabled = true;
        spotlight = false;
        return this;
    }
}
