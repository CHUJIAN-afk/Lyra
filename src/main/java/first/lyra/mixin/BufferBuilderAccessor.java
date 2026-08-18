package first.lyra.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * BufferBuilder 内部状态访问器（批量顶点写入快路径，26.2 向下移植）。
 * <p>
 * RenderUtil 批量写入时经此接口直接操作 reserve/计数/元素标记
 * （一次 reserve + 直写，跳过逐顶点 addVertex 的检查开销）。
 * </p>
 */
@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessor {
    @Accessor
    ByteBufferBuilder getBuffer();

    @Accessor
    VertexFormat getFormat();

    @Accessor
    int getVertices();

    @Mutable
    @Accessor
    void setVertices(int vertices);

    @Accessor
    int getElementsToFill();

    @Mutable
    @Accessor
    void setElementsToFill(int elementsToFill);
}
