# 普通实体接入路径关键帧与字段同步

适用 Minecraft 1.21.1 / NeoForge。实体保留原有的 `Entity`、`Mob`、`Projectile` 等父类，实现 `first.lyra.common.entity.IPathEntity` 即可使用虚拟实体已有的 `PathNode`、`PlannedPath`、`RenderKeyframe` 和 `SyncFieldDispatcher`。

接口提供默认行为，实体自行在 `tick()` 末尾调用 `tickPath()`；Lyra 不会自动替实体调用。`tickPath()` 同时处理路径和字段同步，每 tick 调用一次，两端都要执行。只需要字段同步时，实现 `ISyncEntity` 并调用 `tickSyncFields()`。在实体 NBT 读写方法中分别调用 `loadSyncFields(tag)` 和 `saveSyncFields(tag)`，已注册字段便会自动保存和恢复。

## 完整实体示例

下面是一个展示物品模型的普通实体。将包名替换为自己的包名，按项目现有方式注册实体类型；注册工厂使用 `ExampleEntity::new`。

```java
package example.entity;

import first.lyra.common.attachmentEntity.PathNode;
import first.lyra.common.attachmentEntity.PlannedPath;
import first.lyra.common.attachmentEntity.SyncFieldDispatcher;
import first.lyra.common.entity.IPathEntity;
import first.lyra.utils.LyraStreamCodecs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ExampleEntity extends Entity implements IPathEntity {

    private boolean shooting;
    private float power = 1f;

    public ExampleEntity(EntityType<? extends ExampleEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void registerSyncFields(SyncFieldDispatcher fields) {
        IPathEntity.super.registerSyncFields(fields);
        fields.field(LyraStreamCodecs.BOOL, () -> shooting, value -> shooting = value);
        fields.field(LyraStreamCodecs.FLOAT, () -> power, value -> power = value);
    }

    @Override
    public void tick() {
        super.tick();
        tickPath();
    }

    public void moveAlongArc(Vec3 end) {
        if (!level().isClientSide()) {
            Vec3 start = position();
            Vec3 control = start.lerp(end, 0.5).add(0, 3, 0);
            PlannedPath path = PlannedPath.sample("arc", 20, 4, t -> {
                Vec3 pos = start.lerp(control, t).lerp(control.lerp(end, t), t);
                Vec3 direction = control.subtract(start).scale(1 - t).add(end.subtract(control).scale(t));
                return new PathNode(pos, PathNode.yawFromDirection(direction),
                        PathNode.pitchFromDirection(direction), 360f * t);
            });
            path.run(20, () -> shooting = false);
            shooting = true;
            power = 2f;
            setPath(path);
        }
    }

    public boolean isShooting() {
        return shooting;
    }

    public float getPower() {
        return power;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        loadSyncFields(tag);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        saveSyncFields(tag);
    }
}
```

现有实体已有 `tick()`、存档方法或原版同步字段时，保留这些实现，把 `tickPath()` 放在本 tick 的服务端行为之后。示例基类 `Entity` 的 `defineSynchedData` 是抽象方法，所以方法体为空；继承有实现的父类时继续调用 `super.defineSynchedData(builder)`。

服务端创建并设定位置后，将实体加入世界，再调用 `moveAlongArc(end)`。示例会沿二次贝塞尔曲线运动并翻滚一周；路径终点回调只在服务端执行。`shooting` 与 `power` 自动同步并存档，客户端可以在渲染时读取。

## 接入客户端渲染

渲染器使用 `EntityPathRender.translate` 修正 tick 内曲线位置，再使用 `rotate` 应用完整三轴姿态。下面的渲染器不额外施加原版 yaw/pitch，模型朝向沿用 `PathNode` 的局部 +Z 约定。

```java
package example.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import first.lyra.client.render.EntityPathRender;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ExampleRenderer extends EntityRenderer<ExampleEntity> {

    private final ItemRenderer itemRenderer;
    private final ItemStack model = new ItemStack(Items.DIAMOND_SWORD);

    public ExampleRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ExampleEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        EntityPathRender.translate(entity, poseStack, partialTick);
        EntityPathRender.rotate(entity, poseStack, partialTick);
        itemRenderer.renderStatic(model, ItemDisplayContext.FIXED, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffers, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ExampleEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
```

在自己模组的客户端 `EntityRenderersEvent.RegisterRenderers` 监听器内注册：

```java
event.registerEntityRenderer(MY_ENTITY_TYPE.get(), ExampleRenderer::new);
```

渲染器类与注册逻辑放在客户端代码中。`translate` 在朝向、缩放等模型变换之前调用；`rotate` 已包含 roll，不再调用 `roll`。模型固有的朝向和中心偏移在它们之后调整。`super.render` 保留实体名称等父类渲染行为。

已有自定义渲染器若已按 `getRenderNode(partialTick)` 施加 yaw/pitch，可仅调用 `EntityPathRender.roll`，它绕变换后的局部 +Z 轴旋转。所有角度均为度，朝向插值使用 `PathNode.lerp` 的四元数最短弧。

### LivingEntityRenderer / MobRenderer

生物模型正面沿局部 -Z，基础渲染器已经旋转了 `180° - yaw`。在现有渲染器的 `render` 中用 push/pop 包住 `translate` 和 `super.render`，再重写 `setupRotations`，使用视觉节点的 yaw 后追加 `rotateLiving`：

```java
@Override
public void render(MyMob entity, float yaw, float partialTick,
                   PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
    poseStack.pushPose();
    EntityPathRender.translate(entity, poseStack, partialTick);
    super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    poseStack.popPose();
}

@Override
protected void setupRotations(MyMob entity, PoseStack poseStack, float bob,
                              float bodyYaw, float partialTick, float scale) {
    super.setupRotations(entity, poseStack, bob,
            entity.getRenderNode(partialTick).yaw(), partialTick, scale);
    EntityPathRender.rotateLiving(entity, poseStack, partialTick);
}
```

`rotateLiving` 追加局部 -X 俯仰和 -Z 翻滚，使最终整体姿态与 `PathNode` 一致。普通站立生物可使用上面的接法；睡眠、游泳、死亡翻转等特殊姿态继续由具体渲染器决定组合顺序。模型自身的头部俯仰动画仍会生效，若路径 pitch 只表示整体朝向，应在模型的 `setupAnim` 中自行决定是否保留这层头部俯仰。

## 路径方法与行为

| 方法 | 行为 |
| --- | --- |
| `setPath(List<PathNode>)` / `setPath(PlannedPath)` | 设置待执行路径，并立即应用第一个逻辑节点。 |
| `setPlannedPath(PlannedPath)` | 从当前节点开始逐 tick 消费路径，不立即应用首节点。传入 `null` 可取消。 |
| `getCurrentPath()` / `isExecutingPath()` | 获取路径和执行状态。 |
| `getCurrentPathNode()` / `setCurrentPathNode(PathNode)` | 读取当前逻辑节点；设置时同时更新实体位置和 yaw/pitch。 |
| `getHistoryNodes()` | 最近最多 16 个 tick 的节点，最新记录在首位。 |
| `getRenderNode(float)` | 客户端视觉位置与三轴姿态。 |
| `getRoll()` / `setRoll(float)` | 读取或设置当前翻滚角。 |

路径在服务端设定和推进。`PlannedPath.sample("arc", 20, 4, sampler)` 包含 21 个逻辑节点，每段添加 3 个中间渲染关键帧。`20` 和 `4` 都须为正数；首节点沿用虚拟实体的消费时序，在第一次推进时消费，因此整个示例经过 21 次 `tickPath()` 到达终点。也可使用 `path.render(index, frames)` 手动补帧，下标指目标逻辑节点，`partialTick` 严格递增且处于 0 与 1 之间，两个端点自动补齐。

渲染关键帧只影响显示。逻辑位置和回调每 tick 推进一次，客户端一次 tick 收到多次更新时使用最后一个区间；没有新更新时停留在终点。路径位移直接调用实体 `setPos`，需要沿途碰撞检测时由实体自行实现。父类 AI 和物理在 `tickPath()` 前执行，路径执行期间最后应用的路径位置生效；路径结束或取消后继续采集父类更新的位置，roll 保留。

## 字段注册与继承

字段按注册顺序分配位图位置。客户端与服务端必须以相同顺序、相同 codec 注册，不能按运行时状态增减字段。注册方法只添加 getter/setter，不在其中访问 `getSyncData()`、`getPathData()` 或修改实体状态；实际值在编码、解码时读取。

直接实现 `IPathEntity` 的实体调用 `IPathEntity.super.registerSyncFields(fields)`，以注册内置路径字段。继承已经接入接口的实体时，子类调用 `super.registerSyncFields(fields)` 再追加自己的字段；父类已经在 `tick()` 内调用 `tickPath()` 时，子类不要重复调用。实体父类已有同名具体方法时，Java 会优先选父类方法，实体需要显式重写并委托接口默认方法。

仅同步字段的实体接法如下，将代码加入自己的实体类：

```java
public class MyEntity extends ExistingEntity implements ISyncEntity {

    @Override
    public void registerSyncFields(SyncFieldDispatcher fields) {
        ISyncEntity.super.registerSyncFields(fields);
        fields.field(LyraStreamCodecs.FLOAT, () -> power, value -> power = value);
    }

    @Override
    public void tick() {
        super.tick();
        tickSyncFields();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadSyncFields(tag);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        saveSyncFields(tag);
    }
}
```

这里的 `ExistingEntity`、构造器与 `power` 字段使用已有实体的定义。可使用任何适配 `RegistryFriendlyByteBuf` 的 `StreamCodec`；需要世界解析的字段可使用 `fields.field(codec, getter, (level, value) -> ...)` 重载。

同步方向为服务端到追踪该实体的客户端；玩家实体还包括其自身客户端。NeoForge 附件负责初次追踪时的完整快照和离开追踪范围后的停止发送，后续只发送变化字段。数据在客户端调用接口 tick 时应用，因此本 tick 的其他客户端逻辑若需要最新字段，应放在该调用之后。修改客户端字段不会上传服务端。

## NBT 存档

`saveSyncFields(tag)` 将注册器中的全部字段写入实体 NBT 的 `lyra:sync_fields`，`loadSyncFields(tag)` 使用相同 codec 和 setter 恢复。无需为每个字段再写 `put` / `get`。两种接口都支持这两个默认方法；继承已有实体时先调用父类 NBT 方法，再调用接口方法。父类已接入存档时，子类只需继续调用 `super`，无需重复调用。

每个字段按注册下标保存为独立字节数组，存档不会改变网络增量同步的比较基线。缺失字段保留实体默认值，多余字段忽略，因此可以在注册列表末尾追加字段；已有字段的顺序和 codec 属于存档格式，不能直接重排或替换。旧版示例手写的 `Shooting`、`Power` 不会自动迁移，需要兼容时由实体自行读取后续交给新格式保存。

NBT 复用 `StreamCodec`，字段应编码可跨重启恢复的值。实体引用应保存 UUID，再按业务时机解析，不要直接持久化运行时实体 ID；注册表值应使用资源位置等稳定标识的 codec，避免只保存网络数字 ID。

`IPathEntity` 的逻辑节点、roll、同步 tick 和渲染关键帧同样参与存档。读档后重置历史节点和插值起点，不重放保存前的视觉区间。待执行的 `PlannedPath` 及 `Runnable` 回调未注册为同步字段，仍由服务端业务逻辑在加载后重新规划；示例中的 `shooting` 会保留保存时的值，需要继续运动时重新创建路径。
