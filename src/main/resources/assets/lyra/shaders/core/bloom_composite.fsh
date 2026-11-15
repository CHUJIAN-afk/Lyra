#version 150
#moj_import <lyra:bloom.glsl>
uniform sampler2D Source;
uniform sampler2D Bloom;
uniform float Strength;
uniform float Threshold;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec4 source = texture(Source, texCoord);
    vec3 glow = texture(Bloom, texCoord).rgb;
    fragColor = vec4(source.rgb + (glow - lyra_highlight(source.rgb, Threshold)) * Strength, source.a);
}
