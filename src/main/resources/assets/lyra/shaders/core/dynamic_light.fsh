#version 150
#moj_import <lyra:dynamic_light.glsl>
uniform sampler2D SceneColor;
uniform sampler2D SceneDepth;
uniform vec2 ScreenSize;
uniform mat4 InverseProjection;
uniform float FogStart;
uniform float FogEnd;
in vec2 texCoord;
out vec4 fragColor;
vec3 positionAt(ivec2 pixel) {
    pixel = clamp(pixel, ivec2(0), ivec2(ScreenSize) - 1);
    float depth = texelFetch(SceneDepth, pixel, 0).r;
    vec4 p = InverseProjection * vec4((vec2(pixel) + 0.5) / ScreenSize * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}
void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    vec4 color = texelFetch(SceneColor, pixel, 0);
    float depth = texelFetch(SceneDepth, pixel, 0).r;
    vec3 p = positionAt(pixel);
    if (depth < 1.0) {
        vec3 left = p - positionAt(pixel + ivec2(-1, 0));
        vec3 right = positionAt(pixel + ivec2(1, 0)) - p;
        vec3 down = p - positionAt(pixel + ivec2(0, -1));
        vec3 up = positionAt(pixel + ivec2(0, 1)) - p;
        vec3 n = cross(dot(left, left) < dot(right, right) ? left : right, dot(down, down) < dot(up, up) ? down : up);
        n = dot(n, n) > 1e-12 ? normalize(n) : vec3(0, 0, 1);
        if (dot(n, p) > 0.0) n = -n;
        float fog = 1.0 - smoothstep(FogStart, max(FogEnd, FogStart + 0.001), length(p));
        vec3 albedo = clamp(color.rgb / max(max(color.r, color.g), max(color.b, 0.08)), 0.0, 1.0);
        vec3 energy = lyra_irradiance(pixel, p, n) * fog;
        color.rgb = 1.0 - (1.0 - color.rgb) * exp(-albedo * energy);
    }
    color.rgb = 1.0 - (1.0 - color.rgb) * exp(-lyra_scattering(pixel, p));
    fragColor = color;
}
