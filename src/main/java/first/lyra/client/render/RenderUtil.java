package first.lyra.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import first.lyra.mixin.BufferBuilderAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.client.submit.RenderPhaseKey;
import net.neoforged.neoforge.client.submit.RenderPhaseKeys;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;

public final class RenderUtil {

    /** 全亮光照常量（packed，渲染调用直接使用）。 */
    public static final int FULL_LIGHT = LightCoordsUtil.pack(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_SKY);

    private RenderUtil() {
    }

    /**
     * 渲染独立模型（全参数版本，即时提交，直接遍历 quadCollection）。
     *
     * @param key         模型注册键
     * @param poseStack   姿态栈（模型视图空间）
     * @param collector   提交节点收集器
     * @param tintColor   整体染色 ARGB，-1 不染色
     * @param packedLight 打包光照（LightCoordsUtil.pack）
     */
    public static void renderStandalone(StandaloneModelKey<QuadCollection> key, PoseStack poseStack, SubmitNodeCollector collector, int tintColor, int packedLight) {
        QuadCollection quadCollection = Minecraft.getInstance().getModelManager().getStandaloneModel(key);
        if (quadCollection != null) {
            List<BakedQuad> all = quadCollection.getAll();
            RenderPhaseKey<TranslucentSubmit> phaseKey = RenderPhaseKeys.TRANSLUCENT_MODELS;
            RenderType renderType = LyraRenderTypes.ATTACHMENT_ENTITY_TRANSLUCENT;
            collector.submitSpecial(phaseKey, new LyraCustomSubmit(poseStack.last().copy(), renderType, (pose, consumer) -> writeQuads(pose, consumer, all, tintColor, packedLight)));
        }
    }

    public static void renderImage(Identifier texture, Vec3 center, float width, float height, SubmitNodeCollector collector, boolean alwaysVisible, int tintColor) {
        Camera camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
        if (camera != null) {
            RenderType renderType = LyraRenderTypes.texture(texture, alwaysVisible);
            Vec3 camPos = camera.position();
            Quaternionf rotation = new Quaternionf(camera.rotation()).mul(Axis.XN.rotationDegrees(180), new Quaternionf());
            Matrix4f matrix = new Matrix4f().rotate(rotation).setTranslation((float) (center.x() - camPos.x()), (float) (center.y() - camPos.y()), (float) (center.z() - camPos.z()));
            float halfWidth = width / 2f;
            float halfHeight = height / 2f;
            float[] xyzuvData = new float[4 * 5];
            int[] colorData = new int[4];
            Vector3f v = new Vector3f();
            int vertexIndex = 0;
            matrix.transformPosition(-halfWidth, -halfHeight, 0, v);
            int vertexIndex4 = vertexIndex++;
            float x3 = v.x();
            float y3 = v.y();
            float z3 = v.z();
            int dataIndex3 = 0;
            xyzuvData[dataIndex3] = x3;
            xyzuvData[dataIndex3 + 1] = y3;
            xyzuvData[dataIndex3 + 2] = z3;
            xyzuvData[dataIndex3 + 3] = 0f;
            xyzuvData[dataIndex3 + 4] = 0f;
            colorData[vertexIndex4] = tintColor;
            matrix.transformPosition(-halfWidth, halfHeight, 0, v);
            int vertexIndex3 = vertexIndex++;
            float x2 = v.x();
            float y2 = v.y();
            float z2 = v.z();
            int dataIndex2 = vertexIndex3 * 5;
            xyzuvData[dataIndex2] = x2;
            xyzuvData[dataIndex2 + 1] = y2;
            xyzuvData[dataIndex2 + 2] = z2;
            xyzuvData[dataIndex2 + 3] = 0f;
            xyzuvData[dataIndex2 + 4] = 1f;
            colorData[vertexIndex3] = tintColor;
            matrix.transformPosition(halfWidth, halfHeight, 0, v);
            int vertexIndex2 = vertexIndex++;
            float x1 = v.x();
            float y1 = v.y();
            float z1 = v.z();
            int dataIndex1 = vertexIndex2 * 5;
            xyzuvData[dataIndex1] = x1;
            xyzuvData[dataIndex1 + 1] = y1;
            xyzuvData[dataIndex1 + 2] = z1;
            xyzuvData[dataIndex1 + 3] = 1f;
            xyzuvData[dataIndex1 + 4] = 1f;
            colorData[vertexIndex2] = tintColor;
            matrix.transformPosition(halfWidth, -halfHeight, 0, v);
            int vertexIndex1 = vertexIndex++;
            float x = v.x();
            float y = v.y();
            float z = v.z();
            int dataIndex = vertexIndex1 * 5;
            xyzuvData[dataIndex] = x;
            xyzuvData[dataIndex + 1] = y;
            xyzuvData[dataIndex + 2] = z;
            xyzuvData[dataIndex + 3] = 1f;
            xyzuvData[dataIndex + 4] = 0f;
            colorData[vertexIndex1] = tintColor;
            collector.submitCustomGeometry(new PoseStack(), renderType, (pose, consumer) -> writeVertices(consumer, xyzuvData, colorData, FULL_LIGHT, 4));
        }
    }

    /**
     * 通用顶点批量写入（MemorySegment 直写，伤害信息/轨迹等共用）。
     * <p>
     * 数据约定：每顶点 5 float（已变换 x,y,z + u,v）+ 每顶点 1 int（ARGB 颜色）。
     * 按目标格式自适应：BLOCK（28B，无 overlay/normal）与 ENTITY（36B，overlay=0、
     * normal=0,0,1 填充）。consumer 为 BufferBuilder 时一次 reserve + 逐字段内联写入。
     * </p>
     */
    public static void writeVertices(VertexConsumer consumer, float[] xyzuvData, int[] colorData, int packedLight, int vertexCount) {
        if (consumer instanceof BufferBuilder builder) {
            BufferBuilderAccessor accessor = (BufferBuilderAccessor) builder;
            int vertexSize = accessor.getFormat().getVertexSize();
            long totalBytes = (long) vertexCount * vertexSize;
            long pointer = accessor.getBuffer().reserve((int) totalBytes);
            MemorySegment segment = MemorySegment.ofAddress(pointer).reinterpret(totalBytes);
            boolean entityFormat = vertexSize == 36;
            long offset = 0;
            for (int i = 0; i < vertexCount; i++) {
                int sourceIndex = i * 5;
                segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, xyzuvData[sourceIndex]);
                offset += 4;
                segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, xyzuvData[sourceIndex + 1]);
                offset += 4;
                segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, xyzuvData[sourceIndex + 2]);
                offset += 4;
                segment.set(ValueLayout.JAVA_INT_UNALIGNED, offset, ARGB.toABGR(colorData[i]));
                offset += 4;
                segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, xyzuvData[sourceIndex + 3]);
                offset += 4;
                segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, xyzuvData[sourceIndex + 4]);
                offset += 4;
                if (entityFormat) {
                    // overlay 必须用 NO_OVERLAY（pack(0,10)，白色区）——写 0 会采样到 overlay
                    // 纹理 v<8 的红色区域，导致整片渲染偏红
                    segment.set(ValueLayout.JAVA_INT_UNALIGNED, offset, OverlayTexture.NO_OVERLAY); // overlay
                    offset += 4;
                    segment.set(ValueLayout.JAVA_INT_UNALIGNED, offset, packedLight); // light
                    offset += 4;
                    segment.set(ValueLayout.JAVA_BYTE, offset, (byte) 0); // nx
                    offset += 1;
                    segment.set(ValueLayout.JAVA_BYTE, offset, (byte) 0); // ny
                    offset += 1;
                    segment.set(ValueLayout.JAVA_BYTE, offset, (byte) 127); // nz
                    offset += 1;
                    offset += 1; // padding
                } else {
                    segment.set(ValueLayout.JAVA_INT_UNALIGNED, offset, packedLight); // light
                    offset += 4;
                }
            }
            accessor.setVertices(accessor.getVertices() + vertexCount);
            accessor.setElementsToFill(0);
        } else {
            // 回退：逐顶点写入（非 BufferBuilder consumer）
            for (int i = 0; i < vertexCount; i++) {
                int sourceIndex = i * 5;
                consumer.addVertex(xyzuvData[sourceIndex], xyzuvData[sourceIndex + 1], xyzuvData[sourceIndex + 2], colorData[i], xyzuvData[sourceIndex + 3], xyzuvData[sourceIndex + 4], OverlayTexture.NO_OVERLAY, packedLight, 0, 0, 1);
            }
        }
    }

    /**
     * 顶点写入：consumer 为 BufferBuilder 时走 MemorySegment 批量直写（ENTITY 格式，
     * 含法线）；其他实现回退逐顶点 addVertex。
     */
    private static void writeQuads(PoseStack.Pose pose, VertexConsumer consumer, List<BakedQuad> quads, int tintColor, int packedLight) {
        if (consumer instanceof BufferBuilder builder) {
            writeQuadsBatch(pose, builder, quads, tintColor, packedLight);
        } else {
            writeQuadsSingle(pose, consumer, quads, tintColor, packedLight);
        }
    }

    /**
     * 批量直写（ENTITY 格式 36 字节/顶点：Position 12 + Color 4 + UV0 8 + UV1 4 + UV2 4 + Normal 3 + 对齐 1）。
     * 法线取 quad 朝向（局部法线）经姿态 normal 矩阵变换，写入量化的 byte 三元组。
     * 一次 reserve 全部顶点，MemorySegment 直写；经 BufferBuilderAccessor 同步内部计数。
     */
    private static void writeQuadsBatch(PoseStack.Pose pose, BufferBuilder builder, List<BakedQuad> quads, int tintColor, int packedLight) {
        Matrix4f poseMatrix = pose.pose();
        Vector3f position = new Vector3f();
        Vector3f normal = new Vector3f();
        int vertexCount = quads.size() * 4;
        boolean tinted = tintColor != -1;
        BufferBuilderAccessor accessor = (BufferBuilderAccessor) builder;
        long totalBytes = (long) vertexCount * accessor.getFormat().getVertexSize();
        long pointer = accessor.getBuffer().reserve((int) totalBytes);
        MemorySegment segment = MemorySegment.ofAddress(pointer).reinterpret(totalBytes);
        long offset = 0;
        for (BakedQuad quad : quads) {
            pose.transformNormal(quad.direction().getUnitVec3f(), normal);
            for (int vertex = 0; vertex < 4; vertex++) {
                poseMatrix.transformPosition(quad.position(vertex), position);
                int color = quad.bakedColors().color(vertex);
                if (tinted) {
                    color = ARGB.multiply(tintColor, color);
                }
                long packedUv = quad.packedUV(vertex);
                segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, position.x());
                offset += 4;
                segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, position.y());
                offset += 4;
                segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, position.z());
                offset += 4;
                segment.set(ValueLayout.JAVA_INT_UNALIGNED, offset, ARGB.toABGR(color));
                offset += 4;
                segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, UVPair.unpackU(packedUv));
                offset += 4;
                segment.set(ValueLayout.JAVA_FLOAT_UNALIGNED, offset, UVPair.unpackV(packedUv));
                offset += 4;
                segment.set(ValueLayout.JAVA_INT_UNALIGNED, offset, OverlayTexture.NO_OVERLAY);
                offset += 4;
                segment.set(ValueLayout.JAVA_INT_UNALIGNED, offset, packedLight);
                offset += 4;
                segment.set(ValueLayout.JAVA_BYTE, offset, normalByte(normal.x));
                offset += 1;
                segment.set(ValueLayout.JAVA_BYTE, offset, normalByte(normal.y));
                offset += 1;
                segment.set(ValueLayout.JAVA_BYTE, offset, normalByte(normal.z));
                offset += 1;
                offset += 1; // 对齐
            }
        }
        accessor.setVertices(accessor.getVertices() + vertexCount);
        accessor.setElementsToFill(0);
    }

    /** 逐顶点回退路径（非 BufferBuilder consumer），法线取 quad 朝向变换。 */
    private static void writeQuadsSingle(PoseStack.Pose pose, VertexConsumer consumer, List<BakedQuad> quads, int tintColor, int packedLight) {
        Matrix4f poseMatrix = pose.pose();
        Vector3f position = new Vector3f();
        Vector3f normal = new Vector3f();
        boolean tinted = tintColor != -1;
        for (BakedQuad quad : quads) {
            pose.transformNormal(quad.direction().getUnitVec3f(), normal);
            for (int vertex = 0; vertex < 4; vertex++) {
                poseMatrix.transformPosition(quad.position(vertex), position);
                int color = quad.bakedColors().color(vertex);
                if (tinted) {
                    color = ARGB.multiply(tintColor, color);
                }
                long packedUv = quad.packedUV(vertex);
                consumer.addVertex(position.x(), position.y(), position.z(), color,
                        UVPair.unpackU(packedUv), UVPair.unpackV(packedUv),
                        OverlayTexture.NO_OVERLAY, packedLight,
                        normal.x(), normal.y(), normal.z());
            }
        }
    }

    /** 法线量化（float [-1,1] → byte，与原版 putNormals 同款）。 */
    private static byte normalByte(float value) {
        return (byte) ((int) (Mth.clamp(value, -1.0F, 1.0F) * 127.0F) & 0xFF);
    }
}
