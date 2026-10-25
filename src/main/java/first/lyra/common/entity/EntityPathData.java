package first.lyra.common.entity;

import first.lyra.common.attachmentEntity.PathNode;
import first.lyra.common.attachmentEntity.PlannedPath;
import first.lyra.common.attachmentEntity.RenderKeyframe;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class EntityPathData {

    final ArrayList<PathNode> history = new ArrayList<>();
    PathNode node;
    PlannedPath path;
    List<RenderKeyframe> frames = List.of();
    int tick;
    int lastRenderTick = -1;
    boolean renderKeyframesActive;

    public EntityPathData(Entity entity) {
        node = new PathNode(entity.position(), entity.getYRot(), entity.getXRot(), 0);
    }
}
