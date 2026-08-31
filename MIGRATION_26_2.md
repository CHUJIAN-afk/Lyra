# Lyra → NeoForge 26.2 迁移计划

## 动态光源空间查找 + 跨帧跟踪(26.2 已实现,向后迁移参照)

### 设计意图
旧实现按光源影响半径**预烘焙**(每光源写 ~3375 个 BlockPos,写入 O(半径³)),
或按区块分组(嵌套 Map + Vec3 装箱 + 每帧深拷贝快照),均导致写入/拷贝热点。
26.2 参考 LambDynamicLights 4.x 引擎改为**空间查找**:写入 O(1),查询 O(1) 定位邻居分组内光源实时算衰减;
区块刷新用**身份哈希跨帧跟踪**(O(1) 匹配,替代位移阈值判定)。

### 方案:16×16×16 三维区块分组 + 身份哈希跨帧跟踪
- **数据结构**:`LightSource[]` 按 sectionKey 排序 + `Long2IntOpenHashMap<sectionKey → 起始索引>`
  - `LightSource` record:id(身份哈希)+ sectionKey + (x, y, z) + luminance,原始类型无装箱
  - 当前帧累积 `ArrayList<LightSource>`(addLightSources 追加,update 消费后 clear 复用)
  - 编译线程快照 `Snapshot`(volatile **引用替换**,零拷贝,数组不可变)
- **写入**(`addLightSources(Vec3, int)`):O(1) 追加 1 条 record,**写入时计算身份哈希**
  `lightId = 位置(double 位模式)× 亮度混合(64-bit,碰撞走"位置不同→刷新"分支,安全)`
- **分组 key**:`SectionPos.asLong`(21 bit/轴:x<<42 | y<<21 | z,世界 ±30M 格 = 区块 ±1.9M < 2^21,无碰撞)
- **查询**(方块/实体路径):按查询点**块内位置裁剪邻居区块**——偏移 ≤6 查西/下/北邻居,≥9 查东/上/南邻居,
  [7,8] 只查自身(MAX_RADIUS 7.75 < 16,边界距离决定溢出方向),最多 9 个分组,平均 1-8 个(替代固定 27 cell);
  对分组内光源实时算 `luminance - sqrt(distSq) * (15/7.75)`,取最大,double 精度
- **区块刷新**(`update()`):跨帧对比 `Long2ObjectOpenHashMap<id → 上一帧光源>`:
  - 未命中(新增/大幅移动)→ 刷新
  - 命中但位置或亮度不同(移动)→ 刷新
  - 命中且相同(静止)→ 跳过
  - 旧缓存未被命中的(移除)→ 刷新
  - 区块编译频率**不做限制**(每帧执行差集;静止光源零刷新)
- **精度**(边缘过渡):提升时位运算 `(originalLight & 0xfff00000) | ((int)(level*16.0) & 0xfffff)`
  (Lamb 同款)——4-bit 读取等效 floor 安全,smooth(8-bit)读取保留 16 倍精度
- **挂载点**:方块路径注入 `LightCoordsUtil.BrightnessGetter.DEFAULT`(`lambda$static$0`,@WrapMethod **remap=false**),
  借助 BlockModelLighter.Cache 的 LRU(每方块每编译最多一次查询);
  实体路径注入 `EntityRenderer.getPackedLightCoords`(getDynamicLight(Vec3, int))

### 向后迁移注意事项
1. 分组边长必须满足 `MAX_RADIUS < 分组边长`,否则邻居裁剪覆盖不全(需扩大裁剪范围)
2. `SectionPos.asLong` 21 bit/轴在 26.2 无碰撞(旧版本布局不同,迁移时核对)
3. 快照必须整表重建 + 引用替换(编译线程只读,不可原地写)
4. 静止判定演进史(2026-08-13):位移大阈值(0.5 格)→ 帧间位移远小于 tick 间位移,
   持续移动光源永远"匹配"→ 永不刷新(踩坑);精确匹配 + 20Hz 节流 → 可行但结构冗余;
   最终:身份哈希跨帧跟踪(O(1) 匹配,任何位置/亮度变化必刷,静止必跳)
5. 若未来调用方引入光源 ID,可直接用 ID 替代位置哈希(当前位置哈希已可完全跟踪)
6. 无配置开关(默认启用);若恢复开关,在 addLightSources 加回判断

## 待修复 Bug 清单(迁移完成后处理)

### 🔴 1.21.1 分支:DynamicLightDispatcher.update() 区块累积 bug
- **位置**:1.21.1 分支(旧版本)的 `DynamicLightDispatcher.update()`
- **症状**:光源持续移动时,`LastUpdateSectionSet` 每帧累积历史区块,导致每帧刷新整个移动轨迹的区块,性能随移动距离劣化
- **根因**:`updateSectionSet` 从 `LastUpdateSectionSet` 拷贝后,末尾又 `addAll(updateSectionSet)` 写回——"上一帧区块"语义被破坏为"所有历史区块"
- **修复参考**:26.2 分支已修复(2026-08-12),`LastUpdateSectionSet` 改为只保存本帧光源所在区块;1.21.1 分支应用相同修复(当前 26.2 的修复见 `DynamicLightDispatcher.update()`)

### ✅ 26.2 动态光源方块路径挂载点(已解决)
- `BlockModelLighter.getLightCoords` 仅无 AO 分支调用,非实际入口
- 正确挂载:`LightCoordsUtil.BrightnessGetter.DEFAULT`(`lambda$static$0` @WrapMethod remap=false,参考 LambDynamicLights 26.2),所有方块光照(含 AO)都经过它且保留 vanilla 亮度缓存(LRU)

> 源码从 NeoForge 21.1(MC 1.21.1)迁移到 NeoForge 26.2(MC 1.26.2.0)。
> 本文件是迁移期间的总纲:环境基线、迁移顺序、API 差异清单(由三份子系统探索报告整合)。
> 每完成一个阶段,在本文件顶部勾选。

## 环境基线(已完成 ✅)

| 项 | 值 |
|---|---|
| Gradle | 9.7.0(wrapper) |
| JDK toolchain | 25(NeoForge 26.2 依赖仅兼容 Java 25+) |
| moddev 插件 | net.neoforged.moddev 2.0.143 |
| NeoForge | 26.2.0.59 |
| GeckoLib | com.geckolib:geckolib-neoforge-26.2:5.5.3 |
| run 类型 | data 已拆分为 clientData |
| 已修 mixin | CombatRulesMixin(新增 victim 参数)、LivingEntityMixin(hurtServer + dealDefaultKnockback/knockback 5 参) |

## 迁移阶段(按顺序执行)

- [ ] **阶段 1 纯改名** — EntityTypes.PLAYER、lookup/get、LightCoordsUtil、ARGB、ChunkPos.pack、getMinY/getMaxY、GeckoLib 包改名(import 层)
- [ ] **阶段 2 小签名变化** — ItemCooldowns(Item→ItemStack)、Unbreakable→Unit、I18n→Language、getArmorSlots→getItemBySlot、Vec3.STREAM_CODEC、hurt→hurtServer/hurtOrSimulate、ParticleProvider 加参、addParticle 加参、getNewDamage→getHealthDamage、AnimBanner.blit、VertexConsumer 新抽象方法、RegisterParticleProvidersEvent、RegisterClientReloadListenersEvent、Item.getDescription
- [ ] **阶段 3 数据生成重写** — GatherDataEvent Server/Client 拆分、RecipeProvider(Runner 模式)、ItemTags/BlockTagsProvider、ItemModelProvider→ModelProvider、SimpleJsonResourceReloadListener 泛型化(DamageInfoStyleManager)
- [ ] **阶段 4 护甲组件化** — ArmorItem/ArmorMaterial 移除 → Item + createAttributes(ArmorType) + Equippable、ArmorSet.getArmorSlots
- [ ] **阶段 5 渲染重构** — MultiBufferSource→SubmitNodeCollector、TrailRenderType→RenderSetup.builder(RenderPipelines)、LevelRendererMixin/Accessor 重新设计、ModelRenderer、DynamicLightDispatcher 挂接点
- [ ] **阶段 6 编译通过 + 冒烟** — compileJava 全绿,client 启动验证

---

# API 差异清单(1.21.1 → 26.2)

## 1. 全局改名

| 旧 | 新 | 备注 |
|---|---|---|
| `net.minecraft.resources.ResourceLocation` | `net.minecraft.resources.Identifier` | 部分文件已迁移 |
| `LightTexture.FULL_BRIGHT/block/sky/pack` | `net.minecraft.util.LightCoordsUtil.*` | LightTexture 类不存在 |
| `FastColor.ARGB32.*` | `net.minecraft.util.ARGB.*` | |
| `Camera.getPosition()` | `Camera.position()` | |
| `BlockAndTintGetter` | `BlockAndLightGetter` | |
| `SectionPos.getX()/getY()/getZ()` | `x()/y()/z()` | |
| `EntityType.PLAYER` | `EntityTypes.PLAYER`(net.minecraft.world.entity) | |
| `RegistryAccess.registry(key)` | `RegistryAccess.lookup(key)` | |
| `Registry.getHolder(key)` | `get(key)`(HolderGetter 接口) | |
| `ChunkPos.asLong(x, z)` | `ChunkPos.pack(x, z)` | |
| `getMinBuildHeight()/getMaxBuildHeight()` | `getMinY()/getMaxY()` | 注意 maxY 含上界语义 |
| `RenderType.lines()/entityTranslucent()` | `RenderTypes.lines()/entityTranslucent()`(renderer.rendertype 包) | RenderType 类移包 |
| `BakedQuad` | `net.minecraft.client.resources.model.geometry.BakedQuad` | 移包 |
| GeckoLib `software.bernie.geckolib.*` | `com.geckolib.*` | GeckoLib 5 换 group |
| `BlockState.isSolidRender(BlockAndTintGetter, BlockPos)` | `isSolidRender()` 零参 | |

## 2. 严重破坏(需重构)

### 2.1 渲染管线(最重)
- `MultiBufferSource` 类**删除** → `SubmitNodeCollector.submitCustomGeometry(PoseStack, RenderType, CustomGeometryRenderer)`;`CustomGeometryRenderer.render(PoseStack.Pose, VertexConsumer)`
  - 受影响:AlphaBufferSource、IAttachmentEntityRenderer、AbstractAttachmentEntityRenderer、TrailConfig 全套(Cone/Ribbon/Droplet)、Laser/Lightning/Sphere Helper、GeoSideloader、ModelRenderer
- `VertexConsumer` 接口新增抽象方法 `setColor(int)`、`setLineWidth(float)`;`misc(...)` 覆写删除
  - 受影响:AlphaBufferSource、TintedVertexConsumer
- 自定义 RenderType:`CompositeState`/`RenderStateShard` 体系删除 → `RenderType.create(name, RenderSetup)`;`RenderSetup.builder(RenderPipelines.XXX).withTexture(...)...createRenderSetup()`;`VertexFormat.builder().addAttribute(String, GpuFormat)`;`VertexFormat.Mode.QUADS` → `PrimitiveTopology.QUADS`
  - 受影响:TrailRenderType(不能继承 RenderType,改工厂函数)
- `ItemRenderer` 类删除 → `ItemModel` + `ItemStackRenderState`;`ModelManager.getModel(ModelIdentifier)` → `getItemModel(Identifier)`;`ModelIdentifier`/`BakedModel` 删除;`neoforge.client.model.data.ModelData` 包不存在 → `QuadInstance` 承载 per-vertex 数据
  - 受影响:ModelRenderer(整体失效,需重写)
- LevelRenderer 重构:`renderLevel` → `render(GraphicsResourceAllocator, DeltaTracker, boolean, CameraRenderState, Matrix4fc, GpuBufferSlice, Vector4f, boolean)`;`getLightColor` 移除;`setSectionDirty` 移除;`level` 字段移除(改 LevelRenderState);`RenderBuffers.bufferSource()` 移除
  - 受影响:LevelRendererMixin、LevelRendererAccessor(全部失效,重新设计挂接点,候选 `submitFeatures`/`submitEntities`)
  - DynamicLight 区块刷新需新机制(renderLineBox→gizmos:`net.minecraft.gizmos.Gizmos.cuboid/line`)

### 2.2 护甲组件化
- `ArmorItem`/`ArmorItem.Type`/`ArmorMaterial` 全部删除 → `net.minecraft.world.item.equipment.ArmorType` 枚举、`ArmorMaterial` record(新增 `createAttributes(ArmorType)`);护甲物品 = `Item` + `attributes(createAttributes(type))` + `Equippable` 组件(`Item.Properties.equippable(EquipmentSlot)`)
- `DataComponents.UNBREAKABLE` 类型 `Unbreakable`→`Unit`;`new Unbreakable(true)` → `Unit.INSTANCE`
- `LivingEntity.getArmorSlots()` 删除 → `getItemBySlot(EquipmentSlot)` 遍历 HEAD/CHEST/LEGS/FEET
  - 受影响:ArmorSet、AttributeArmorItemBuilder、TooltipHandler

### 2.3 数据生成
- `GatherDataEvent` 拆 `Server`/`Client`;`includeClient/includeServer/getExistingFileHelper` 删除;`ExistingFileHelper` 类删除(provider 构造删参,资源用 `event.getResourceManager(PackType)`)
- `RecipeProvider` 构造 `(PackOutput, CompletableFuture)` → `(HolderLookup.Provider, RecipeOutput)`;子类走嵌套 `Runner`(abstract);`buildRecipes()` 无参,写 `this.output`
- `ItemTagsProvider`:vanilla 拆分,用 `net.neoforged.neoforge.common.data.ItemTagsProvider(PackOutput, lookupProvider, modId)`;`IntrinsicTagAppender` 删除 → `TagAppender.add(ResourceKey)`(`item.builtInRegistryHolder().key()`)
- `ItemModelProvider` → vanilla `net.minecraft.client.data.models.ModelProvider`;`basicItem/handheldItem` → `ItemModelUtils.plainModel` + `ItemModelOutput.accept`
- `SimpleJsonResourceReloadListener(Gson, dir)` + 覆写 `apply(...)` → 泛型 codec 构造 `(Codec<T>, FileToIdConverter)` / `(HolderLookup.Provider, Codec<T>, ResourceKey)`;apply 移除
  - 受影响:DamageInfoStyleManager

### 2.4 其他破坏
- `LivingEntity.hurt(DamageSource, float)` 删除(Entity.hurt 变 void)→ `hurtServer(ServerLevel, ...)` 或 `hurtOrSimulate(...)`
  - 受影响:InvincibleData.AttackBuilder
- `FriendlyByteBuf.writeVec3/readVec3` → `Vec3.STREAM_CODEC`
  - 受影响:AttachmentEntity
- `ItemCooldowns.isOnCooldown/addCooldown` 参数 `Item` → `ItemStack`
  - 受影响:ServantWeaponItemBuilder
- `ParticleProvider.createParticle` 新增第 10 参 `RandomSource`
- `Level.addParticle(...)` 新增 `alwaysShow` 参数
- `LivingDamageEvent.Post.getNewDamage()` → `getHealthDamage()`/`getInflictedDamage()`
- `I18n.exists` → `Language.getInstance().has`;`Item.getDescription()` → `translatable(getDescriptionId())`
- `AnimBanner.blit` 9 参重载语义改变 → `blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, w, h, tw, th)`
- `RegisterClientReloadListenersEvent` 删除 → `Minecraft.getInstance().getResourceManager().registerReloadListener(...)`
- `RegisterParticleProvidersEvent.registerSpriteSet` 签名变(注册器接口换)

## 3. 已确认兼容(不动)

- mixin 注入点:tick、attack、hurtServer、dealDefaultKnockback、buildContents、extractRenderState、EffectsInInventory.extractRenderState、getPackedLightCoords(final 可包装)、getLightProbePosition
- 注册:DeferredRegister/DeferredHolder/DeferredItem、NewRegistryEvent/RegistryBuilder、AttachmentType、EntityAttributeModificationEvent(DeferredHolder 即 Holder)、RangedAttribute、Attribute.setSyncable
- 网络:CustomPacketPayload/StreamCodec/ByteBufCodecs/RegisterPayloadHandlersEvent/PacketDistributor/HandlerThread、RegistryFriendlyByteBuf
- 实体:DamageSource 4 参构造(可继承)、typeSupplier、DamageSources.generic、Vec3.offsetRandom、Mth 全套、getEntitiesOfClass、getBlockCollisions、Shapes.collide(List→Iterable 源码兼容)
- GUI:CreativeModeTab.buildContents/字段、selectedTab/scrollOffs、getLeftPos/getTopPos、TooltipFlag、ModConfigSpec、ConfigurationScreen、IConfigScreenFactory
- 事件:bust 8.0.5、EventBusSubscriber、LevelTickEvent、PlayerInteractEvent、LivingEquipmentChangeEvent(存在性待 NeoForge jar 终验)
