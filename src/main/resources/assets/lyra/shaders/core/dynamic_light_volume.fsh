#version 150
#moj_import <lyra:dynamic_light.glsl>
uniform sampler2D SceneDepth;
uniform vec2 ScreenSize;
uniform mat4 InverseProjection;
out vec4 fragColor;

vec3 positionAt(ivec2 pixel, float depth) {
    vec4 p = InverseProjection * vec4((vec2(pixel) + 0.5) / ScreenSize * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    float depth = texelFetch(SceneDepth, pixel, 0).r;
    vec3 origin = positionAt(pixel, 0.0);
    vec3 ray = normalize(positionAt(pixel, 0.5) - origin);
    float reach = depth < 1.0 ? dot(positionAt(pixel, depth) - origin, ray) : 1e5;
    fragColor = vec4(lyra_scattering(pixel, origin, ray, reach), 1.0);
}
