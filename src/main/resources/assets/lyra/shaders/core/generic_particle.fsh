#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec2 texCoord;
in vec2 corner;
flat in vec4 centerColor;
flat in vec4 edgeColor;
flat in float spriteWidth;

out vec4 fragColor;

void main() {
    vec4 edge = texture(Sampler0, texCoord) * edgeColor * ColorModulator;
    if (edge.a < 0.1) {
        edge.a = 0.0;
    }
    vec4 center = vec4(0.0);
    if (all(lessThanEqual(abs(corner), vec2(1.0 / 3.0)))) {
        float halfWidth = spriteWidth * 0.5 / float(textureSize(Sampler0, 0).x);
        center = textureLod(Sampler0, texCoord - vec2(halfWidth, 0.0), 0.0) * centerColor * ColorModulator;
        if (center.a < 0.1) {
            center.a = 0.0;
        }
    }
    float centerAlpha = center.a * (1.0 - edge.a);
    float alpha = edge.a + centerAlpha;
    if (alpha == 0.0) {
        discard;
    }
    vec3 rgb = (edge.rgb * edge.a + center.rgb * centerAlpha) / alpha;
    fragColor = linear_fog(vec4(rgb, alpha), vertexDistance, FogStart, FogEnd, FogColor);
}
