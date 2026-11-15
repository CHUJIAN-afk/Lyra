#version 150
uniform sampler2D Sampler0;
uniform float Brightness;
uniform int Additive;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    vec4 color = texture(Sampler0, texCoord) * vertexColor;
    if (color.a < 0.001) discard;
    fragColor = vec4(color.rgb * color.a * Brightness, Additive != 0 ? 0.0 : color.a);
}
