# Lyra API 改动日志

## 当前开发版 — 模型渲染器模块化（破坏性变更）

模型渲染统一改为 `LyraModelRenderer`，并按格式拆分为互不复用的独立模块：

| 旧入口 | 新入口 |
|---|---|
| `RenderUtil.renderStandalone(...)` | `LyraModelRenderer.json(...).color(...).light(...).render(...)` |
| `AnimatedModelRenderer.request(...)` | `LyraModelRenderer.geo(...)` |
| `VirtualEntityRenderer.render(..., pose)` | `LyraModelRenderer.virtualEntity(...).pose(...).render(...)` |

新增 `LyraModelRenderer.bbmodel(...)`，直接加载
`assets/<mod>/lyra_model/bbmodel/<id>.bbmodel`，支持
BBModel 内嵌贴图、骨骼关键帧、循环播放和隐藏骨骼。旧 `client.geo`、`client.render.animated`
与 `client.render.virtual` 包中的入口已移除。

渲染资源目录统一到 `assets/<mod>/lyra_model/`：Geo 使用
`geo/<model_id>/<id>.geo.json|animation.json|png`，原版 JSON 使用
`json/<model_id>/<id>.json|png`，BBModel 使用 `bbmodel/<id>.bbmodel`，
广告牌贴图使用 `textures/<id>.png`。

## 1.21.1.4 — 通用属性命名：Minion* → Summon*

仆从与哨兵共用的通用属性从 minion 前缀改为 Summon（召唤伤害等），与 `LyraDamageRegister.Summon`、`summon.` 语言键统一。各类型的专属计数属性保留 minion/sentry 命名。

| 旧（1.21.1.3） | 新（1.21.1.4） | 注册 ID |
|---|---|---|
| `LyraAttributeRegister.MinionDamage` | `SummonDamage` | `minion_damage` → `summon_damage` |
| `LyraAttributeRegister.MinionKnockback` | `SummonKnockback` | `minion_knockback` → `summon_knockback` |
| `LyraAttributeRegister.MinionArmorPierce` | `SummonArmorPierce` | `minion_armor_pierce` → `summon_armor_pierce` |
| `LyraAttributeRegister.MinionSearchRange` | `SummonSearchRange` | `minion_search_range` → `summon_search_range` |
| `IMinionWeaponItem.getMinionDamage` | `getSummonDamage` | — |
| `IMinionWeaponItem.getMinionKnockback` | `getSummonKnockback` | — |
| `IMinionWeaponItem.getMinionArmorPierce` | `getSummonArmorPierce` | — |
| `LyraAttributeRegister.SentryMaxCount` 注册 ID | 不变 | `sentry__max_count` → `sentry_max_count`（修正双下划线） |

**保持不变的命名**：`MinionMaxCount`/`SentryMaxCount`（各类型专属计数）、`Minion` 实体类、`AttachmentEntityDamageSource`、`AttachmentEntityData.Type.Minion/Sentry`、`MinionWeaponItemBuilder`。

> 注意：注册 ID 变化（`minion_damage` → `summon_damage` 等）会重置旧存档中的属性修饰符（由装备/药水重新应用）。

## 1.21.1.3 — 召唤命名：damage type → Summon、语言键 → summon.

承 1.21.1.2 的术语重构（servant→minion），本轮将"召唤"概念从 minion 收拢为 Summon——因为召唤伤害与召唤物名称同时覆盖仆从（minion）和哨兵（sentry），minion 语义过窄。

| 旧（1.21.1.2） | 新（1.21.1.3） |
|---|---|
| `LyraDamageRegister.Minion`（`lyra:minion`） | `LyraDamageRegister.Summon`（`lyra:summon`） |
| `LyraItemRegisterBuilder.minionLanguage(...)` | `LyraItemRegisterBuilder.summonLanguage(...)` |
| 实体名键前缀 `minion.<mod>.<path>` | `summon.<mod>.<path>` |
| `death.attack.lyra.minion(.player)` | `death.attack.lyra.summon(.player)` |
| 资源 `data/lyra/damage_type/minion.json` | `data/lyra/damage_type/summon.json` |

`Minion` 实体类、`AttachmentEntityData.Type.Minion/Sentry`、`MinionMaxCount` 等属性**保持 minion/sentry 命名不变**（它们是实体类别，不是召唤伤害概念）。

迁移示例：

```java
// 旧（1.21.1.2）
LyraDamageRegister.Minion
weapon.minionLanguage(HORNET, "Hornet", "黄蜂");

// 新（1.21.1.3）
LyraDamageRegister.Summon
weapon.summonLanguage(HORNET, "Hornet", "黄蜂");
```

## 1.21.1.2 — 术语重命名：servant → minion、sentryServant → sentry

对齐泰拉瑞亚官方术语（Minion 仆从 / Sentry 哨兵），全量重命名 API 中的 servant 术语。
**破坏性变更**：1.21.1.1 及更早版本的所有 servant 相关 API 均已更名，依赖方需按本表迁移。

### 包

| 旧 | 新 |
|---|---|
| `first.lyra.common.servant` | `first.lyra.common.minion` |

### 类 / 接口

| 旧 | 新 |
|---|---|
| `Servant` | `Minion` |
| `ServantDamageSource` | `AttachmentEntityDamageSource` |
| `ServantGoal` | `AttachmentEntityGoal` |
| `ServantGoalSelector` | `AttachmentEntityGoalSelector` |
| `MomentumServant` | `MomentumMinion` |
| `PathfinderServant` | `PathfinderMinion` |
| `PathNavigator` | 类名不变（仅换包） |
| `IServantWeaponItem` | `IMinionWeaponItem` |
| `ServantWeaponItemBuilder` | `MinionWeaponItemBuilder` |
| `ServantWeaponItemBuilder.ServantWeaponItem`（内部类） | `MinionWeaponItemBuilder.MinionWeaponItem` |

### 枚举 `AttachmentEntityData.Type`

| 旧 | 新 |
|---|---|
| `Type.Servant` | `Type.Minion` |
| `Type.SentryServant` | `Type.Sentry` |
| `Type.ExtraServant` | `Type.ExtraMinion` |

### 属性 `LyraAttributeRegister`（字段 + 注册 ID）

| 旧字段 | 新字段 | 注册 ID |
|---|---|---|
| `ServantMaxCount` | `MinionMaxCount` | `servant_max_count` → `minion_max_count` |
| `SentryServantMaxCount` | `SentryMaxCount` | `sentry_servant_max_count` → `sentry_max_count` |
| `ServantDamage` | `MinionDamage` | `servant_damage` → `minion_damage` |
| `ServantKnockback` | `MinionKnockback` | `servant_knockback` → `minion_knockback` |
| `ServantArmorPierce` | `MinionArmorPierce` | `servant_armor_pierce` → `minion_armor_pierce` |
| `ServantSearchRange` | `MinionSearchRange` | `servant_search_range` → `minion_search_range` |

> 注意：注册 ID 变化会重置旧存档中的玩家属性修饰符（由装备/药水重新应用）。

### 伤害类型

| 旧 | 新 |
|---|---|
| `LyraDamageRegister.Servant`（`lyra:servant`） | `LyraDamageRegister.Minion`（`lyra:minion`） |

资源：`data/lyra/damage_type/servant.json` → `data/lyra/damage_type/minion.json`

### 方法

| 位置 | 旧 | 新 |
|---|---|---|
| `IMinionWeaponItem` | `createServant(...)` | `createMinion(...)` |
| `IMinionWeaponItem` | `getServantDamage(...)` | `getMinionDamage(...)` |
| `IMinionWeaponItem` | `getServantKnockback(...)` | `getMinionKnockback(...)` |
| `IMinionWeaponItem` | `getServantArmorPierce(...)` | `getMinionArmorPierce(...)` |
| `MinionWeaponItemBuilder` | `sentryServant()` | `sentry()` |
| `LyraItemRegisterBuilder` | `servantLanguage(...)` | `minionLanguage(...)` |
| `LyraHelper` | 内部 servant 相关方法/变量 | 对应 minion 命名 |

### 语言键

| 旧 | 新 |
|---|---|
| `servant.<mod>.<path>`（实体名键前缀） | `minion.<mod>.<path>` |
| `item.lyra.tooltip.servant_slots` | `item.lyra.tooltip.minion_slots` |
| `item.lyra.tooltip.sentry_servant_slots` | `item.lyra.tooltip.sentry_slots` |
| `death.attack.lyra.servant` | `death.attack.lyra.minion` |
| `death.attack.lyra.servant.player` | `death.attack.lyra.minion.player` |

### 迁移示例

```java
// 旧（1.21.1.1）
import first.lyra.common.servant.Servant;
ServantWeaponItemBuilder<Hornet> builder = new ServantWeaponItemBuilder<>(HORNET);
AttachmentEntityData.Type.Servant
LyraAttributeRegister.ServantMaxCount
weapon.createServant(player, stack);

// 新（1.21.1.2）
import first.lyra.common.minion.Minion;
MinionWeaponItemBuilder<Hornet> builder = new MinionWeaponItemBuilder<>(HORNET);
AttachmentEntityData.Type.Minion
LyraAttributeRegister.MinionMaxCount
weapon.createMinion(player, stack);
```

### 其他分支同步指南

1. 按上述映射表执行全量替换（替换顺序敏感：先 `sentryServant`→`sentry`、`SentryServant`→`Sentry`、`sentry_servant`→`sentry_`、`Sentry Servant`→`Sentry`，再 `servant`→`minion`、`Servant`→`Minion`）
2. 移动 `first/lyra/common/servant` 包目录为 `common/minion`
3. 类文件名 `Servant*.java` → `Minion*.java`
4. 资源 `data/lyra/damage_type/servant.json` → `minion.json`
5. 重新运行数据生成（语言键随代码自动更新）
6. 中文翻译保留"仆从"（泰拉瑞亚官方 minion 译名），"哨戒仆从" → "哨兵"
