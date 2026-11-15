#version 150
uniform sampler2D Source;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec3 color = vec3(0.0);
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            float weight = float((x == 0 ? 2 : 1) * (y == 0 ? 2 : 1)) / 16.0;
            color += texture(Source, texCoord + vec2(x, y) * 0.005).rgb * weight;
        }
    }
    fragColor = vec4(color, 0.0);
}
