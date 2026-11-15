#version 150
uniform sampler2D Source;
uniform vec2 Direction;
uniform float Threshold;
in vec2 texCoord;
out vec4 fragColor;
vec3 sampleColor(vec2 uv) {
    vec3 color = texture(Source, uv).rgb;
    float peak = max(color.r, max(color.g, color.b));
    return color * max(peak - Threshold, 0.0) / max(peak, 0.0001);
}
void main() {
    vec3 color = sampleColor(texCoord) * 0.227027;
    color += (sampleColor(texCoord + Direction * 1.384615) + sampleColor(texCoord - Direction * 1.384615)) * 0.316216;
    color += (sampleColor(texCoord + Direction * 3.230769) + sampleColor(texCoord - Direction * 3.230769)) * 0.070270;
    fragColor = vec4(color, 0.0);
}
