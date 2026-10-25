package first.lyra.common.entity;

import first.lyra.common.attachmentEntity.PathNode;
import first.lyra.common.attachmentEntity.PlannedPath;
import first.lyra.common.attachmentEntity.RenderKeyframe;
import first.lyra.common.attachmentEntity.SyncFieldDispatcher;
import first.lyra.utils.LyraStreamCodecs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public interface IPathEntity extends ISyncEntity {

    default EntityPathData getPathData() {
        EntitySyncData data = getSyncData();
        if (data.path == null) {
            data.path = new EntityPathData(getSyncEntity());
        }
        return data.path;
    }

    @Override
    default void registerSyncFields(SyncFieldDispatcher fields) {
        fields.field(LyraStreamCodecs.INT, () -> getPathData().tick, value -> getPathData().tick = value);
        fields.field(LyraStreamCodecs.PATH_NODE, this::getCurrentPathNode, this::setCurrentPathNode);
        fields.field(LyraStreamCodecs.RENDER_KEYFRAMES, this::getRenderKeyframes, this::setRenderKeyframes);
    }

    @Override
    default void loadSyncFields(CompoundTag tag) {
        ISyncEntity.super.loadSyncFields(tag);
        EntityPathData data = getPathData();
        data.path = null;
        data.history.clear();
        data.history.add(data.node);
        data.lastRenderTick = data.tick;
        data.renderKeyframesActive = false;
        Entity entity = getSyncEntity();
        entity.setOldPosAndRot();
        if (entity instanceof LivingEntity living) {
            living.yBodyRotO = living.yBodyRot;
            living.yHeadRotO = living.yHeadRot;
        }
    }

    /** 在实体自身 tick 的末尾调用，包含字段同步。 */
    default void tickPath() {
        Entity entity = getSyncEntity();
        EntityPathData data = getPathData();
        data.history.addFirst(data.node);
        if (data.history.size() > 16) {
            data.history.removeLast();
        }
        if (entity.level().isClientSide()) {
            tickSyncFields();
            data.renderKeyframesActive = data.tick != data.lastRenderTick;
            data.lastRenderTick = data.tick;
            applyPathNode(data.node);
            PathNode previous = data.history.getFirst();
            entity.xo = previous.pos().x;
            entity.yo = previous.pos().y;
            entity.zo = previous.pos().z;
            entity.xOld = previous.pos().x;
            entity.yOld = previous.pos().y;
            entity.zOld = previous.pos().z;
            entity.yRotO = previous.yaw();
            entity.xRotO = previous.pitch();
            if (entity instanceof LivingEntity living) {
                living.yBodyRotO = previous.yaw();
                living.yHeadRotO = previous.yaw();
            }
        } else {
            data.tick++;
            data.frames = List.of();
            data.renderKeyframesActive = false;
            if (isExecutingPath()) {
                List<RenderKeyframe> frames = data.path.getRenderKeyframes(data.path.getCurrentIndex());
                PathNode from = data.node;
                setCurrentPathNode(data.path.advance());
                if (!frames.isEmpty()) {
                    List<RenderKeyframe> segment = new ArrayList<>(frames.size() + 2);
                    segment.add(new RenderKeyframe(0f, from));
                    segment.addAll(frames);
                    segment.add(new RenderKeyframe(1f, data.node));
                    data.frames = List.copyOf(segment);
                    data.renderKeyframesActive = true;
                }
            } else {
                data.node = new PathNode(entity.position(), entity.getYRot(), entity.getXRot(), getRoll());
            }
            tickSyncFields();
        }
    }

    default void applyPathNode(PathNode node) {
        Entity entity = getSyncEntity();
        entity.setPos(node.pos());
        entity.setYRot(node.yaw());
        entity.setXRot(node.pitch());
        if (entity instanceof LivingEntity living) {
            living.yBodyRot = node.yaw();
            living.yHeadRot = node.yaw();
        }
    }

    default PathNode getCurrentPathNode() {
        return getPathData().node;
    }

    default void setCurrentPathNode(PathNode node) {
        getPathData().node = node;
        applyPathNode(node);
    }

    default PlannedPath getCurrentPath() {
        return getPathData().path;
    }

    default void setPlannedPath(PlannedPath path) {
        EntityPathData data = getPathData();
        data.path = path;
        data.frames = List.of();
        data.renderKeyframesActive = false;
    }

    default boolean isExecutingPath() {
        PlannedPath path = getCurrentPath();
        return path != null && !path.isFinished();
    }

    default void setPath(List<PathNode> nodes) {
        setPath(new PlannedPath(nodes));
    }

    default void setPath(PlannedPath path) {
        setPlannedPath(path);
        if (!path.getNodes().isEmpty()) {
            setCurrentPathNode(path.getNodes().getFirst());
        }
    }

    default ArrayList<PathNode> getHistoryNodes() {
        return getPathData().history;
    }

    default PathNode getRenderNode(float partialTick) {
        EntityPathData data = getPathData();
        if (data.renderKeyframesActive && !data.frames.isEmpty()) {
            return RenderKeyframe.interpolate(data.frames, partialTick);
        }
        if (!data.history.isEmpty()) {
            return data.history.getFirst().lerp(data.node, partialTick);
        }
        return data.node;
    }

    default List<RenderKeyframe> getRenderKeyframes() {
        return getPathData().frames;
    }

    default void setRenderKeyframes(List<RenderKeyframe> frames) {
        getPathData().frames = frames;
    }

    default float getRoll() {
        return getCurrentPathNode().roll();
    }

    default void setRoll(float roll) {
        EntityPathData data = getPathData();
        data.node = data.node.modifyEuler(data.node.yaw(), data.node.pitch(), roll);
    }
}
