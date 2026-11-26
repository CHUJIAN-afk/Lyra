uniform sampler2D LyraLightData;
uniform sampler2D LyraLightTiles;
#moj_import <lyra:dynamic_light_voxel.glsl>
uniform sampler2D LyraVisibility;
uniform ivec2 LyraTileCount;
uniform int LyraVolumeSteps;
uniform int LyraShadowSamples;

ivec2 lyra_lightCell(ivec2 pixel) {
    int multiply[4] = int[](0, 2, 3, 1);
    ivec2 cell = pixel & 3;
    return ivec2(cell.x ^ cell.y, cell.x ^ multiply[cell.y]);
}

float lyra_visibility(int index, vec3 p, vec3 lightPosition) {
    vec3 delta = lightPosition - p;
    vec3 fromLight = mat3(LyraViewInverse) * -delta;
    vec3 n = fromLight / max(abs(fromLight.x) + abs(fromLight.y) + abs(fromLight.z), 1e-20);
    vec2 uv = n.z >= 0.0 ? n.xy : (1.0 - abs(n.yx)) * vec2(n.x >= 0.0 ? 1.0 : -1.0, n.y >= 0.0 ? 1.0 : -1.0);
    ivec2 texel = ivec2(index % 8, index / 8) * 128 + clamp(ivec2((uv * 0.5 + 0.5) * 128.0), ivec2(0), ivec2(127));
    return clamp((texelFetch(LyraVisibility, texel, 0).r - length(delta)) * 2.0 + 1.0, 0.0, 1.0);
}

float lyra_surfaceVisibility(ivec2 pixel, int index, vec3 p, vec3 lightPosition, float radius) {
    vec3 delta = lightPosition - p;
    vec3 direction = delta / max(length(delta), 0.0001);
    vec3 tangent = cross(abs(direction.y) < 0.99 ? vec3(0, 1, 0) : vec3(1, 0, 0), direction);
    tangent /= max(length(tangent), 0.0001);
    vec3 bitangent = cross(direction, tangent);
    vec3 origin = lyra_voxelPosition(p);
    int samples = radius > 0.0 ? max(LyraShadowSamples, 1) : 1;
    float visibility = 0.0;
    for (int sourceSample = 0; sourceSample < samples; sourceSample++) {
        ivec2 cell = samples == 1 ? lyra_lightCell(pixel) : ivec2(sourceSample / 4, sourceSample & 3);
        float angle = (float(cell.y) + 0.5 + 0.25 * float(cell.x)) * 1.5707963 + float(index) * 1.3;
        float offset = max(radius, 0.0) * sqrt((float(cell.x) + 0.5) / 4.0);
        vec3 ray = delta + (tangent * cos(angle) + bitangent * sin(angle)) * offset;
        float distance = length(ray);
        float reach = max(distance - 0.3, 0.0);
        float hit = lyra_voxelTrace(origin, mat3(LyraViewInverse) * ray / max(distance, 0.0001), reach);
        visibility += hit >= reach ? 1.0 : 0.0;
    }
    return visibility / float(samples);
}

float lyra_spotCosine(vec3 delta, vec3 axis, float radius) {
    float axial = dot(delta, axis);
    float radial = length(delta - axis * axial);
    radial = axial >= 0.0 ? max(radial - radius, 0.0) : radial + radius;
    return axial >= 0.0 && radial == 0.0 ? 1.0 : axial / max(length(vec2(axial, radial)), 0.0001);
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
                float radius = texelFetch(LyraLightData, ivec2(4, index), 0).x;
                float cosine = radius > 0.0 ? lyra_spotCosine(-delta, cone.xyz, radius) : dot(-direction, cone.xyz);
                energy *= smoothstep(cone.w, settings.x, cosine);
            }
            if (settings.y > 0.5) {
                energy *= lyra_surfaceVisibility(pixel, index, p + normal * (0.03 + 0.002 * max(-p.z, 0.0)), position.xyz, settings.z);
            }
            result += color.rgb * energy * max(dot(normal, direction), 0.0);
        }
    }
    return result;
}

vec2 lyra_coneRange(vec3 origin, vec3 ray, vec3 position, vec3 axis, float cosOuter, float radius) {
    vec3 offset = origin - position;
    float rayAxis = dot(ray, axis);
    float offsetAxis = dot(offset, axis);
    float cosSquared = cosOuter * cosOuter;
    float quadratic = rayAxis * rayAxis - cosSquared;
    float linear = rayAxis * offsetAxis - cosSquared * dot(ray, offset);
    float constant = offsetAxis * offsetAxis - cosSquared * dot(offset, offset);
    if (radius > 0.0) {
        float spread = radius * cosOuter;
        float slope = sqrt(max(1.0 - cosSquared, 0.0));
        linear += spread * slope * rayAxis;
        constant += 2.0 * spread * slope * offsetAxis + spread * spread;
    }
    if (abs(quadratic) < 1e-6) {
        quadratic = quadratic >= 0.0 ? 1e-6 : -1e-6;
    }
    float discriminant = linear * linear - quadratic * constant;
    float start = -1e9;
    float end = 1e9;
    if (discriminant < 0.0) {
        if (quadratic < 0.0) {
            return vec2(1.0, 0.0);
        }
    } else {
        float q = -(linear + (linear >= 0.0 ? sqrt(discriminant) : -sqrt(discriminant)));
        float first = q / quadratic;
        float second = abs(q) > 1e-12 ? constant / q : first;
        if (quadratic < 0.0) {
            start = min(first, second);
            end = max(first, second);
        } else if (rayAxis > 0.0) {
            start = max(first, second);
        } else {
            end = min(first, second);
        }
    }
    if (abs(rayAxis) < 1e-6) {
        if (offsetAxis < 0.0) {
            return vec2(1.0, 0.0);
        }
    } else if (rayAxis > 0.0) {
        start = max(start, -offsetAxis / rayAxis);
    } else {
        end = min(end, -offsetAxis / rayAxis);
    }
    return vec2(start, end);
}

// 按光锥交段等角采样，解析积分体积光的距离衰减。
vec3 lyra_scattering(ivec2 pixel, vec3 origin, vec3 ray, float reach) {
    ivec2 tile = clamp(pixel / 64, ivec2(0), LyraTileCount - 1);
    int base = tile.x * 33;
    int count = clamp(int(texelFetch(LyraLightTiles, ivec2(base, tile.y), 0).r), 0, 32);
    vec3 result = vec3(0);
    ivec2 stratumCell = lyra_lightCell(pixel);
    float stratum = (float(stratumCell.x * 4 + stratumCell.y) + 0.5) / 16.0;
    for (int entry = 1; entry <= count; entry++) {
        int index = int(texelFetch(LyraLightTiles, ivec2(base + entry, tile.y), 0).r);
        vec4 settings = texelFetch(LyraLightData, ivec2(3, index), 0);
        if (settings.w > 0.0) {
            vec4 position = texelFetch(LyraLightData, ivec2(0, index), 0);
            vec4 color = texelFetch(LyraLightData, ivec2(1, index), 0);
            vec4 cone = texelFetch(LyraLightData, ivec2(2, index), 0);
            float radius = texelFetch(LyraLightData, ivec2(4, index), 0).x;
            vec3 relative = position.xyz - origin;
            float center = dot(ray, relative);
            float radial = max(dot(relative, relative) - center * center, 0.0);
            float halfSpan = sqrt(max(position.w * position.w - radial, 0.0));
            float start = max(0.0, center - halfSpan);
            float end = min(reach, center + halfSpan);
            if (color.w > 0.5 && cone.w > 0.087) {
                vec2 interval = lyra_coneRange(origin, ray, position.xyz, cone.xyz, cone.w, radius);
                start = max(start, interval.x);
                end = min(end, interval.y);
            }
            if (end > start) {
                float height = sqrt(radial + 1.0);
                float shift = 1.0 / (position.w * position.w + 1.0);
                float first = start - center;
                float last = end - center;
                float firstAngle = atan(first / height);
                float lastAngle = atan(last / height);
                int steps = color.w > 0.5 || settings.y > 0.5 ? LyraVolumeSteps * 2 : LyraVolumeSteps;
                if (color.w > 0.5 && radius > 0.0) {
                    steps *= 2;
                }
                float total = 0.0;
                for (int side = 0; side < 2; side++) {
                    float a = side == 0 ? first : max(first, 0.0);
                    float b = side == 0 ? min(last, 0.0) : last;
                    if (b > a) {
                        float angleStep = ((side == 0 ? min(lastAngle, 0.0) : lastAngle)
                                - (side == 0 ? firstAngle : max(firstAngle, 0.0))) / float(steps);
                        float tanStep = tan(angleStep);
                        float tanOffset = tan((settings.y > 0.5 ? stratum : 0.5) * angleStep);
                        float tanA = a / height;
                        for (int step = 0; step < steps; step++) {
                            float tanB = step == steps - 1 ? b / height : (tanA + tanStep) / (1.0 - tanA * tanStep);
                            float integral = angleStep / height - shift * height * (tanB - tanA);
                            float middle = height * (tanA + tanOffset) / (1.0 - tanA * tanOffset);
                            tanA = tanB;
                            float distance = max(sqrt(middle * middle + radial), 0.0001);
                            float phase = 1.09 + 0.6 * middle / distance;
                            float energy = 0.91 / (phase * sqrt(phase));
                            if (color.w > 0.5) {
                                float cosine = radius > 0.0 ? lyra_spotCosine((center + middle) * ray - relative, cone.xyz, radius)
                                        : dot((center + middle) * ray - relative, cone.xyz) / distance;
                                energy *= smoothstep(cone.w, settings.x, cosine);
                            }
                            if (settings.y > 0.5) {
                                energy *= lyra_visibility(index, origin + ray * (center + middle), position.xyz);
                            }
                            total += max(integral, 0.0) * energy;
                        }
                    }
                }
                result += color.rgb * settings.w * 0.03 * total;
            }
        }
    }
    return min(result, vec3(3e4));
}
