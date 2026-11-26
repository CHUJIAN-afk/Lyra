#version 150
uniform sampler2D SceneColor;
uniform sampler2D SceneDepth;
uniform sampler2D VolumeLight;
uniform sampler2D Irradiance;
uniform vec2 ScreenSize;
uniform mat4 InverseProjection;
uniform float FogStart;
uniform float FogEnd;
in vec2 texCoord;
out vec4 fragColor;
vec3 positionAt(ivec2 pixel, float depth) {
    vec4 p = InverseProjection * vec4((vec2(pixel) + 0.5) / ScreenSize * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}
void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    vec4 color = texelFetch(SceneColor, pixel, 0);
    float depth = texelFetch(SceneDepth, pixel, 0).r;
    if (depth < 1.0) {
        vec3 p = positionAt(pixel, depth);
        float fog = 1.0 - smoothstep(FogStart, max(FogEnd, FogStart + 0.001), length(p));
        vec3 albedo = clamp(color.rgb / max(max(color.r, color.g), max(color.b, 0.08)), 0.0, 1.0);
        vec3 energy = texelFetch(Irradiance, pixel, 0).rgb * fog;
        color.rgb = 1.0 - (1.0 - color.rgb) * exp(-albedo * energy);
    }
    color.rgb = 1.0 - (1.0 - color.rgb) * exp(-texelFetch(VolumeLight, pixel, 0).rgb);
    fragColor = color;
}
