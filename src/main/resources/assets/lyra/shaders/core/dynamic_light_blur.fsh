#version 150
uniform sampler2D LightBuffer;
uniform sampler2D SceneDepth;
uniform vec2 ScreenSize;
uniform mat4 InverseProjection;
uniform vec2 BlurDirection;
uniform float SkyDepth;
out vec4 fragColor;

float depthAt(ivec2 pixel) {
    float depth = texelFetch(SceneDepth, pixel, 0).r;
    vec4 p = InverseProjection * vec4((vec2(pixel) + 0.5) / ScreenSize * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return depth < 1.0 ? -p.z / p.w : SkyDepth;
}

void main() {
    ivec2 center = ivec2(gl_FragCoord.xy);
    ivec2 last = ivec2(ScreenSize) - 1;
    ivec2 axis = ivec2(BlurDirection);
    float depth = depthAt(center);
    vec3 value = texelFetch(LightBuffer, center, 0).rgb;
    if (depth < 0.0) {
        fragColor = vec4(value, 1.0);
    } else {
        float positive = depthAt(clamp(center + axis, ivec2(0), last));
        float negative = depthAt(clamp(center - axis, ivec2(0), last));
        float slope = 0.0;
        if (positive > 0.0 && negative > 0.0) {
            float forward = 1.0 / positive - 1.0 / depth;
            float backward = 1.0 / depth - 1.0 / negative;
            slope = abs(forward) < abs(backward) ? forward : backward;
        }
        vec3 sum = value;
        float weightSum = 1.0;
        for (int offset = 1; offset <= 3; offset++) {
            for (int side = -1; side <= 1; side += 2) {
                ivec2 pixel = clamp(center + axis * (offset * side), ivec2(0), last);
                float sampleDepth = depthAt(pixel);
                if (sampleDepth >= 0.0) {
                    float expected = 1.0 / max(1.0 / depth + slope * float(offset * side), 1e-5);
                    float weight = (1.0 - float(offset) * 0.25) * exp(-abs(sampleDepth - expected) / (0.03 * depth + 0.05));
                    sum += texelFetch(LightBuffer, pixel, 0).rgb * weight;
                    weightSum += weight;
                }
            }
        }
        fragColor = vec4(sum / weightSum, 1.0);
    }
}
