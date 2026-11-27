#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

out float vertexDistance;
out vec2 texCoord;
out vec2 corner;
flat out vec4 centerColor;
flat out vec4 edgeColor;
flat out float spriteWidth;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = fog_distance(Position, FogShape);
    texCoord = UV0;
    corner = Normal.xy;
    vec4 light = texelFetch(Sampler2, ivec2(15), 0);
    centerColor = Color * light;
    edgeColor = vec4(vec3(UV1.y & 255, (UV1.x >> 8) & 255, UV1.x & 255) / 255.0, 1.0) * light;
    spriteWidth = float(UV2.x & 65535);
}
