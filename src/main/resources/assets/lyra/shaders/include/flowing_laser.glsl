#ifndef LYRA_FLOW_LIBRARY
#define LYRA_FLOW_LIBRARY

#ifdef LYRA_FLOW_LIGHTING
uniform sampler2D LyraFlowData;

struct LyraFlowProfile {
    vec4 radius;
    vec4 shape;
    vec4 inner;
    vec4 edge;
};

LyraFlowProfile lyra_flowProfile(float tag) {
    int index = int(tag + 0.5) - 2;
    ivec2 base = ivec2((index & 255) * 4, index >> 8);
    return LyraFlowProfile(texelFetch(LyraFlowData, base, 0), texelFetch(LyraFlowData, base + ivec2(1, 0), 0),
            texelFetch(LyraFlowData, base + ivec2(2, 0), 0), texelFetch(LyraFlowData, base + ivec2(3, 0), 0));
}

float lyra_flowRadius(float progress, LyraFlowProfile profile) {
    if (progress < 0.5) {
        return mix(profile.radius.x, profile.radius.y, 1.0 - pow(1.0 - progress * 2.0, profile.shape.x));
    }
    return mix(profile.radius.z, profile.radius.y, 1.0 - pow(progress * 2.0 - 1.0, profile.shape.y));
}

vec4 lyra_flowSample(vec3 point, vec3 axis, LyraFlowProfile profile) {
    float along = dot(point, axis);
    if (along < 0.0 || along > profile.radius.w) return vec4(0.0);
    float progress = along / profile.radius.w;
    float radius = lyra_flowRadius(progress, profile);
    vec3 radial = point - axis * along;
    float ratio = length(radial) / max(radius, 1e-5);
    if (ratio >= profile.shape.w) return vec4(0.0);
    float core = 1.0 - smoothstep(profile.shape.z * 0.75, profile.shape.z, ratio);
    float shell = 1.0 - smoothstep(profile.shape.z, profile.shape.w, ratio);
    vec3 tangent = normalize(cross(abs(axis.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0), axis));
    float angle = dot(radial, radial) > 1e-10 ? atan(dot(radial, cross(axis, tangent)), dot(radial, tangent)) : 0.0;
    float travel = along - profile.edge.w * profile.inner.w;
    float streaks = pow(0.5 + 0.5 * sin(angle * 9.0 + travel * 2.4 + sin(angle * 3.0 + travel * 0.8)), 7.0);
    float pulses = 0.65 + 0.35 * pow(0.5 + 0.5 * sin(travel * 3.2), 3.0);
    vec3 color = mix(profile.edge.rgb, profile.inner.rgb, core);
    color *= 0.8 + core * 1.7 + streaks * (1.0 - core) * 2.0;
    return vec4(color, shell * pulses * (0.55 + core * 1.3 + streaks * 0.8));
}

float lyra_flowSpot(vec3 direction, inout vec4 color, vec4 axis, vec4 shadow) {
    LyraFlowProfile profile = lyra_flowProfile(color.w);
    vec3 point = -direction * sqrt(max(lyra_flowDistanceSq, 0.0));
    vec4 sampleColor = lyra_flowSample(point, axis.xyz, profile);
    color.rgb *= sampleColor.rgb;
    return sampleColor.a * (lyra_flowDistanceSq + 1.0);
}

#ifdef LYRA_FLOW_VOLUME
vec3 lyra_flowVolume(vec3 o, vec3 d, vec4 v0, vec4 v1, vec4 v2, float first, float last, int index, float stratum) {
    LyraFlowProfile profile = lyra_flowProfile(v1.w);
    vec3 relative = o - v0.xyz;
    float along = dot(relative, v2.xyz);
    float rate = dot(d, v2.xyz);
    if (abs(rate) < 1e-6) {
        if (along < 0.0 || along > profile.radius.w) return vec3(0.0);
    } else {
        float a = -along / rate;
        float b = (profile.radius.w - along) / rate;
        first = max(first, min(a, b));
        last = min(last, max(a, b));
    }
    float radius = max(profile.radius.x, max(profile.radius.y, profile.radius.z)) * profile.shape.w;
    vec3 perpendicular = relative - v2.xyz * along;
    vec3 perpendicularRay = d - v2.xyz * rate;
    float aa = dot(perpendicularRay, perpendicularRay);
    float bb = dot(perpendicular, perpendicularRay);
    float cc = dot(perpendicular, perpendicular) - radius * radius;
    if (aa < 1e-6) {
        if (cc > 0.0) return vec3(0.0);
    } else {
        float discriminant = bb * bb - aa * cc;
        if (discriminant <= 0.0) return vec3(0.0);
        float root = sqrt(discriminant);
        first = max(first, (-bb - root) / aa);
        last = min(last, (-bb + root) / aa);
    }
    if (last <= first) return vec3(0.0);
    int steps = clamp(int(VolumeParams.y) * 8, 24, 96);
    float stepSize = (last - first) / float(steps);
    bool shadowed = index < int(VolumeParams.z + 0.5);
    vec3 sum = vec3(0.0);
    for (int i = 0; i < steps; i++) {
        vec3 point = relative + d * (first + (float(i) + stratum) * stepSize);
        vec4 sampleColor = lyra_flowSample(point, v2.xyz, profile);
        if (sampleColor.a > 0.0) {
            float visibility = shadowed ? photon_mapVisibility(index, point, length(point)) : 1.0;
            sum += sampleColor.rgb * sampleColor.a * visibility;
        }
    }
    return sum * v1.rgb * stepSize * 12.0;
}
#endif
#endif
#endif
