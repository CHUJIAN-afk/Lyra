package first.lyra.client.light;

import java.util.List;

public final class FlowingLaserShaders {

    private FlowingLaserShaders() {
    }

    public static String importLibrary(String source) {
        if (source.contains("photon:") && !source.contains("lyra:flowing_laser.glsl") && !source.contains("LYRA_FLOW_LIBRARY")) {
            return source + "\n#moj_import <lyra:flowing_laser.glsl>\n";
        }
        return source;
    }

    public static List<String> specialize(List<String> parts) {
        String source = String.join("", parts);
        if (source.contains("LYRA_FLOW_LIBRARY") && source.contains("float photon_spot(") && !source.contains("#define LYRA_FLOW_LIGHTING")) {
            String defines = "\n";
            source = source.replace("float photon_attenuation(float distanceSq, float range) {", """
                    #define LYRA_FLOW_LIGHTING
                    float lyra_flowDistanceSq;
                    float lyra_flowSpot(vec3 direction, inout vec4 color, vec4 axis, vec4 shadow);
                    float photon_attenuation(float distanceSq, float range) {
                        lyra_flowDistanceSq = distanceSq;
                    """);
            source = source.replace("float photon_spot(vec3 toLightDir, vec4 t1, vec4 t2, vec4 t3) {", """
                    float photon_spot(vec3 toLightDir, inout vec4 t1, vec4 t2, vec4 t3) {
                        if (t1.w > 1.5) return lyra_flowSpot(toLightDir, t1, t2, t3);
                    """);
            if (source.contains("uniform sampler2D VolumeLightData;")) {
                defines += "#define LYRA_FLOW_VOLUME\n";
                source = source.replace("void main() {", """
                        vec3 lyra_flowVolume(vec3 o, vec3 d, vec4 v0, vec4 v1, vec4 v2, float first, float last, int index, float stratum);
                        void main() {
                        """);
                source = source.replace("float H = sqrt(h2 + 1.0);", """
                        if (v1.w > 1.5) {
                            sum += lyra_flowVolume(o, d, v0, v1, v2, t0, t1, index, stratum);
                            continue;
                        }
                        float H = sqrt(h2 + 1.0);
                        """);
            }
            source = source.replaceFirst("(?m)(#version[^\\r\\n]*)", "$1" + defines);
            return List.of(source);
        }
        return parts;
    }
}
