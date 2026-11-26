uniform usampler2D LyraVoxelPages;
uniform usampler2D LyraVoxelBlocks;
uniform vec3 LyraVoxelOffset;
uniform mat4 LyraViewInverse;

vec3 lyra_voxelPosition(vec3 view) {
    return (LyraViewInverse * vec4(view, 1.0)).xyz + LyraVoxelOffset;
}

float lyra_voxelTrace(vec3 origin, vec3 direction, float reach) {
    vec3 start = origin * 2.0;
    vec3 safeDirection = vec3(abs(direction.x) < 1e-6 ? 1e-6 : direction.x,
            abs(direction.y) < 1e-6 ? 1e-6 : direction.y, abs(direction.z) < 1e-6 ? 1e-6 : direction.z);
    vec3 inverse = 1.0 / safeDirection;
    ivec3 stepDirection = ivec3(sign(safeDirection));
    vec3 forward = step(vec3(0), safeDirection);
    ivec3 cell = ivec3(floor(start));
    vec3 boundary = (vec3(cell) + forward - start) * inverse;
    float distance = 0.0;
    for (int step = 0; step < 160; step++) {
        if (step > 0) {
            if (any(lessThan(cell, ivec3(0))) || any(greaterThanEqual(cell, ivec3(1024, 512, 1024)))) {
                return reach;
            }
            ivec3 section = cell >> 5;
            int index = section.x + 32 * (section.z + 32 * section.y);
            uint page = texelFetch(LyraVoxelPages, ivec2(index & 127, index >> 7), 0).r;
            if (page == 2u) {
                return distance * 0.5;
            }
            float jump = 32.0;
            if (page >= 3u) {
                int slot = int(page - 3u);
                ivec3 block = (cell >> 1) & 15;
                ivec2 texel = ivec2((slot & 63) * 64 + block.x + 16 * (block.y & 3),
                        (slot >> 6) * 64 + block.z + 16 * (block.y >> 2));
                uint mask = texelFetch(LyraVoxelBlocks, texel, 0).r;
                ivec3 sub = cell & 1;
                if ((mask & (1u << uint(sub.x + 2 * sub.y + 4 * sub.z))) != 0u) {
                    return distance * 0.5;
                }
                jump = mask == 0u ? 2.0 : 0.0;
            }
            if (jump > 0.0) {
                vec3 box = floor(vec3(cell) / jump) * jump;
                vec3 exitDistance = (box + forward * jump - start) * inverse;
                float exit = min(exitDistance.x, min(exitDistance.y, exitDistance.z));
                distance = exit + 0.001;
                if (distance >= reach * 2.0) {
                    return reach;
                }
                cell = ivec3(floor(start + direction * distance));
                // 边界上的微小方向分量可能被浮点舍入，显式跨过离开的面。
                if (exitDistance.x <= exit) {
                    cell.x = int(box.x) + (stepDirection.x > 0 ? int(jump) : -1);
                }
                if (exitDistance.y <= exit) {
                    cell.y = int(box.y) + (stepDirection.y > 0 ? int(jump) : -1);
                }
                if (exitDistance.z <= exit) {
                    cell.z = int(box.z) + (stepDirection.z > 0 ? int(jump) : -1);
                }
                boundary = (vec3(cell) + forward - start) * inverse;
                continue;
            }
        }
        if (boundary.x < boundary.y && boundary.x < boundary.z) {
            distance = boundary.x;
            boundary.x += abs(inverse.x);
            cell.x += stepDirection.x;
        } else if (boundary.y < boundary.z) {
            distance = boundary.y;
            boundary.y += abs(inverse.y);
            cell.y += stepDirection.y;
        } else {
            distance = boundary.z;
            boundary.z += abs(inverse.z);
            cell.z += stepDirection.z;
        }
        if (distance >= reach * 2.0) {
            return reach;
        }
    }
    return reach;
}
