package first.lyra.client.light;

import java.nio.FloatBuffer;

public record FlowingLaserProfile(int innerColor, int edgeColor, float innerRadius, float edgeRadius,
                                 float length, float startRadius, float processRadius, float endRadius,
                                 float expansion, float contraction, float flowSpeed, float time) {

    void write(FloatBuffer buffer, int offset) {
        buffer.put(offset, startRadius).put(offset + 1, processRadius).put(offset + 2, endRadius).put(offset + 3, length);
        buffer.put(offset + 4, expansion).put(offset + 5, contraction).put(offset + 6, innerRadius).put(offset + 7, edgeRadius);
        buffer.put(offset + 8, (innerColor >> 16 & 255) / 255f).put(offset + 9, (innerColor >> 8 & 255) / 255f)
                .put(offset + 10, (innerColor & 255) / 255f).put(offset + 11, flowSpeed);
        buffer.put(offset + 12, (edgeColor >> 16 & 255) / 255f).put(offset + 13, (edgeColor >> 8 & 255) / 255f)
                .put(offset + 14, (edgeColor & 255) / 255f).put(offset + 15, time);
    }
}
