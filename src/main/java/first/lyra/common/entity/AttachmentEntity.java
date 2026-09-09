package first.lyra.common.entity;

import first.lyra.common.attachment.InvincibleData;
import first.lyra.register.LyraDamageRegister;
import first.lyra.utils.LyraStreamCodecs;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public abstract class AttachmentEntity {

    protected UUID uuid = UUID.randomUUID();
    protected final Holder<AttachmentEntityType<?>> type;
    protected Player owner = null;
    protected PlannedPath currentPlannedPath = null;
    protected final ArrayList<PathNode> historyNodes = new ArrayList<>();
    protected boolean remove = false;
    protected SyncFieldDispatcher syncFields = null;
    protected @NotNull Function<Player, DamageSource> DamageSourceSupplier = player -> new AttachmentEntityDamageSource(LyraDamageRegister.getDamageTypeHolder(LyraDamageRegister.Summon, player.level()), null, player, getCurrentPathNode().pos(), this);

    protected float damage = 0;
    protected float knockback = 0;
    protected float armorPierce = 0;
    protected int tickCount = 0;
    protected PathNode currentPathNode = null;

    protected void registerSyncFields(SyncFieldDispatcher fields) {
        fields.field(LyraStreamCodecs.FLOAT, this::getDamage, this::setDamage);
        fields.field(LyraStreamCodecs.FLOAT, this::getKnockback, this::setKnockback);
        fields.field(LyraStreamCodecs.FLOAT, this::getArmorPierce, this::setArmorPierce);
        fields.field(LyraStreamCodecs.INT, this::getTickCount, this::setTickCount);
        fields.field(LyraStreamCodecs.PATH_NODE, this::getCurrentPathNode, this::setCurrentPathNode);
    }

    public AttachmentEntity(Holder<AttachmentEntityType<?>> type) {
        this.type = type;
        init(new PathNode(Vec3.ZERO, 0, 0, 0));
    }

    @Nullable
    public DamageSource getDamageSource() {
        return getOwner() != null ? DamageSourceSupplier.apply(getOwner()) : null;
    }

    public void copyDamageSource(AttachmentEntity other) {
        setDamageSourceSupplier(other.getDamageSourceSupplier());
        setDamage(other.getDamage());
        setKnockback(other.getKnockback());
        setArmorPierce(other.getArmorPierce());
    }

    public @NotNull Function<Player, DamageSource> getDamageSourceSupplier() {
        return DamageSourceSupplier;
    }

    public void setDamageSourceSupplier(@NotNull Function<Player, DamageSource> damageSourceSupplier) {
        DamageSourceSupplier = damageSourceSupplier;
    }

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

    public SyncFieldDispatcher syncFieldRegistrar() {
        if (syncFields == null) {
            syncFields = new SyncFieldDispatcher();
            registerSyncFields(syncFields);
        }
        return syncFields;
    }

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

    public boolean isTarget(LivingEntity target) {
        if (target != null && owner != target && target.isAlive()) {
            if (target instanceof Enemy) {
                return true;
            }
            if (target instanceof Targeting targeting && targeting.getTarget() == owner) {
                return true;
            }
            if (InvincibleData.get(target).hasAttack(owner.getUUID())) {
                return true;
            }
            if (InvincibleData.get(owner).hasAttack(target.getUUID())) {
                return true;
            }
            return InvincibleData.get(target).hasAttack(this.getUuid());
        }
        return false;
    }

    public void onRemove() {
    }

    public void setPlannedPath(PlannedPath path) {
        this.currentPlannedPath = path;
    }

    public PlannedPath getCurrentPath() {
        return this.currentPlannedPath;
    }

    public PathNode getCurrentPathNode() {
        return currentPathNode;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isExecutingPath() {
        return this.currentPlannedPath != null && !this.currentPlannedPath.isFinished();
    }

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

    public void init(PathNode node) {
        this.currentPathNode = node;
        this.historyNodes.clear();
        this.historyNodes.add(node);
        this.historyNodes.add(node);
    }

    public void dimensionChange() {
        syncFields = null;
    }

    public int getHistoryNodesSize() {
        return 16;
    }

    public ArrayList<PathNode> getHistoryNodes() {
        return historyNodes;
    }

    public PathNode getRenderNode(float partialTick) {
        if (historyNodes.size() < 2) {
            return currentPathNode;
        }
        return historyNodes.get(1).lerp(currentPathNode, partialTick);
    }

    public void setCurrentPathNode(PathNode currentPathNode) {
        this.currentPathNode = currentPathNode;
    }

    /** @return 当前位置 */
    public Vec3 getPos() {
        return currentPathNode.pos();
    }

    public void setPos(Vec3 pos) {
        currentPathNode = new PathNode(pos, getYaw(), getPitch(), getRoll());
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

    public void setTickCount(int tickCount) {
        this.tickCount = tickCount;
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

    public AttachmentEntityType<?> getType(){
        return type.value();
    }

    @Deprecated
    public void writeAdditional(RegistryFriendlyByteBuf buf) {
    }

    @Deprecated
    public void readAdditional(RegistryFriendlyByteBuf buf) {
    }

    @Deprecated
    public void writeBase(RegistryFriendlyByteBuf buf) {
    }

    @Deprecated
    public void readBase(RegistryFriendlyByteBuf buf) {
    }
}
