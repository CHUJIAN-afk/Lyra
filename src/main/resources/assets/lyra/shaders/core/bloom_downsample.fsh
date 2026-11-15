#version 150
uniform sampler2D Source;
uniform vec2 TexelSize;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec3 color = texture(Source, texCoord).rgb * 0.125;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            if (x != 0 || y != 0) {
                float weight = x == 0 || y == 0 ? 0.0625 : 0.03125;
                color += texture(Source, texCoord + vec2(x, y) * TexelSize * 2.0).rgb * weight;
            }
            if (x != 0 && y != 0) {
                color += texture(Source, texCoord + vec2(x, y) * TexelSize).rgb * 0.125;
            }
        }
    }
    fragColor = vec4(color, 0.0);
}
