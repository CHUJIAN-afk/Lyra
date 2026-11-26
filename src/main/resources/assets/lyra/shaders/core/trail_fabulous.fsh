#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D SceneDepth;
uniform sampler2D CloudColor;
uniform sampler2D CloudDepth;
uniform int HasClouds;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
out vec4 fragColor;

void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    if (texelFetch(SceneDepth, pixel, 0).r < gl_FragCoord.z) {
        discard;
    }
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }
    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color = linear_fog(color * lightMapColor, vertexDistance, FogStart, FogEnd, FogColor);

    // The scene already contains the cloud, so retain its contribution over effects behind it.
    if (HasClouds != 0 && texelFetch(CloudDepth, pixel, 0).r < gl_FragCoord.z) {
        vec4 cloud = texelFetch(CloudColor, pixel, 0);
        color.rgb = cloud.rgb + color.rgb * (1.0 - cloud.a);
    }
    fragColor = color;
}
