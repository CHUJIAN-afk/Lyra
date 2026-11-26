#version 150
#moj_import <lyra:dynamic_light.glsl>
uniform sampler2D SceneDepth;
uniform vec2 ScreenSize;
uniform mat4 InverseProjection;
out vec4 fragColor;

vec3 positionAt(ivec2 pixel) {
    pixel = clamp(pixel, ivec2(0), ivec2(ScreenSize) - 1);
    float depth = texelFetch(SceneDepth, pixel, 0).r;
    vec4 p = InverseProjection * vec4((vec2(pixel) + 0.5) / ScreenSize * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    if (texelFetch(SceneDepth, pixel, 0).r >= 1.0) {
        fragColor = vec4(0.0);
    } else {
        vec3 p = positionAt(pixel);
        vec3 l = positionAt(pixel - ivec2(1, 0));
        vec3 r = positionAt(pixel + ivec2(1, 0));
        vec3 d = positionAt(pixel - ivec2(0, 1));
        vec3 u = positionAt(pixel + ivec2(0, 1));
        ivec2 last = ivec2(ScreenSize) - 1;
        bool left = pixel.x == last.x || (pixel.x > 0 && abs(l.z - p.z) < abs(r.z - p.z));
        bool down = pixel.y == last.y || (pixel.y > 0 && abs(d.z - p.z) < abs(u.z - p.z));
        vec3 normal = cross(left ? p - l : r - p, down ? p - d : u - p);
        normal = dot(normal, normal) > 1e-24 ? normalize(normal) : -normalize(p);
        if (dot(normal, p) > 0.0) normal = -normal;
        vec3 worldNormal = normalize(mat3(LyraViewInverse) * normal);
        vec3 axes = abs(worldNormal);
        float major = max(axes.x, max(axes.y, axes.z));
        if (major > 0.985) {
            worldNormal = axes.y == major ? vec3(0, sign(worldNormal.y), 0)
                    : axes.x == major ? vec3(sign(worldNormal.x), 0, 0) : vec3(0, 0, sign(worldNormal.z));
            normal = transpose(mat3(LyraViewInverse)) * worldNormal;
        }
        fragColor = vec4(min(lyra_irradiance(pixel, p, normal), vec3(6e4)), 1.0);
    }
}
