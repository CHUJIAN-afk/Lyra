package first.lyra.common.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 附件实体抽象基类，为仆从和射弹提供统一的存储架构和渲染支持。
 */
public abstract class AttachmentEntity {

    // ===================== 基础标识 =====================

    protected UUID uuid;
    protected Player owner;
    protected float damage = 0;
    protected float knockback = 0;
    protected float armorPierce = 0;
    protected int tickCount = 0;

    // ===================== 路径与轨迹 =====================

    protected PlannedPath currentPlannedPath = null;
    protected final ArrayList<PathNode> historyNodes = new ArrayList<>();
    protected PathNode currentPathNode;
    protected PathNode clientTargetNode;
    protected boolean clientInitialized = false;
    protected boolean remove = false;

    public AttachmentEntity() {
        this.uuid = UUID.randomUUID();
        this.currentPathNode = new PathNode(Vec3.ZERO, 0, 0, 0);
        historyNodes.addFirst(currentPathNode);
        historyNodes.addFirst(currentPathNode);
    }

    // ===================== 抽象方法 =====================

    public float getDamage() {
        return damage;
    }

    public void setArmorPierce(float armorPierce) {
        this.armorPierce = armorPierce;
    }

    public float getArmorPierce() {
        return armorPierce;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getKnockback() {
        return knockback;
    }

    public void setKnockback(float knockback) {
        this.knockback = knockback;
    }

    public abstract AttachmentEntityType<? extends AttachmentEntity> getType();

    public void writeAdditional(RegistryFriendlyByteBuf buf) {
    }

    public void readAdditional(RegistryFriendlyByteBuf buf) {
    }

    public void onRemove() {
    }

    // ===================== 生命周期 =====================

    /**
     * 每tick更新方法，由附件数据管理器调用。
     */
    public void tick() {
        boolean clientSide = owner.level().isClientSide();
        if (!clientSide) {
            if (!isRemove()) {
                // 方块碰撞检测
                if (this instanceof IBlockCollision<?>) {
                    @SuppressWarnings("unchecked") IBlockCollision<AttachmentEntity> blockCollision = (IBlockCollision<AttachmentEntity>) this;
                    if (blockCollision.canCollideWithBlocks()) {
                        blockCollision.processBlockCollision(this);
                    }
                }
            }
            if (!isRemove()) {
                // 碰撞攻击检测
                if (this instanceof IEntityCollision<?>) {
                    @SuppressWarnings("unchecked") IEntityCollision<AttachmentEntity> collideAttack = (IEntityCollision<AttachmentEntity>) this;
                    if (collideAttack.canCollideAttack()) {
                        collideAttack.processCollision(this);
                    }
                }
            }
        } else {
            // 客户端：使用同步数据更新位置
            currentPathNode = clientTargetNode;
        }
        tickCount++;
        // 更新历史轨迹
        this.historyNodes.addFirst(this.currentPathNode);
        if (this.historyNodes.size() > getHistoryNodesSize()) {
            this.historyNodes.removeLast();
        }
        // 路径推进
        if (!clientSide && currentPlannedPath != null && !currentPlannedPath.isFinished()) {
            currentPathNode = currentPlannedPath.advance();
        }
    }

    /**
     * 设置当前要执行的计划路径。
     *
     * @param path 计划路径
     */
    public void setPlannedPath(PlannedPath path) {
        this.currentPlannedPath = path;
    }

    /**
     * 获取当前正在执行的路径。
     *
     * @return 当前路径，可能为 null
     */
    public PlannedPath getCurrentPath() {
        return this.currentPlannedPath;
    }

    /**
     * 获取当前所在的路径节点。
     *
     * @return 当前节点
     */
    public PathNode getCurrentPathNode() {
        return currentPathNode;
    }

    /**
     * 检查是否正在执行路径且未完成。
     *
     * @return 是否正在执行路径
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isExecutingPath() {
        return this.currentPlannedPath != null && !this.currentPlannedPath.isFinished();
    }

    /**
     * 设置一组路径节点，创建默认计划路径。
     *
     * @param nodes 路径节点列表
     */
    public void setPath(List<PathNode> nodes) {
        this.currentPlannedPath = new PlannedPath("default", nodes);
        if (!nodes.isEmpty()) {
            this.currentPathNode = nodes.getFirst();
        }
    }

    public void setPath(PlannedPath plannedPath) {
        List<PathNode> nodes = plannedPath.getNodes();
        if (!nodes.isEmpty()) {
            this.currentPathNode = nodes.getFirst();
        }
    }

    public boolean isRemove() {
        return remove;
    }

    public void setRemove() {
        this.remove = true;
    }

    /**
     * 初始化实体的路径节点和历史队列。
     * <p>
     * 通常在实体创建或重新定位时调用。
     * </p>
     *
     * @param node 初始节点
     */
    public void init(PathNode node) {
        this.currentPathNode = node;
        this.historyNodes.clear();
        this.historyNodes.addFirst(node);
        this.historyNodes.addFirst(node);
    }

    public void dimensionChange() {

    }

    // ===================== 历史轨迹 =====================

    /**
     * 返回历史节点队列的最大容量。
     * <p>
     * 子类可重写以改变拖尾渲染的历史长度。
     * 默认 16，射弹通常使用较小的值（如 8）。
     * </p>
     *
     * @return 队列最大长度
     */
    public int getHistoryNodesSize() {
        return 16;
    }

    /**
     * 获取历史节点队列。
     *
     * @return 历史节点队列
     */
    public ArrayList<PathNode> getHistoryNodes() {
        return historyNodes;
    }

    // ===================== 渲染支持 =====================

    /**
     * 获取用于渲染的插值节点。
     * <p>
     * 使用历史队列中最近两个节点进行线性插值，
     * 使视觉运动更平滑，消除帧间跳跃。
     * </p>
     *
     * @param partialTick 部分 tick 进度（0~1）
     * @return 插值后的渲染节点
     */
    public PathNode getRenderNode(float partialTick) {
        if (historyNodes.size() < 2) {
            return currentPathNode;
        }
        return historyNodes.get(1).lerp(currentPathNode, partialTick);
    }

    public void setCurrentPathNode(PathNode currentPathNode) {
        this.currentPathNode = currentPathNode;
    }

    /**
     * 写入实体的基础同步数据（位置与朝向），并调用子类的附加数据写入。
     * <p>
     * 基础数据包含：位置（x, y, z）、旋转（yaw, pitch, roll）。
     * </p>
     *
     * @param buf 数据包缓冲区
     */
    public void writeBase(RegistryFriendlyByteBuf buf) {
        buf.writeVec3(currentPathNode.pos());
        buf.writeFloat(currentPathNode.yaw());
        buf.writeFloat(currentPathNode.pitch());
        buf.writeFloat(currentPathNode.roll());
    }

    /**
     * 读取实体的基础同步数据，并调用子类的附加数据读取。
     * <p>
     * 首次同步时，会初始化当前位置和历史队列。
     * </p>
     *
     * @param buf 数据包缓冲区
     */
    public void readBase(RegistryFriendlyByteBuf buf) {
        this.clientTargetNode = new PathNode(buf.readVec3(), buf.readFloat(), buf.readFloat(), buf.readFloat());
        // 首次同步时初始化位置
        if (!clientInitialized) {
            clientInitialized = true;
            init(clientTargetNode);
        }
    }

    // ===================== 便捷访问器 =====================

    /** @return 当前位置 */
    public Vec3 getPos() {
        return currentPathNode.pos();
    }

    /** @return 当前偏航角（度） */
    public float getYaw() {
        return currentPathNode.yaw();
    }

    /** @return 当前俯仰角（度） */
    public float getPitch() {
        return currentPathNode.pitch();
    }

    /** @return 当前翻滚角（度） */
    public float getRoll() {
        return currentPathNode.roll();
    }

    /** @return 实体 UUID */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * 设置 UUID，用于从网络数据恢复。
     *
     * @param uuid UUID值
     */
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    /** @return 所有者玩家 */
    public Player getOwner() {
        return owner;
    }

    public int getTickCount() {
        return tickCount;
    }

    /**
     * 设置所有者玩家，由附件数据管理器调用。
     *
     * @param owner 玩家实例
     */
    public void setOwner(Player owner) {
        this.owner = owner;
    }

    /**
     * 计算贝塞尔曲线上的点（De Casteljau算法，支持任意数量控制点）
     */
    public Vec3 calculateBezierPoint(float delta, Vec3... P) {
        if (P.length == 0) {
            return Vec3.ZERO;
        }
        if (P.length == 1) {
            return P[0];
        }
        Vec3[] pts = P.clone();
        for (int k = P.length - 1; k > 0; k--) {
            for (int i = 0; i < k; i++) {
                pts[i] = pts[i].lerp(pts[i + 1], delta);
            }
        }
        return pts[0];
    }

    public Vec3 getLookAngle() {
        return Vec3.directionFromRotation(getPitch(), getYaw()).normalize();
    }

    /**
     * 获取当前速度向量
     */
    public Vec3 getCurrentVelocity() {
        Vec3 currentPos = getPos();
        ArrayList<PathNode> history = getHistoryNodes();
        if (history.size() > 1) {
            Vec3 rawVel = currentPos.subtract(history.getFirst().pos());
            if (rawVel.lengthSqr() > 1e-5) {
                return rawVel.normalize();
            }
        }
        return Vec3.directionFromRotation(getPitch(), getYaw()).normalize();
    }

    /**
     * 获取当前法线向量（基于旋转）
     */
    public Vec3 getCurrentNormal() {
        Quaternionf q = new Quaternionf()
                .rotateY((float) Math.toRadians(-getYaw()))
                .rotateX((float) Math.toRadians(getPitch()))
                .rotateZ((float) Math.toRadians(getRoll()));
        Vector3f upV = new Vector3f(0, 1, 0).rotate(q);
        return new Vec3(upV.x(), upV.y(), upV.z()).normalize();
    }

    public PathNode getEulerNode(Vec3 pos, Vec3 direction, Vec3 normal) {
        direction = direction.normalize();
        normal = normal.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) Math.toDegrees(Math.asin(-direction.y));
        // 已偏转yaw/pitch后，局部Y轴（无roll时法向量）的世界方向
        float pr = (float) Math.toRadians(pitch);
        float yr = (float) Math.toRadians(yaw);
        float cp = (float) Math.cos(pr);
        float sp = (float) Math.sin(pr);
        float cy = (float) Math.cos(yr);
        float sy = (float) Math.sin(yr);
        Vec3 localY = new Vec3(-sy * sp, cp, cy * sp);
        // 投影到垂直于direction的平面
        Vec3 projLocalY = localY.subtract(direction.scale(localY.dot(direction))).normalize();
        Vec3 projNormal = normal.subtract(direction.scale(normal.dot(direction))).normalize();
        // 不翻转projNormal：atan2自然处理正负，避免dot≈0时翻转振荡
        double d = projLocalY.dot(projNormal);
        Vec3 c = projLocalY.cross(projNormal);
        float roll = (float) Math.toDegrees(Math.atan2(c.dot(direction), d));
        return new PathNode(pos, yaw, pitch, roll);
    }
}