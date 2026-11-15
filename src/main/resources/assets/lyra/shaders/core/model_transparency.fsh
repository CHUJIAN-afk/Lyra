#version 150

#moj_import <lyra:dynamic_light.glsl>

uniform sampler2D ModelColor;
uniform sampler2D ModelDepth;
uniform vec2 ScreenSize;
uniform mat4 InverseProjection;
uniform int LyraEnabled;
uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;
uniform int FogShape;
in vec2 atlasPosition;
flat in vec2 modelParams;
flat in vec3 modelTint;
out vec4 fragColor;

vec3 viewPosition(ivec2 screenPixel, float depth) {
    vec2 screen = vec2(screenPixel) + 0.5;
    vec4 p = InverseProjection * vec4(screen / ScreenSize * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

vec3 neighbour(ivec2 pixel, ivec2 screenPixel, ivec2 delta) {
    ivec2 screen = clamp(screenPixel + delta, ivec2(0), ivec2(ScreenSize) - 1);
    return viewPosition(screen, texelFetch(ModelDepth, pixel + screen - screenPixel, 0).r);
}

vec3 illuminate(vec3 color, ivec2 pixel, ivec2 screenPixel, float depth) {
    vec3 p = viewPosition(screenPixel, depth);
    vec3 left = p - neighbour(pixel, screenPixel, ivec2(-1, 0));
    vec3 right = neighbour(pixel, screenPixel, ivec2(1, 0)) - p;
    vec3 down = p - neighbour(pixel, screenPixel, ivec2(0, -1));
    vec3 up = neighbour(pixel, screenPixel, ivec2(0, 1)) - p;
    vec3 dx = dot(left, left) < dot(right, right) ? left : right;
    vec3 dy = dot(down, down) < dot(up, up) ? down : up;
    vec3 normal = cross(dx, dy);
    normal = dot(normal, normal) > 1e-12 ? normalize(normal) : vec3(0, 0, 1);
    if (dot(normal, p) > 0.0) {
        normal = -normal;
    }
    vec3 light = lyra_irradiance(screenPixel, p, normal);
    float distance = FogShape == 0 ? length(p) : max(length((LyraViewInverse * vec4(p, 1)).xz), abs((LyraViewInverse * vec4(p, 1)).y));
    float fog = smoothstep(FogStart, max(FogEnd, FogStart + 1e-4), distance) * FogColor.a;
    vec3 unfogged = fog < 0.999 ? clamp((color - FogColor.rgb * fog) / (1.0 - fog), 0.0, 1.0) : color;
    float bound = dot(unfogged, vec3(0.2126, 0.7152, 0.0722)) / 0.85;
    vec3 albedo = min(unfogged / max(modelParams.y, max(bound, 1e-4)), vec3(1));
    return 1.0 - (1.0 - color) * exp(-albedo * light * (1.0 - fog));
}

void main() {
    ivec2 screenPixel = ivec2(gl_FragCoord.xy);
    ivec2 pixel = ivec2(atlasPosition);
    vec4 color = texelFetch(ModelColor, pixel, 0);
    float depth = texelFetch(ModelDepth, pixel, 0).r;
    if (color.a <= 0.0 || depth >= 1.0) {
        discard;
    }
    color.rgb /= color.a;
    if (LyraEnabled != 0) {
        color.rgb = illuminate(color.rgb, pixel, screenPixel, depth);
    }
    color.rgb *= modelTint;
    gl_FragDepth = max(0.0, depth - 1e-7);
    fragColor = vec4(color.rgb, color.a * modelParams.x);
}
