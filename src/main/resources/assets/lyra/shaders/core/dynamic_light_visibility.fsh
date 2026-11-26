#version 150
#moj_import <lyra:dynamic_light_voxel.glsl>
uniform sampler2D LyraLightData;
uniform int LyraLightCount;
out vec4 fragColor;

void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    ivec2 tile = pixel / 128;
    int index = tile.x + tile.y * 8;
    float reach = 0.0;
    if (index < LyraLightCount && texelFetch(LyraLightData, ivec2(3, index), 0).y > 0.5) {
        vec4 light = texelFetch(LyraLightData, ivec2(0, index), 0);
        vec2 uv = (vec2(pixel - tile * 128) + 0.5) / 64.0 - 1.0;
        vec3 direction = vec3(uv, 1.0 - abs(uv.x) - abs(uv.y));
        float fold = max(-direction.z, 0.0);
        direction.xy += vec2(direction.x >= 0.0 ? -fold : fold, direction.y >= 0.0 ? -fold : fold);
        reach = lyra_voxelTrace(lyra_voxelPosition(light.xyz), normalize(direction), light.w);
    }
    fragColor = vec4(reach, 0, 0, 1);
}
