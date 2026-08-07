package first.lyra.common.entity;

import java.util.List;

public class PlannedPath {
    private final String identifier;
    private List<PathNode> nodes;
    private int currentIndex;

    public PlannedPath(String identifier, List<PathNode> nodes) {
        this.identifier = identifier;
        this.nodes = nodes;
        this.currentIndex = 0;
    }

    public String getIdentifier() {
        return identifier;
    }

    public List<PathNode> getNodes() {
        return nodes;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * 动态修正轨迹：允许 Goal 在运行时利用支点重新计算后续节点，并更新当前路径
     */
    public void updateNodes(List<PathNode> newNodes) {
        this.nodes = newNodes;
    }

    public void setIndex(int index) {
        this.currentIndex = index;
    }

    /**
     * 获取下一个节点并推进进度（不移除列表内数据）
     */
    public PathNode advance() {
        if (isFinished()) return null;
        return nodes.get(currentIndex++);
    }

    public boolean isFinished() {
        return currentIndex >= nodes.size();
    }

}