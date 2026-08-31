# Lyra API 调用参考（WIKI）

> 面向宿主模组（迁移方）的 Lyra 库调用手册。基于当前仓库源码整理。
> 环境：Minecraft 1.21.1 · NeoForge 21.1.248 · Java 21 · GeckoLib 4.8.4（compileOnly）

---

## 目录

**扩展功能**

- [1. 虚拟实体体系](#feat-1)
- [2. 动态光照](#feat-2)
- [3. 局部无敌帧](#feat-3)
- [4. 外挂 GeckoLib 渲染](#feat-4)
- [5. 创造模式分页与动画横幅](#feat-5)
- [6. 物品注册数据生成集成](#feat-6)
- [7. 伤害显示](#feat-7)
- [8. 套装效果集成](#feat-8)

**API 调用参考**

1. [引入 Lyra 依赖](#1-引入-lyra-依赖)
2. [核心概念](#2-核心概念)
3. [API 速查表](#3-api-速查表)
4. [玩家数据：LyraHelper](#4-玩家数据lyrahelper)
5. [自定义注册表](#5-自定义注册表)
6. [仆从系统](#6-仆从系统)
7. [射弹系统](#7-射弹系统)
8. [盔甲系统](#8-盔甲系统)
9. [通用粒子](#9-通用粒子)
10. [音效播放](#10-音效播放)
11. [无敌帧攻击](#11-无敌帧攻击)
12. [属性与伤害](#12-属性与伤害)
13. [创造模式 Tab](#13-创造模式-tab)
14. [语言条目](#14-语言条目)
15. [数据生成接入](#15-数据生成接入)
16. [工具类](#16-工具类)

---

## <a id="features"></a>扩展功能

Lyra 提供以下开箱即用的高级能力，均基于实际源码实现。事件挂接全部由 Lyra 自动完成（`@EventBusSubscriber` + mixin），宿主只需声明内容。

### <a id="feat-1"></a>1. 虚拟实体体系（召唤物 / 射弹抽象基类）

不占用原版实体 ID 的战斗单位框架，实体以纯数据对象存储在**玩家附件** `AttachmentEntityData` 中：

- **抽象基类已提供**：`AttachmentEntity`（UUID、所有者、`PathNode` 路径节点、历史轨迹、伤害/击退/护甲穿透属性、`writeBase/readBase` 网络序列化）→ 派生 `Servant`（召唤物，AI 驱动）与 `Projectile` / `AttachingProjectile`（射弹，动量驱动，支持黏着目标跟随）
- 分组存储（`AttachmentEntityData.Type`：`Servant` / `SentryServant` / `ExtraServant` / `Projectile`），延迟队列添加、标记移除、**栏位溢出自动遣散**
- 三层结构（Type → 实体类型 → 实体列表）随玩家附件网络同步，客户端按 UUID 复用实例
- 渲染：`AttachmentEntityRenderDispatcher` 按实体类型注册 `IAttachmentEntityRenderer`，基于历史节点插值（`getRenderNode(partialTick)`）平滑渲染；内置第一人称近距离透明度修正与调试描边

### <a id="feat-2"></a>2. 动态光照

`DynamicLightDispatcher` 双路径架构：

- **光源注册**：`addLightSources(PathNode, AABB, light)`（按 AABB 沿 Z 轴 ≤0.5 格间距放置点光源，并按 yaw/pitch/roll 旋转到世界空间）或 `addLightSources(Vec3, light)`，半径 7.75 格线性衰减，只提升 block-light、不动 sky-light
- **方块路径**：mixin `LevelRenderer.getLightColor` 返回 packed light，GPU 顶点插值实现跨方块平滑
- **实体路径**：mixin `EntityRenderer.getPackedLightCoords` 用实体眼睛的连续 Vec3 精确计算，天然无跨方块跳变
- 分帧 section 脏标记，客户端配置开关（`ClientConfig.DynamicLight`）

### <a id="feat-3"></a>3. 局部无敌帧

`InvincibleData`（LivingEntity 附件）实现**按攻击者 UUID 隔离**的无敌帧：

- `PARTIAL`：每个攻击者 UUID 独立无敌计时（`partialInvincibleFrames`），互不影响；`GLOBAL`：全局无敌
- `InvincibleData.attack(target)` 链式构建器：`attacker(uuid)` + `damageSource` + `damageAmount` + `invincibleTime(ticks)` + `global()` + `effect(...)`，`apply()` 时**绕过原版 invulnerableTime 强制造成伤害**，并按配置写入无敌帧
- `recordHit(uuid, ticks)` 写入攻击历史（不造成伤害），`hasAttack(uuid)` 查询——`LivingDamageEvent.Post` 中玩家造成的伤害自动记录（100 tick）
- 对原版目标：敌人攻击过玩家 / 玩家攻击过敌人 / 召唤物被攻击，均纳入仆从 `isTarget` 判定

### <a id="feat-4"></a>4. 外挂 GeckoLib 渲染

`GeoSideloader`：**不依赖实体或方块、无需继承或扩展的静态 Geo 模型渲染**：

```java
GeoSideloader.create(Lyra.rl("laser_minigun"))   // 模型 RL → geo/texture/animation 三资源自动推导
        .setAnimation("shooting", tickProgress)   // 自定义动画进度（tick 域直接注入）
        .hideBone("magazine")                     // 骨骼隐藏（含子骨骼）
        .render(poseStack, bufferSource, partialTick, packedLight);
```

- **自定义动画进度**：`GeoAnimationSampler` 从 `GeckoLibCache` 直接读取 Animation 关键帧（支持 easing、LOOP 取模/钳制），绕开 GeckoLib 的 `AnimationController`/`handleAnimations` 管线，进度由调用方精确注入
- **骨骼隐藏**：`hideBone(String...)` 按骨骼名隐藏（含子骨骼）
- 每帧新实例、无跨帧状态；渲染前后均 reset 共享 `GeoBone` 到初始姿态，防止交叉污染

### <a id="feat-5"></a>5. 创造模式物品栏分页 + 动画横幅

- `Section(order, texture, animBanner, tag)` 定义分段：**特征标签**决定物品自动归组（无需注册时指定）
- `CreativeTabDispatcher.registerTab(tab)` 让宿主 Tab 接入 Lyra 分组逻辑；mixin `CreativeModeTab.buildContents` 将物品按分段排序，每段前插整行空位（横幅行）
- 动画横幅：`AnimBanner(frameHeight, frameTime, totalFrames)` 帧动画贴图，`AbstractContainerScreenMixin` 在物品栏中逐段渲染横幅（跟随滚动对齐，**鼠标悬停时播放**）

### <a id="feat-6"></a>6. 物品注册时的数据生成集成

`LyraItemRegisterBuilder.build(DeferredRegister.Items, name, supplier)` 注册物品的同时链式声明数据生成内容，`runData` 一键输出：

- `.itemLanguage / blockLanguage / servantLanguage / itemLanguageTooltip` → 语言条目（`LyraLanguageRegister`）
- `.recipe(...)` → 合成表（`LyraRecipeProvider`）
- `.itemTag(...)` → 物品标签（`LyraItemTagsProvider`，护甲自动细分到原版护甲分类标签）
- `.itemModel(...)`（内置 `basicModel` / `handheldItem`）→ 物品模型（`LyraItemModelProvider`）

### <a id="feat-7"></a>7. 伤害显示

- **渲染**：`DamageInfo` 贴图字形（0-9 + 小数点共 11 glyph），直接构造 `Matrix4f` 顶点写入**绕过 PoseStack**（每帧相机朝向预计算一次、全部数字共享），暴击着色、10 tick 闪烁脉冲、`EasingCurve` 缓动淡入淡出
- **样式**：`DamageInfoStyle` 由 **JSON 数据包**驱动（按 damageType 查表：贴图/尺寸/颜色/寿命，不占网络）
- **批量管道**：服务端 `LivingDamageEvent.Post` 自动收集 → `DamageInfoData.build(level).damageType(...).damageAmount(...).pos(...).velocity(...).critical(...).emit()` 链式构建 → 每 tick `BatchedDamageInfoPayload` 维度广播 → 客户端 `DamageInfoRenderDispatcher` 按贴图分组渲染
- 暴击标记：`IDamageSourceCritical`（mixin 注入 `lyra$isCritical`）

### <a id="feat-8"></a>8. 套装效果集成

`ArmorSet.builder(id).piece(...).modifier(attr, amount, op).onStart(...).onRemove(...).tooltip(index, en, zh).build()` 注册到 `LyraRegistries.ARMOR_SETS`，由 `ArmorSet.handler`（`LivingEquipmentChangeEvent` 自动挂接）维护：

- 全套装匹配（4 个护甲槽 vs piece 列表）→ 首次激活回调 `onStart` / 失效回调 `onRemove`
- 套装属性修饰：条件增删（`AttributeUtils.condition` 幂等），穿上才生效、脱掉自动移除
- 套装 tooltip 自动注册语言条目

> 以上功能**无需宿主注册任何事件**：Lyra 通过 `@EventBusSubscriber` 自动挂接全部事件（装备变化、右键、伤害、tick 等），宿主只需按 §4~§16 的 API 声明内容。

---

## 1. 引入 Lyra 依赖

Lyra 已配置 `maven-publish`，可发布到本地 Maven 后作为库使用：

```bash
# 在 Lyra 项目目录执行
./gradlew publishToMavenLocal
```

宿主 mod 的 `build.gradle`：

```groovy
repositories {
    mavenLocal()
}

dependencies {
    implementation "first:lyra:1.0.0"   // group:mod_id:version（见 Lyra 的 gradle.properties）
    // 可选：渲染 Gecko 模型用
    compileOnly("software.bernie.geckolib:geckolib-neoforge-1.21.1:4.8.4")
}
```

注意：Lyra 内部的 `@EventBusSubscriber`、`@Mod` 均使用 `lyra` modid，宿主与 Lyra 共存互不干扰。

---

## 2. 核心概念

### 2.1 AttachmentEntity（附件实体）系统

Lyra 不使用 Minecraft 原版实体，而是**虚拟实体**：

- `AttachmentEntity`（`first.lyra.common.entity`）：抽象基类，提供 UUID、所有者、路径节点（`PathNode`）、历史轨迹、伤害/击退/护甲穿透属性、网络序列化
- `Servant`：仆从（AI 驱动），继承 `AttachmentEntity`
- `Projectile`：射弹（动量驱动），继承 `AttachmentEntity`

虚拟实体存储在每个玩家的 `AttachmentEntityData` 附件中，按 `Type` 分组：

```java
public enum AttachmentEntityData.Type {
    Servant,        // 普通仆从（占仆从栏位）
    SentryServant,  // 哨戒仆从（占哨戒栏位）
    ExtraServant,   // 额外仆从（不占栏位）
    Projectile      // 射弹
}
```

### 2.2 自定义注册表（`first.lyra.register.LyraRegistries`）

| 注册表 | 条目类型 | 用途 |
|--------|---------|------|
| `LyraRegistries.ATTACHMENT_ENTITY_TYPES` | `AttachmentEntityType<T>` | 注册仆从/射弹类型 |
| `LyraRegistries.ARMOR_SETS` | `ArmorSet` | 注册盔甲套装 |

两者均为 NeoForge 新注册表（`RegistryBuilder`），网络同步。

---

## 3. API 速查表

| 包路径 | 类 | 一句话用途 |
|--------|-----|-----------|
| `first.lyra.api` | `LyraHelper` | 玩家数据统一入口：召唤、栏位查询、附件访问 |
| `first.lyra.register` | `LyraRegistries` | 自定义注册表（仆从类型 / 盔甲套装） |
| `first.lyra.register` | `LyraItemRegisterBuilder` | 宿主物品注册 + 语言/配方/标签/模型 datagen |
| `first.lyra.register` | `LyraAttributeRegister` | 7 个注册属性（仆从栏位/伤害/索敌等） |
| `first.lyra.register` | `LyraDamageRegister` | 仆从伤害类型 `lyra:servant` + 获取 DamageSource |
| `first.lyra.register` | `LyraLanguageRegister` | 运行时语言条目收集 |
| `first.lyra.common.builder` | `ServantWeaponItemBuilder` | 链式构建仆从武器物品 |
| `first.lyra.common.item` | `IServantWeaponItem` | 仆从武器接口（伤害/击退/穿透/召唤/移除） |
| `first.lyra.common.builder` | `ArmorSetBuilder` | 链式构建盔甲套装 |
| `first.lyra.common.builder` | `AttributeArmorItemBuilder` | 构建带属性修饰的盔甲物品 |
| `first.lyra.common.particle.genericParticle` | `GenericParticleBuilder` | 通用粒子构建器（颜色/寿命/旋转/阻力/大小） |
| `first.lyra.common.sound` | `Playable` | 音效播放（随机音高/音量） |
| `first.lyra.common.attachment` | `InvincibleData` | 无敌帧管理（部分/全局无敌 + 链式攻击） |
| `first.lyra.common.creativeTab` | `CreativeTabDispatcher` / `Section` | 创造模式 Tab 分组逻辑 |
| `first.lyra.utils` | `AttributeUtils` | 条件性属性修饰符增删 |
| `first.lyra.utils` | `EasingCurve` | 缓动曲线（预置 + 贝塞尔） |

---

## 4. 玩家数据：LyraHelper

```java
import first.lyra.api.LyraHelper;
import first.lyra.common.attachment.AttachmentEntityData;
import first.lyra.common.entity.AttachmentEntity;
import net.minecraft.world.entity.player.Player;

LyraHelper helper = LyraHelper.get(player);

// 访问玩家附件数据
AttachmentEntityData data = helper.getEntityData();

// 查询仆从栏位
boolean ok = helper.canSummon(AttachmentEntityData.Type.Servant, 1); // 剩余 >= 1 格
int max   = helper.getMaxCount(AttachmentEntityData.Type.Servant);   // 栏位上限（属性驱动）
int used  = helper.getUsedSlots(AttachmentEntityData.Type.Servant);  // 已用栏位

// 添加虚拟实体（延迟队列，tick 后生效）
helper.add(AttachmentEntityData.Type.Servant, myServant);
// 添加的实体会被自动 setOwner(player)

// 目标缓存（索敌用）
first.lyra.common.attachment.TargetCache cache = helper.getTargetCache();

Player p = helper.getPlayer();
```

---

## 5. 自定义注册表

### 5.1 注册仆从/射弹类型

`AttachmentEntityType<T>` 是一个 record：`new AttachmentEntityType<>(factory)`。

```java
import first.lyra.common.entity.AttachmentEntityType;
import first.lyra.register.LyraRegistries;
import net.minecraft.resources.Identifier;

// 在任何初始化阶段（如 mod 构造器或 RegisterEvent）：
Identifier id = Identifier.fromNamespaceAndPath("my_mod", "sword_servant");
LyraRegistries.ATTACHMENT_ENTITY_TYPES.register(id, new AttachmentEntityType<>(MyServant::new));
```

> 新注册表条目无顺序依赖，宿主任意时机注册即可。可用 `DeferredRegister.create(LyraRegistries.ATTACHMENT_ENTITY_TYPES.key(), ...)` 或直接 `register`。

### 5.2 注册盔甲套装

```java
import first.lyra.common.armorSet.ArmorSet;
import first.lyra.register.LyraRegistries;
import first.lyra.common.builder.ArmorSetBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

ArmorSet set = ArmorSet.builder(Identifier.fromNamespaceAndPath("my_mod", "knight"))
        .piece(MyItems.KNIGHT_HELMET)   // 收集整套部件
        .piece(MyItems.KNIGHT_CHESTPLATE)
        .piece(MyItems.KNIGHT_LEGGINGS)
        .piece(MyItems.KNIGHT_BOOTS)
        .modifier(Attributes.MAX_HEALTH, 10, AttributeModifier.Operation.ADD_VALUE) // 套装全穿上才生效
        .onStart(player -> { /* 首次激活回调 */ })
        .onRemove(player -> { /* 失效回调 */ })
        .tooltip(0, "Knight Set", "骑士套装")   // 套装奖励 tooltip 语言
        .build();

LyraRegistries.ARMOR_SETS.register(
        Identifier.fromNamespaceAndPath("my_mod", "knight"), set);
```

> 生效判定：玩家 4 个护甲槽物品与 `piece` 列表完全匹配。生效回调与属性修饰由 Lyra 的 `ArmorSet.handler`（挂在 `LivingEquipmentChangeEvent`）自动维护，宿主无需监听。

---

## 6. 仆从系统

### 6.1 创建仆从实体

继承 `Servant`，实现 `getSearchDistance()` 与 `registerGoals()`：

```java
import first.lyra.common.servant.Servant;
import first.lyra.common.servant.ServantGoal;
import first.lyra.common.servant.ServantGoalSelector;
import net.minecraft.world.entity.LivingEntity;

public class MyServant extends Servant {

    public MyServant() {
        super();
    }

    @Override
    public void registerGoals(ServantGoalSelector selector) {
        selector.addGoal(1, new ServantGoal<>(this) {
            @Override
            public boolean canUse() {
                return servant.getTarget() != null;
            }
            @Override
            public void tick() {
                // 朝目标移动、攻击……
                LivingEntity target = servant.getTarget();
            }
        });
    }

    @Override
    public int getSearchDistance() {
        return 16;   // 索敌距离（受 ServantSearchRange 属性加成）
    }
}
```

仆从可重写/使用的方法：

| 方法 | 说明 |
|------|------|
| `registerGoals(ServantGoalSelector)` | 注册 AI 目标（优先级低=先执行，可被更高优先级打断） |
| `getSearchDistance()` | 索敌距离（抽象方法，必实现） |
| `isTarget(LivingEntity)` | 判定目标是否有效（默认：敌对生物/攻击过玩家的/玩家攻击过的） |
| `getDamageSource()` | 构造仆从专属 `ServantDamageSource`（`lyra:servant` 伤害类型） |
| `getSlotCost()` / `setSlotCost(int)` | 占用的栏位数 |
| `getOrder()` / `getSameSize()` | 同类型仆从中的顺序/数量（AI 阵型用） |
| `getGoalSelector()` | 访问 AI 选择器（可 `addGoal`/`removeGoal`） |
| `setTarget(LivingEntity)` / `getTarget()` | 目标管理 |

### 6.2 仆从武器

```java
import first.lyra.common.builder.ServantWeaponItemBuilder;
import first.lyra.common.entity.AttachmentEntityType;
import first.lyra.register.LyraItemRegisterBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.registries.DeferredRegister;

DeferredRegister.Items items = DeferredRegister.createItems("my_mod");

// 注册仆从类型
AttachmentEntityType<MyServant> type = new AttachmentEntityType<>(MyServant::new);
LyraRegistries.ATTACHMENT_ENTITY_TYPES.register(
        Identifier.fromNamespaceAndPath("my_mod", "my_servant"), type);

// 构建武器
ServantWeaponItemBuilder<MyServant> weaponBuilder = new ServantWeaponItemBuilder<>(() -> type)
        .damage(5.0f)          // 仆从伤害
        .knockback(0.5f)       // 击退
        .armorPierce(2.0f)     // 护甲穿透
        .sentryServant()       // 可选：设为哨戒仆从
        .sound(() -> SoundEvents.SKELETON_DEATH)   // 召唤音效
        .onRemove(player -> { /* 遣散回调 */ })
        .properties(p -> p.durability(1000));

// 注册物品 + 自动生成语言/模型
LyraItemRegisterBuilder.build(items, "my_weapon", weaponBuilder::build)
        .itemLanguage("My Weapon", "我的武器")
        .itemModel(LyraItemRegisterBuilder::handheldItem)
        .build();
```

**交互逻辑（Lyra 已自动挂接 `PlayerInteractEvent.RightClickItem`）**：

- 右键：召唤仆从（需 `canSummon` 通过，否则无事发生）
- 潜行 + 右键：遣散该类型全部仆从
- 冷却 4 tick，每次召唤播放音效

自定义召唤逻辑（重写 `summonAction`）：

```java
weaponBuilder.summon((weapon, player, stack) -> {
    MyServant servant = weapon.createServant(player, stack); // 已初始化伤害/击退/穿透
    // 自定义位置/条件……
    LyraHelper.get(player).add(AttachmentEntityData.Type.Servant, servant);
});
```

`IServantWeaponItem<T>` 接口可自行实现（不依赖 Builder），需实现：

```java
@NotNull AttachmentEntityType<T> getType();
boolean isSentryServant();
void summon(@NotNull Player player, @Nullable ItemStack itemStack);
float getServantDamage(@Nullable Player player, @Nullable ItemStack itemStack);
float getServantKnockback(@Nullable Player player, @Nullable ItemStack itemStack);
float getServantArmorPierce(@Nullable Player player, @Nullable ItemStack itemStack);
// 可选覆写：getSummonTooltip / getSoundEvent / createServant / remove
```

---

## 7. 射弹系统

继承 `Projectile`（或 `AttachingProjectile`），配置物理属性后发射：

```java
import first.lyra.common.projectile.Projectile;
import first.lyra.common.entity.PathNode;
import net.minecraft.world.phys.Vec3;

public class MyProjectile extends Projectile {

    public MyProjectile() {
        super();
    }

    public MyProjectile(Vec3 startPos, Vec3 direction) {
        super(startPos, direction);
    }

    @Override
    public void tick() {
        super.tick();
        if (!owner.level().isClientSide()) {
            // 命中检测、伤害……
        }
    }

    @Override
    public float getSpinSpeed() { return 0f; }        // 自旋（弧度/tick）
    @Override
    public int getTrailDuration() { return 15; }       // 拖尾时长（tick）
    @Override
    public double getMaxDistance() { return 128.0; }   // 最大飞行距离
    @Override
    public int getHistoryNodesSize() { return 8; }     // 轨迹历史长度（默认 8）
}
```

发射：

```java
Vec3 direction = player.getLookAngle().normalize();
MyProjectile projectile = new MyProjectile(player.getEyePosition().add(direction.scale(0.5)), direction);
projectile.setDamage(6.0f);
projectile.setKnockback(0.3f);
projectile.setArmorPierce(1.0f);
projectile.setDrag(0.99f);      // 速度衰减 [0,1]，1=无阻力
projectile.setGravity(0.02f);
projectile.setMaxSpeed(1.5f);   // 格/tick 上限
projectile.setMaxLife(200);     // 最大寿命 tick
projectile.setDamageSource(servant.getDamageSource()); // 仆从伤害源（自动继承伤害属性）
projectile.applyForce(new Vec3(0, 0.1, 0));            // 附加力

projectile.join(player);        // 关键：加入玩家射弹数据，开始 tick
```

`AttachingProjectile` 附加方法：`attachTo(Vec3)`（吸附到位置）、`setAttachedTarget(LivingEntity)`（跟随目标）、`isAttached()`。

物理模型：每 tick `velocity = velocity * drag + (0, gravity, 0)`，限速后更新朝向与位置，超出 `maxLife` 或 `maxDistance` 自动移除。

---

## 8. 盔甲系统

### 8.1 盔甲物品

```java
import first.lyra.common.builder.AttributeArmorItemBuilder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.core.Holder;

AttributeArmorItemBuilder builder = AttributeArmorItemBuilder.builder(ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET);
ArmorItem helmet = builder
        .modifier(Attributes.ARMOR, 3.0, AttributeModifier.Operation.ADD_VALUE)
        .modifier(Attributes.MAX_HEALTH, 2.0, AttributeModifier.Operation.ADD_VALUE)
        .properties(p -> p.rarity(Rarity.RARE))
        .build();
```

> 构建器已内置：无限耐久 + 不可破坏 + 无附魔。`modifier(attribute, amount, operation)` 自动使用 `lyra:armor_<type>` 修饰符 ID，槽位自动匹配。

### 8.2 盔甲套装

见 [5.2 注册盔甲套装](#52-注册盔甲套装)。套装激活后自动：调用 `onStart`/`onRemove` 回调 + 按条件增删属性修饰符（`AttributeUtils.condition`）。

---

## 9. 通用粒子

```java
import first.lyra.common.particle.genericParticle.GenericParticleBuilder;
import first.lyra.common.particle.genericParticle.GenericParticleOptions;
import net.minecraft.core.particles.ParticleOptions;

GenericParticleOptions options = GenericParticleBuilder.create()
        .centerColor(0xFF0000)      // 中心色 RGB
        .edgeColor(0x00FF00)        // 边缘色 RGB
        .lifetime(20)               // 寿命（tick）
        .lifetimeRandom(10)         // 寿命抖动
        .spin(0.1f)                 // 旋转速度（弧度/tick）
        .spinRandom(0.05f)          // 旋转抖动
        .friction(0.98f)            // 阻力（每 tick 速度乘数）
        .scale(0.5f)                // 大小
        .scaleRandom(0.2f)          // 大小抖动
        .build();

// 直接使用（原版方式）
level.addParticle(options, x, y, z, vx, vy, vz);
// 或通过 Lyra 的批量粒子管道（服务端累积，客户端批量渲染）
```

粒子类型：`LyraParticleRegister.Generic`（`lyra:generic`），`GenericParticleOptions` 支持网络流编解码，可直接跨端发送。

---

## 10. 音效播放

```java
import first.lyra.common.sound.Playable;
import net.minecraft.world.phys.Vec3;

// 随机音高 0.9~1.1、音量 0.9~1.1，全玩家可闻
Playable.play(soundEvent, level, new Vec3(x, y, z), net.minecraft.sounds.SoundSource.PLAYERS);
// 也支持 Holder<SoundEvent> 重载；null 安全
```

---

## 11. 无敌帧攻击

Lyra 提供可配置的无敌帧攻击系统（默认实体无敌帧会被绕过，由 Lyra 自行控制）：

```java
import first.lyra.common.attachment.InvincibleData;
import net.minecraft.world.effect.MobEffects;

// 链式攻击构建器（可直接伤害，绕过原版无敌帧）
boolean hurt = InvincibleData.attack(target)
        .attacker(player.getUUID())              // 攻击者 UUID（PARTIAL 无敌按攻击者隔离）
        .damageSource(servant.getDamageSource()) // 伤害来源（缺省用 GENERIC）
        .damageAmount(8.0f)                      // 伤害值（必填 > 0）
        .invincibleTime(10)                      // 无敌帧 tick（默认 0 = 不设）
        // .global()                             // 切换为全局无敌（所有来源都打不动）
        .effect(new MobEffectInstance(MobEffects.WEAKNESS, 100)) // 命中附加效果（可选）
        .apply();                                // 返回是否造成伤害

// 查询"是否被某玩家攻击过"（伤害历史记录，记录 100 tick）
boolean attackedByMe = InvincibleData.get(entity).hasAttack(player.getUUID());
// 手动写入攻击历史（不同步伤害）
InvincibleData.get(entity).recordHit(player.getUUID(), 100);
```

> `LivingDamageEvent.Post` 中 Lyra 已自动为所有玩家造成的伤害记录攻击历史。

---

## 12. 属性与伤害

### 12.1 Lyra 注册的属性（`LyraAttributeRegister`）

| 属性 | 默认值 | 范围 | 说明 |
|------|--------|------|------|
| `HealthRegen` | 0 | ±1,000,000 | 生命再生（挂接在所有 LivingEntity 上） |
| `ServantMaxCount` | 1 | 0~1000 | 仆从栏位上限 |
| `SentryServantMaxCount` | 1 | 0~1000 | 哨戒仆从栏位上限 |
| `ServantDamage` | 1 | 0~1,000,000 | 仆从伤害倍率 |
| `ServantKnockback` | 1 | 0~10 | 仆从击退倍率 |
| `ServantArmorPierce` | 1 | 0~1,000,000 | 仆从护甲穿透倍率 |
| `ServantSearchRange` | 1 | 0~10 | 仆从索敌范围倍率 |

```java
// 读属性值
AttributeInstance attr = player.getAttribute(LyraAttributeRegister.ServantMaxCount);
int max = attr != null ? (int) attr.getValue() : 0;
```

### 12.2 伤害类型（`LyraDamageRegister`）

```java
// 仆从伤害类型 key：lyra:servant（数据文件 src/main/resources/data/lyra/damage_type/servant.json）
ResourceKey<DamageType> key = LyraDamageRegister.Servant;

Holder<DamageType> holder = LyraDamageRegister.getDamageTypeHolder(key, level);
DamageSource source = LyraDamageRegister.getDamageSource(key, level);
```

---

## 13. 创造模式 Tab

Lyra 的分组 Tab 逻辑（横幅动画 + 按特征标签分组）：

```java
import first.lyra.common.creativeTab.CreativeTabDispatcher;
import first.lyra.common.creativeTab.Section;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.core.Holder;

// 1) 注册分段：特征标签 + 顺序 + 横幅贴图
Section section = new Section(0,
        Identifier.fromNamespaceAndPath("my_mod", "textures/banner.png"),
        null,   // AnimBanner 可选（Gecko 动画横幅）
        ItemTags.create(Identifier.fromNamespaceAndPath("my_mod", "weapons")));
CreativeTabDispatcher.registerSection(section);

// 2) 让宿主 Tab 接入 Lyra 分组逻辑
CreativeTabDispatcher.registerTab(tabHolder);

// 3) 物品只需带特征标签即自动归入分段
// Item.Properties().tag(MyTags.WEAPONS)  或 datagen 生成标签
```

---

## 14. 语言条目

```java
import first.lyra.register.LyraLanguageRegister;

// 运行时动态注册（覆盖可安全重复调用）
LyraLanguageRegister.entry("my.key", "English Text", "中文文本");
```

> 这些条目由 Lyra 的语言数据生成器输出。仅需注意：`LyraItemRegisterBuilder.language()` 系列方法**只在非生产环境生效**（`FMLLoader.isProduction()` 为 false 时写入），因为实际输出靠 datagen 跑 runData。

---

## 15. 数据生成接入

Lyra 的 `LyraDataGeneratorEvent` 已自动注册（`@EventBusSubscriber`），跑 `runData` 时输出：

- 语言：`LyraLanguageRegister` 全部条目 → `en_us` / `zh_cn`
- 物品模型：`LyraItemModelProvider`（收集自 `LyraItemRegisterBuilder.itemModel()`）
- 配方：`LyraRecipeProvider`（收集自 `LyraItemRegisterBuilder.recipe()`）
- 标签：`LyraItemTagsProvider`（收集自 `LyraItemRegisterBuilder.itemTag()`，护甲自动细分到原版护甲分类标签）

宿主物品注册 + datagen 一体化示例：

```java
import first.lyra.register.LyraItemRegisterBuilder;
import net.neoforged.neoforge.registries.DeferredRegister;

DeferredRegister.Items items = DeferredRegister.createItems("my_mod");

LyraItemRegisterBuilder.build(items, "my_item", () -> new Item(new Item.Properties()))
        .itemLanguage("My Item", "我的物品")                  // 物品名
        .itemLanguageTooltip(0, "Tooltip line", "提示行")     // tooltip 翻译
        .blockLanguage("My Block", "我的方块")                // 方块名
        .servantLanguage(myServantTypeHolder, "My Servant", "我的仆从") // 仆从名
        .recipe(output -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, myItem)
                .pattern("AAA").define('A', Items.IRON_INGOT)
                .unlockedBy("has_iron", has(Items.IRON_INGOT)).save(output))
        .itemTag(ItemTags.SWORDS)                              // 物品标签
        .itemModel(LyraItemRegisterBuilder::handheldItem)      // 手持模型
        .build();                                              // 返回 DeferredItem<T>
```

---

## 16. 工具类

### 16.1 AttributeUtils

```java
import first.lyra.utils.AttributeUtils;

// 条件成立则添加（不存在或值/操作变化时更新），不成立则移除
AttributeUtils.condition(player, Attributes.MOVEMENT_SPEED,
        Identifier.fromNamespaceAndPath("my_mod", "speed_buff"),
        0.05, AttributeModifier.Operation.ADD_VALUE, true);

// 直接增删（按修饰符 ID 幂等）
AttributeUtils.addAttributeModifier(entity, attr, id, amount, operation);
AttributeUtils.removeAttributeModifier(entity, attr, id);
```

### 16.2 EasingCurve

```java
import first.lyra.utils.EasingCurve;

// 预置：LINEAR / EASE_IN_QUAD / EASE_OUT_QUAD / EASE_IN_OUT_QUAD / CUBIC 系列 / BACK 系列 / BOUNCE / ELASTIC
float t = EasingCurve.EASE_OUT_QUAD.apply(0.5f);

// 贝塞尔构建（Y 可越界产生过冲/回弹）
EasingCurve ease = EasingCurve.bezier()
        .control(0.25f, 0.1f)
        .control(0.75f, 1.3f)   // 过冲
        .build();

// 组合
EasingCurve c = EasingCurve.EASE_IN_QUAD.compose(EasingCurve.EASE_OUT_BACK);
```

### 16.3 GroundPathHelper

地面寻路辅助（`first.lyra.utils.GroundPathHelper`），供仆从在地面上规划路径（路径节点 `PathNode` / 计划路径 `PlannedPath` 位于 `first.lyra.common.entity`）。

---

## 附录：宿主 mod 接入最小清单

1. **依赖**：Lyra `publishToMavenLocal` → 宿主 `implementation "first:lyra:1.0.0"`
2. **注册仆从类型**：`LyraRegistries.ATTACHMENT_ENTITY_TYPES.register(id, new AttachmentEntityType<>(MyServant::new))`
3. **注册武器**：`ServantWeaponItemBuilder` + `LyraItemRegisterBuilder.build()`（右键召唤/潜行遣散已自动处理）
4. **注册射弹**：继承 `Projectile`，`new MyProjectile(...)` 后 `join(player)`
5. **注册套装**：`ArmorSet.builder(id)...build()` → `LyraRegistries.ARMOR_SETS.register`
6. **其他**：`Playable.play`（音效）、`GenericParticleBuilder`（粒子）、`InvincibleData.attack()`（自定义无敌帧伤害）、`LyraLanguageRegister.entry`（语言）
7. **跑数据生成**：Gradle `runData`（Lyra 自动输出语言/配方/标签/模型）
