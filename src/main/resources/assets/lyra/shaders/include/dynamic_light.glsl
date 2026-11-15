uniform sampler2D LyraLightData;
uniform sampler2D LyraLightTiles;
uniform sampler2D LyraSceneDepth;
uniform ivec2 LyraTileCount;
uniform mat4 LyraProjection;
uniform mat4 LyraViewInverse;
uniform int LyraShadowSteps;
uniform int LyraVolumeSteps;

float lyra_visibility(vec3 p, vec3 lightPosition, float radius) {
    for (int step = 1; step <= LyraShadowSteps; step++) {
        vec3 samplePosition = mix(p, lightPosition, float(step) / float(LyraShadowSteps + 1));
        vec4 clip = LyraProjection * vec4(samplePosition, 1.0);
        vec3 screen = clip.xyz / clip.w * 0.5 + 0.5;
        if (clip.w > 0.0 && all(greaterThan(screen.xy, vec2(0))) && all(lessThan(screen.xy, vec2(1)))) {
            float depth = textureLod(LyraSceneDepth, screen.xy, 0.0).r;
            float bias = max(0.0001, radius * 0.001);
            if (screen.z > depth + bias && screen.z - depth < 0.02) {
                return 0.0;
            }
        }
    }
    return 1.0;
}

vec3 lyra_irradiance(ivec2 pixel, vec3 p, vec3 normal) {
    ivec2 tile = clamp(pixel / 64, ivec2(0), LyraTileCount - 1);
    int base = tile.x * 33;
    int count = clamp(int(texelFetch(LyraLightTiles, ivec2(base, tile.y), 0).r), 0, 32);
    vec3 result = vec3(0);
    for (int entry = 1; entry <= count; entry++) {
        int index = int(texelFetch(LyraLightTiles, ivec2(base + entry, tile.y), 0).r);
        vec4 position = texelFetch(LyraLightData, ivec2(0, index), 0);
        vec4 color = texelFetch(LyraLightData, ivec2(1, index), 0);
        vec4 cone = texelFetch(LyraLightData, ivec2(2, index), 0);
        vec4 settings = texelFetch(LyraLightData, ivec2(3, index), 0);
        vec3 delta = position.xyz - p;
        float distance = length(delta);
        if (distance < position.w) {
            vec3 direction = delta / max(distance, 0.0001);
            float edge = max(0.0, 1.0 - pow(distance / position.w, 4.0));
            float energy = edge * edge / (1.0 + distance * distance);
            if (color.w > 0.5) {
                energy *= smoothstep(cone.w, max(cone.w + 0.0001, settings.x), dot(-direction, cone.xyz));
            }
            if (settings.y > 0.5) {
                energy *= lyra_visibility(p + normal * 0.03, position.xyz, settings.z);
            }
            result += color.rgb * energy * max(dot(normal, direction), 0.0);
        }
    }
    return result;
}

vec3 lyra_scattering(ivec2 pixel, vec3 surface) {
    float reach = min(length(surface), 128.0);
    vec3 ray = normalize(surface);
    ivec2 tile = clamp(pixel / 64, ivec2(0), LyraTileCount - 1);
    int base = tile.x * 33;
    int count = clamp(int(texelFetch(LyraLightTiles, ivec2(base, tile.y), 0).r), 0, 32);
    vec3 result = vec3(0);
    for (int entry = 1; entry <= count; entry++) {
        int index = int(texelFetch(LyraLightTiles, ivec2(base + entry, tile.y), 0).r);
        vec4 settings = texelFetch(LyraLightData, ivec2(3, index), 0);
        if (settings.w > 0.0) {
            vec4 position = texelFetch(LyraLightData, ivec2(0, index), 0);
            vec4 color = texelFetch(LyraLightData, ivec2(1, index), 0);
            vec4 cone = texelFetch(LyraLightData, ivec2(2, index), 0);
            float center = dot(ray, position.xyz);
            float radial = dot(position.xyz, position.xyz) - center * center;
            float halfSpan = sqrt(max(position.w * position.w - radial, 0.0));
            float start = max(0.0, center - halfSpan);
            float end = min(reach, center + halfSpan);
            if (end > start) {
                float stepSize = (end - start) / float(LyraVolumeSteps);
                float visibility = settings.y > 0.5 ? lyra_visibility(ray * ((start + end) * 0.5), position.xyz, settings.z) : 1.0;
                for (int step = 0; step < LyraVolumeSteps; step++) {
                    vec3 p = ray * (start + (float(step) + 0.5) * stepSize);
                    vec3 delta = p - position.xyz;
                    float distance = length(delta);
                    float edge = max(0.0, 1.0 - pow(distance / position.w, 4.0));
                    float energy = edge * edge / (1.0 + distance * distance);
                    if (color.w > 0.5) {
                        energy *= smoothstep(cone.w, max(cone.w + 0.0001, settings.x), dot(delta / max(distance, 0.0001), cone.xyz));
                    }
                    energy *= visibility;
                    result += color.rgb * energy * settings.w * stepSize * 0.03;
                }
            }
        }
    }
    return result;
}
