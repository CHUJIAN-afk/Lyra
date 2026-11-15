#version 150
uniform sampler2D Source;
uniform sampler2D Bloom0;
uniform sampler2D Bloom1;
uniform sampler2D Bloom2;
uniform sampler2D Bloom3;
uniform float Strength;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec4 source = texture(Source, texCoord);
    vec3 glow = texture(Bloom0, texCoord).rgb * 0.4 + texture(Bloom1, texCoord).rgb * 0.3
        + texture(Bloom2, texCoord).rgb * 0.2 + texture(Bloom3, texCoord).rgb * 0.1;
    fragColor = vec4(source.rgb + glow * Strength, source.a);
}
