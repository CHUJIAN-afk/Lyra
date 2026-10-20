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

struct LyraFlowFrame {
    vec3 axis;
    vec3 tangent;
    vec3 bitangent;
    vec3 jitter;
    float period;
    float phase;
};

vec4 lyra_flowHash(vec2 p) {
    vec4 h = fract(p.xyxy * vec4(0.1031, 0.1030, 0.0973, 0.1099));
    h += dot(h, h.wzxy + 33.33);
    return fract((h.xxyz + h.yzzw) * h.zywx);
}

LyraFlowFrame lyra_flowFrame(vec3 axis, LyraFlowProfile profile) {
    vec3 worldAxis = normalize(mat3(PhotonViewInverse) * axis);
    vec3 worldTangent = normalize(cross(abs(worldAxis.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0), worldAxis));
    vec3 tangent = normalize(transpose(mat3(PhotonViewInverse)) * worldTangent);
    float radius = max(profile.radius.x, max(profile.radius.y, profile.radius.z)) * profile.shape.w;
    float period = max(1.0, min(profile.radius.w * 0.35, radius * 6.0));
    float phase = profile.edge.w * profile.inner.w;
    float clock = phase / period * 7.0;
    float t = fract(clock);
    t = t * t * (3.0 - 2.0 * t);
    vec3 jitter = mix(lyra_flowHash(vec2(floor(clock), 17.0)).xyz,
            lyra_flowHash(vec2(floor(clock) + 1.0, 17.0)).xyz, t) * 2.0 - 1.0;
    return LyraFlowFrame(axis, tangent, cross(axis, tangent), jitter, period, phase);
}

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

vec2 lyra_flowSpindles(float angle, float radial, float travel, LyraFlowFrame frame) {
    float lane = angle * (12.0 / 6.28318530718);
    float base = floor(lane);
    float body = 0.0;
    float highlight = 0.0;
    for (int i = -1; i <= 1; i++) {
        float id = base + float(i);
        float seed = mod(id, 12.0);
        float packet = travel / frame.period + fract(seed * 0.61803398875);
        vec4 random = lyra_flowHash(vec2(seed + 3.0, floor(packet) + 7.0));
        float along = (fract(packet) - 0.5) / (0.3 + random.x * 0.17);
        float taper = max(1.0 - along * along, 0.0);
        if (taper > 0.0) {
            float center = id + 0.5 + (random.y - 0.5) * 0.35 + frame.jitter.x * 0.14;
            float radius = 0.16 + random.z * 0.72 + frame.jitter.y * 0.045;
            vec2 delta = vec2((lane - center) / ((0.3 + random.w * 0.35) * taper),
                    (radial - radius) / ((0.1 + random.y * 0.12) * taper));
            float distanceSq = dot(delta, delta);
            float pulse = taper * (0.7 + random.x * 0.3);
            body = max(body, (1.0 - smoothstep(0.15, 1.0, distanceSq)) * pulse);
            highlight = max(highlight, (1.0 - smoothstep(0.015, 0.16, distanceSq)) * pulse);
        }
    }
    return vec2(body, highlight);
}

vec4 lyra_flowSample(vec3 point, LyraFlowFrame frame, LyraFlowProfile profile) {
    float along = dot(point, frame.axis);
    if (along < 0.0 || along > profile.radius.w) return vec4(0.0);
    float progress = along / profile.radius.w;
    float radius = lyra_flowRadius(progress, profile);
    vec3 radial = point - frame.axis * along;
    float ratio = length(radial) / max(radius, 1e-5);
    if (ratio >= profile.shape.w) return vec4(0.0);
    float core = 1.0 - smoothstep(profile.shape.z * 0.75, profile.shape.z, ratio);
    float shell = 1.0 - smoothstep(profile.shape.z, profile.shape.w, ratio);
    float angle = dot(radial, radial) > 1e-10 ? atan(dot(radial, frame.bitangent), dot(radial, frame.tangent)) : 0.0;
    vec2 pulse = lyra_flowSpindles(angle, ratio / profile.shape.w, along - frame.phase, frame);
    vec3 color = mix(profile.edge.rgb * 0.65, profile.inner.rgb * 2.0, core);
    color = mix(color, profile.edge.rgb * 1.1, pulse.x * 0.9);
    color = mix(color, profile.inner.rgb * 2.4, pulse.y);
    float density = core * (2.4 - pulse.x * 1.5) + pulse.x * 1.8 + pulse.y * 1.2 + 0.035;
    density *= 0.94 + frame.jitter.z * pulse.x * 0.06;
    return vec4(color, shell * density);
}

float lyra_flowSpot(vec3 direction, inout vec4 color, vec4 axis, vec4 shadow) {
    LyraFlowProfile profile = lyra_flowProfile(color.w);
    vec3 point = -direction * sqrt(max(lyra_flowDistanceSq, 0.0));
    vec4 sampleColor = lyra_flowSample(point, lyra_flowFrame(axis.xyz, profile), profile);
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
    LyraFlowFrame frame = lyra_flowFrame(v2.xyz, profile);
    vec3 sum = vec3(0.0);
    float transmission = 1.0;
    for (int i = 0; i < steps; i++) {
        vec3 point = relative + d * (first + (float(i) + stratum) * stepSize);
        vec4 sampleColor = lyra_flowSample(point, frame, profile);
        if (sampleColor.a > 0.0) {
            float visibility = shadowed ? photon_mapVisibility(index, point, length(point)) : 1.0;
            float opacity = 1.0 - exp(-sampleColor.a * stepSize * 1.5);
            opacity *= visibility;
            sum += sampleColor.rgb * opacity * transmission;
            transmission *= 1.0 - opacity;
            if (transmission < 0.015) break;
        }
    }
    return sum * v1.rgb * 4.0;
}
#endif
#endif
#endif
