# Lyra → NeoForge 26.2 迁移计划

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
- 实体:DamageSource 4 参构造(可继承)、typeHolder、DamageSources.generic、Vec3.offsetRandom、Mth 全套、getEntitiesOfClass、getBlockCollisions、Shapes.collide(List→Iterable 源码兼容)
- GUI:CreativeModeTab.buildContents/字段、selectedTab/scrollOffs、getLeftPos/getTopPos、TooltipFlag、ModConfigSpec、ConfigurationScreen、IConfigScreenFactory
- 事件:bust 8.0.5、EventBusSubscriber、LevelTickEvent、PlayerInteractEvent、LivingEquipmentChangeEvent(存在性待 NeoForge jar 终验)
