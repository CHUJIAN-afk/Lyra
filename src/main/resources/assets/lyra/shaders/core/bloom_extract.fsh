#version 150
#moj_import <lyra:bloom.glsl>
uniform sampler2D Source;
uniform float Threshold;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    fragColor = vec4(lyra_highlight(texture(Source, texCoord).rgb, Threshold), 0.0);
}
