# Lyra 迁移状态文档(MIGRATION_STATUS)

> 本文件是 Servantry(仆从学)API 拆分为独立库 mod **Lyra** 的迁移工作备忘。
> 源项目:`D:\IDEA\Servantry`(保持不变,未改动)
> 目标项目:`D:\IDEA\Lyra`(本目录)

## 一、迁移目标与包映射规则

> ⚠️ 以下包结构已被第四轮(模块化重构)取代,当前结构见文末"第四轮"小节。

从 `Servantry` 的 `src/main/java/first/servantry/api/**` 及连带类迁移到 Lyra,
包名从 `first.servantry.api.*` → `first.lyra.*`,并且**只区分 common / client 两层**:

| 源(旧) | 目标(新) |
|---|---|
| `first.servantry.api.common.*` | `first.lyra.common.*` |
| `first.servantry.api.client.*` | `first.lyra.client.*` |
| `first.servantry.api.<其他子包>`(armorSet/builder/entity/servant/item/damageInfo/projectile) | `first.lyra.common.<子包>` |
| `first.servantry.api.ServantryHelper` | `first.lyra.api.LyraHelper`(根包,已改名) |
| `first.servantry.utils.*`(4 个工具类) | `first.lyra.common.utils.*` |
| `first.servantry.network.*`(2 个 payload) | `first.lyra.common.network.*` |
| `first.servantry.config.ClientConfig` | `first.lyra.client.config.ClientConfig` |
| `first.servantry.mixin.LevelRendererAccessor` | `first.lyra.mixin.LevelRendererAccessor` |
| `first.servantry.register.Servantry*Register`(4 个) | `first.lyra.register.Lyra*Register` |

**命名约定**:类名 `Servantry*` → `Lyra*`(如 `ServantryRegistries` → `LyraRegistries`)。

## 二、已完成 ✅(2026-08-07 最终确认)

1. **82 个 Java 文件迁移完成**,包名 `first.servantry` / 类名 `Servantry*` / 字符串 `servantry` 在 `src/` 下残留 = 0。
2. **修复了批量替换的 2 类误伤**:
   - package 声明/import 丢失 `common.`/`client.` 层级(约 68 文件,`api.<子包>` 被直接映射成 `lyra.<子包>`;`network`/`utils`/`config` 根级包同理)——已全部修正并校验 package-vs-路径一致。
   - 被正则损坏的文件:`LevelRendererAccessor.java`(`imaort`/`aublic`/`saongeaowered`)、`ClientConfig.java`(`imaort`/`aublic`/`ModConfigSaec`/`AlahaModify`)——已按源项目重写。(之前"6 个已修复文件"列表遗漏了这 2 个。)
3. **删除了重复文件** `register/ServantryRegistries.java`(旧版),保留用户手写的 `LyraRegistries.java`(已补 `AttachmentEntity`/`AttachmentEntityType` import、`Servantry.rl(` → `Lyra.rl(`)。`common/armorSet/ArmorSet.java` 为用户手写版,package/import 已统一。
4. **类名替换**(111 处):`ServantryAttachment/Attribute/Damage/ParticleRegister` → `Lyra*Register`、`ServantryRegistries` → `LyraRegistries`、`ServantryHelper` → `LyraHelper`(文件同步改名)、`Servantry.MODID`/`Servantry.rl(` → `Lyra.*`、`item.servantry.*` 语言键 → `item.lyra.*`、注释 `servantry:` → `lyra:`。
5. **Lyra.java**:补 `rl()` 方法(`MODID` 已是 `lyra`),构造函数注册了 Attachment/Attribute/Particle 三个 Register。
6. **build.gradle**:repositories 加 Illusive Soulworks + GeckoLib maven;dependencies 加 `curios-neoforge:9.5.1+1.21.1` + `geckolib-neoforge-1.21.1:4.8.4`。
7. **lyra.mixins.json**:mixins 数组已注册 `LevelRendererAccessor`(`first.lyra.mixin`);neoforge.mods.toml 模板已含 `[[mixins]]` 声明。
8. **3 个硬骨头已解耦**(见第三节)。
9. **资源文件**已复制到 `assets/lyra/`:`textures/trail.png`、`textures/particle/generic.png`、`textures/damage_font.png`、`particles/generic.json`、`damage_info/damage_info.json`(json 内 `servantry:` → `lyra:` 已改写),`data/lyra/damage_type/servant.json`。语言文件 `en_us.json`(库全部键)+ 新建 `zh_cn.json`。
10. **编译验证通过**:`gradlew compileJava`(JBR 21)→ **BUILD SUCCESSFUL**。

## 二点五、第二轮 ✅(2026-08-07:模块入口补全 + 创造横幅 + 语言数据生成)

从主 mod 迁移/新建,让库的既有模块全部有入口:

1. **`LyraHelper` 移到 `first.lyra.api` 包**(API 外观层,引用方已全部更新)。
2. **创造模式分类横幅渲染**(主 mod 原样迁移,库不注册任何 Tab):
   - `client/creativeTab/AnimInfo.java`(动画横幅 blit)
   - `register/TabGroup.java`(order/texture/animBanner)
   - `register/LyraCreativeTabRegister.java`(`TabBuilder` 分组 + `renderBanners`/`processItems` + `registerTab(Holder)` 登记外部 Tab + `isManaged(tab)` 判断)
   - `mixin/CreativeModeTabMixin`(buildContents 覆写,注入已登记 Tab 的物品展示)+ `mixin/CreativeModeInventoryScreenMixin`(渲染横幅);两个 mixin 均走 `isManaged`,未登记的 Tab 完全不受影响
3. **渲染入口 mixin**(动态光照/附件渲染/伤害数字此前无调用方):
   - `mixin/LevelRendererMixin`(方块动态光照 `getLightColor` + `renderLevel` 内调用 `AttachmentEntityRenderDispatcher.render`/`DamageInfoRenderDispatcher.render` + TAIL 调用 `DynamicLightDispatcher.update`)
   - `mixin/EntityRendererMixin`(实体动态光照 `getPackedLightCoords`)
   - `lyra.mixins.json`:`mixins` 加 `CreativeModeTabMixin`(LevelRendererAccessor 照主 mod 留 mixins),`client` 加 `CreativeModeInventoryScreenMixin`/`EntityRendererMixin`/`LevelRendererMixin`
4. **网络入口**:新建 `register/LyraNetworkPacketRegister.java`,在 `lyra` 通道注册 `BatchedParticlesPayload`/`BatchedDamageInfoPayload`(此前两个 payload 无注册方,直接不可用)。
5. **配置入口**:`Lyra.java` 构造器改 `(IEventBus, Dist, ModContainer)`,注册 `LyraCreativeTabRegister`/`LyraItemRegisterBuilder`/`LyraNetworkPacketRegister`,并 `registerConfig(CLIENT, ClientConfig.Spec)` + `ConfigurationScreen`。
6. **`register/LyraItemRegisterBuilder.java`**(迁移 `ServantryItemRegisterBuilder`,语言数据生成,库不注册物品):仅保留外接版 `build(DeferredRegister.Items, TabGroup, String, ...)` 重载,宿主 mod 传入自己的 DeferredRegister 注册物品;链式配置 `language`/`itemLanguage`/`itemLanguageTooltip`/`blockLanguage`/`servantLanguage` 写入 `LyraLanguageRegister`(带 `!FMLLoader.isProduction()` 判断)。recipe/itemTag/itemModel 依赖主 mod datagen provider,未迁移(主 mod 自行扩展)。
7. **语言文件**补 `itemGroup.lyra`、`lyra.configuration.*`(title/section/4 开关+tooltip)。
8. 编译验证通过(JBR 21)。

**主 mod 对接要点**:库不注册任何 CreativeModeTab/物品。主 mod 注册自己的 Tab 后调用 `LyraCreativeTabRegister.registerTab(tabHolder)` 接入横幅渲染与分组物品展示;物品用 `LyraItemRegisterBuilder.build(自己的DeferredRegister.Items, tabGroup, ...)` 注册;横幅贴图纹理由主 mod 提供(自己命名空间)。

## 二点六、第三轮 ✅(2026-08-07:数据生成集成)

`LyraItemRegisterBuilder` 的数据生成链式方法完整恢复,datagen 基础设施迁入库:

1. **`dataGenerator/provider/` 4 个 provider**(主 mod 原样迁移):
   - `LyraLanguageProvider`(en_us/zh_cn,遍历 `LyraLanguageRegister`;zh_cn 输出后清空条目)
   - `LyraRecipeProvider`(`RecipeGenerate` 静态收集器)
   - `LyraItemTagsProvider`(`ItemTagsGenerate`,护甲自动细分 HEAD/CHEST/LEG/FEET 标签)+ 空 `LyraBlockTagsProvider` 作依赖
   - `LyraItemModelProvider`(`ItemModelGenerate`)
2. **`dataGenerator/LyraDataGeneratorEvent`**:显式 `eventBus.addListener` 挂载到 Lyra.java(不走 @EventBusSubscriber,避免默认 bus 歧义);`Lyra.java` 构造器已接线。
3. **静态语言键改由 datagen 输出**:`LyraLanguageRegister.init()` 注册库的静态键(tooltip/属性/死亡消息/配置),删除手动 `assets/lyra/lang/en_us.json`+`zh_cn.json`(避免与生成的 lyra lang 同路径冲突)。`runData` 已验证,生成的 `src/generated/resources/assets/lyra/lang/*.json` 含全部 29 键(已提交)。
4. **`LyraItemRegisterBuilder`** 恢复 `recipe()`/`itemTag()`/`itemModel()`/`basicModel()`/`handheldItem()`,均带 `!FMLLoader.isProduction()` 判断。
5. 编译 + `gradlew runData`(JBR 21)均 BUILD SUCCESSFUL。

**宿主对接**:物品注册链式 `.recipe(...)`/`.itemTag(...)`/`.itemModel(LyraItemRegisterBuilder::basicModel)` 后,宿主跑 runData 即输出到其 datagen 目录。

## 二点七、第四轮 ❌(2026-08-07:模块化包结构重构 —— 已回退)

包结构曾重构为**按模块分包**(每个功能模块一个包,模块下分 client/common/register),并先后将 entity/render、servant/projectile 并入 `attachmentEntity` 核心模块。**用户评估后决定回退**:模块化结构不如开始的 common/client 双主包,已改回第一轮结构(见第一节,恢复为当前有效)。

**回退时保留的用户功能决策**(与包结构无关):
- `AnimInfo` → `client/creativeTab/AnimBanner.java`(用户改名)
- 横幅渲染与物品展示逻辑内联进 `CreativeModeInventoryScreenMixin`/`CreativeModeTabMixin`(`LyraCreativeTabRegister` 仅保留 registerTab/isManaged/sortedTabGroup/TabBuilder)
- `SphereRenderer` → `SphereRendererHelper`(用户改名,`client/render/sphere/`)
- `ServantGoal`/`ServantGoalSelector` 扁平化(无 `ai` 子包,直接 `common/servant/`)
- 创造 Tab/物品不注册、datagen 集成、`LyraHelper` 在 `api` 包等全部保留

**当前包结构(有效)**:`first.lyra.common.*`(armorSet/attachment/builder/damageInfo/entity/item/network/particle/genericParticle/projectile/servant/sound/utils + Event)、`first.lyra.client.*`(config/creativeTab/dynamicLight/geo/render/renderType/tooltip + ClientEvent)、`first.lyra.register.*`(8 个 Lyra*Register + TabGroup + LyraItemRegisterBuilder + LyraLanguageRegister)、`first.lyra.api.LyraHelper`、`first.lyra.mixin.*`(5 个)、`first.lyra.dataGenerator.*`。编译 BUILD SUCCESSFUL。

## 二点八、第五轮 ✅(2026-08-07:创造分类改为特征标签自动归类)

- **`TabGroup` → `Section`**(`register/Section.java`):record 增加特征标签字段 `TagKey<Item> tag`;`order`/`texture`/`animBanner` 保留。
- **自动分类**:物品带 Section 特征标签即自动归入该分段,无需在注册时指定。`LyraItemRegisterBuilder.build` 的 `Section` 参数已移除(签名变 `build(DeferredRegister.Items, String name, ...)`),物品带标签方式:物品 `Properties.tag(tagKey)` 或 datagen 物品标签。
- **`LyraCreativeTabRegister`**:`TabBuilder`(Map)移除,改 `Sections` 列表 + `registerSection(Section)`;新增 `itemsOf(Section)`(遍历 `BuiltInRegistries.ITEM` 按 `builtInRegistryHolder().is(tag)` 过滤);`sortedTabGroup()` → `sortedSections()`。
- 两个 mixin 内联逻辑改走 `sortedSections()` + `itemsOf()`,补空格/行数逻辑不变。
- 编译 BUILD SUCCESSFUL。

**宿主用法**:`LyraCreativeTabRegister.registerSection(new Section(0, banner, anim, Tags.MY_SECTION));` + 物品注册时 `Properties.tag(Tags.MY_SECTION)`。

### 第五轮补充(2026-08-07:API 门面分层)

- `register/LyraCreativeTabRegister` 移除,改为 **`common/creativeTab/CreativeTabDispatcher`**(调度类,逻辑不变:Sections/ManagedTabs/isManaged/sortedSections/itemsOf);`Section` 随迁至 `common/creativeTab`。
- **`api/LyraCreativeTabs`**(API 门面,新建):`registerTab(Holder<CreativeModeTab>)` / `registerSection(Section)`,委托给调度类。宿主只接触 api 门面。
- 两个 mixin 改引用 `CreativeTabDispatcher`。编译 BUILD SUCCESSFUL。

### 第五轮补充(2026-08-07:ArmorSet 缓存迁移附件 —— 精简版)

- **`common/attachment/ArmorSetData`**:公开字段 `Map<ArmorSet, Boolean> activeSets`(玩家级套装状态表,装备变化事件维护)。
- **`ArmorSet`**:逻辑形态完全复刻原静态缓存,仅存储位置从静态 `CACHE`(WeakHashMap)改为 `player.getData(LyraAttachmentRegister.ArmorSetData).activeSets`:`full(Player)` 用 `activeSets.computeIfAbsent` 实时扫描装备槽;`handler` 取出附件缓存对比触发 onStart/onRemove 后清空重算(原逻辑天然无时序问题,无额外复杂度)。
- **`LyraAttachmentRegister`** 注册 `armor_set_data` 附件(不同步)。
- 编译 BUILD SUCCESSFUL。

## 二点九、第六轮 ✅(2026-08-07:attachment 入口补全 + tick 统一命名)

- **方法统一为 tick(仅 tick 驱动类)**:`TargetCache.update(Player)` → `tick(Player)`、`ParticlesData.handler(LevelTickEvent.Post)` → `tick`、`DamageInfoData.handler(LevelTickEvent.Post)` → `tick`;`common/Event.java` 调用点同步。伤害事件回调(LivingDamageEvent.Post)保留 handler 名,`HealthData.healthRegenTick`/`DamageInfoData.tick()` 等不变。
- **新增 3 个 mixin(参考原项目完整迁移,补无入口类)**:
  - `LivingEntityMixin`:`LivingEntity.tick` TAIL → `InvincibleData.tick()`;`hurt` 仆从伤害/击退缩放(基于 ServantDamageSource + LyraAttributeRegister)
  - `PlayerMixin`:`Player.tick` TAIL → `TargetCache.tick(player)`(服务端)+ `AttachmentEntityData.tick(player)`;`attack` 暴击标记 `lyra$setCritical`(IDamageSourceCritical)
  - `CombatRulesMixin`:`getDamageAfterAbsorb` 护甲穿透(Servant.getArmorPierce + ServantArmorPierce 属性)
  - 均注册进 `lyra.mixins.json` 的 `mixins` 数组(common 目标类)。
- 持久记忆已写入:歧义指令先提问、不做额外扩展、谨慎新增方法体。
- 编译 BUILD SUCCESSFUL。

## 三、硬骨头解耦方案(已完成)

1. **`IServantWeaponItem`**:`INFINITE_SHADOW` 特判 + `ScabbardContainer`/`ServantryDataComponentRegister`(主 mod 内容)已删除,改为接口 default 方法钩子:
   ```java
   default Component getSummonTooltip(ItemStack itemStack, AttachmentEntityType<?> type, ResourceLocation location, Player player)
   ```
   默认显示仆从类型名翻译。**主 mod 的无限阴影武器需覆写此方法**显示剑鞘内物品名。
2. **`ArmorSetBuilder.tooltip()`**:不再调用主 mod 的 `ServantryLanguageGenerateRegister`,改走 Lyra 内建的 **`LyraLanguageRegister`**(`register` 包,静态 `Map<String, String[]>`)。键格式 `lyra.<ns>.<path>.set.<index>`,与 `TooltipHandler` 读取一致。**主 mod 的语言数据生成器需遍历 `LyraLanguageRegister.LanguageGenerate`** 输出翻译。
3. **`IDamageSourceCritical`**:接口方法已改为 `lyra$isCritical()` / `lyra$setCritical()`。**主 mod 的 `DamageSourceMixin` 需同步改为 `lyra$` 前缀**(当前仍是 `servantry$`,对接 Lyra 时必须改,否则接口实现不匹配)。

## 四、未迁移的(留在 Servantry,正确)

- `register/ServantryAttachmentEntityRegister`(30+ 仆从实体,主 mod 内容本体)
- `register/ServantryDataComponentRegister`(剑鞘组件)+ `common/dataComponent/ScabbardContainer`
- `register/ServantryLanguageGenerateRegister`(语言表,主 mod datagen 消费端;Lyra 侧对应 `LyraLanguageRegister`)
- `register/ServantryNetworkPacketRegister` + `network/MithrilAnvilPlaceRecipePayload`(密银砧玩法)
- `utils/Ballistics`、`utils/ParticleHelper`、`utils/RenderUtil`(仅主代码用)
- `config`、`mixin` 其余部分(主 mod 自己的)
- 主 mod 声音、实体模型、物品纹理等资源(主 mod 自己的)

## 五、主 mod 对接 Lyra 时的待办(将来)

- [ ] 主 mod 的 `DamageSourceMixin`:`servantry$` → `lyra$`(与 `IDamageSourceCritical` 一致)
- [ ] 无限阴影武器类覆写 `IServantWeaponItem.getSummonTooltip(...)` 恢复剑鞘 tooltip
- [ ] 主 mod datagen 语言 Provider 遍历 `LyraLanguageRegister.LanguageGenerate` 输出套装奖励键
- [ ] 主 mod 依赖声明改为 `implementation project(":Lyra")` 或 maven 坐标,删除本地 `api` 副本
- [ ] 主 mod 的 `item.servantry.tooltip.*` 键按 `item.lyra.*` 迁移(或保留旧键由主 mod 覆盖)

## 六、关键决策(已拍板)

1. **命名空间 = `lyra:`**(与 `MODID = "lyra"` 一致,资源已复制到 `assets/lyra/`)
2. **`ServantryHelper` → `LyraHelper`**(已改名)
3. **mixin 前缀 → `lyra$`**(主 mod 对接时同步改 `DamageSourceMixin`)

## 七、编译命令

```powershell
# 系统 PATH 的 java 是 25,Gradle 8.8 不支持,必须用 JBR 21:
.\gradlew.bat compileJava "-Dorg.gradle.java.home=C:\Users\l1518\AppData\Local\Programs\IntelliJ IDEA Ultimate 2025.2.5\jbr" --console=plain
```
