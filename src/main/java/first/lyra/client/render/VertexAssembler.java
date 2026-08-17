package first.lyra.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * 顶点批量组装器（自定义几何共用）。
 * <p>
 * 逐顶点 {@link #addVertex} 组装到连续缓冲（每顶点 5 float x,y,z,u,v + 1 int ARGB 颜色），
 * 完成后 {@link #write} 经 {@link RenderUtil#writeVertices} MemorySegment 批量直写
 * （BLOCK 格式，一次 reserve + 内联写入）。BLOCK 格式无法线/overlay，组装不含法线。
 * </p>
 */
public final class VertexAssembler {

    private float[] xyzuvData = new float[512 * 5];
    private int[] colorData = new int[512];
    private int vertexCount;

    /** 追加一个顶点（坐标须为模型视图空间已变换值）。 */
    public void addVertex(float x, float y, float z, int color, float u, float v) {
        ensureCapacity(this.vertexCount + 1);
        int dataIndex = this.vertexCount * 5;
        this.xyzuvData[dataIndex] = x;
        this.xyzuvData[dataIndex + 1] = y;
        this.xyzuvData[dataIndex + 2] = z;
        this.xyzuvData[dataIndex + 3] = u;
        this.xyzuvData[dataIndex + 4] = v;
        this.colorData[this.vertexCount] = color;
        this.vertexCount++;
    }

    /** 批量直写当前收集的顶点并清空。 */
    public void write(VertexConsumer consumer, int packedLight) {
        if (this.vertexCount == 0) {
            return;
        }
        RenderUtil.writeVertices(consumer, this.xyzuvData, this.colorData, packedLight, this.vertexCount);
        this.clear();
    }

    /** 清空收集（帧首/回调开始调用）。 */
    public void clear() {
        this.vertexCount = 0;
    }

    private void ensureCapacity(int requiredVertexCount) {
        if (requiredVertexCount * 5 > this.xyzuvData.length) {
            int newVertexCapacity = Math.max(requiredVertexCount, this.xyzuvData.length / 5 * 2);
            float[] newXyzuvData = new float[newVertexCapacity * 5];
            System.arraycopy(this.xyzuvData, 0, newXyzuvData, 0, this.vertexCount * 5);
            this.xyzuvData = newXyzuvData;
            int[] newColorData = new int[newVertexCapacity];
            System.arraycopy(this.colorData, 0, newColorData, 0, this.vertexCount);
            this.colorData = newColorData;
        }
    }
}
