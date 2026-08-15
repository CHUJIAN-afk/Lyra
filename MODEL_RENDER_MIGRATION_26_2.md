# 26.2 模型渲染迁移开发经验

> 基于 Summoner 模组 1.21.1 → 26.2 迁移实战整理。目标：将渲染基础设施迁移到 Lyra 时，
> 按 26.2 的 RenderState 模式重新设计渲染管线，并评估 Java 模型迁移以获取性能收益。

---

## 1. 26.2 渲染架构总览：从"即时绘制"到"RenderState + 提交链"

1.21.1 的渲染是**即时模式**：渲染器在渲染回调里拿到 `MultiBufferSource`，调
`bufferSource.getBuffer(renderType)` 拿 `VertexConsumer` 立即写顶点。

26.2 改为**两阶段提交**：

```
extractRenderState(entity, state, partialTick)   // 阶段一：逻辑状态 → 渲染状态(可缓存)
        ↓
submit(state, poseStack, collector, camera)      // 阶段二：用状态构建提交节点
        ↓
SubmitNodeCollection.submitXxx(...)              // 提交链：按透明度/类型分派到 FeatureRenderer
        ↓
FeatureRenderer.buildGroup(...) → putBakedQuad   // 最终顶点写入
```

**核心类**：
- `SubmitNodeCollector`（接口）/ `SubmitNodeCollection`（实现）—— 提交入口，替代 `MultiBufferSource`
- `EntityRenderState` / `BaseRenderState`（neoforge 扩展）—— 渲染状态载体，可挂扩展数据
  （`IRenderStateExtension.getRenderData/setRenderData(ContextKey)`）
- `FeatureRenderer` 体系（`ModelFeatureRenderer`/`BlockModelFeatureRenderer`/`ItemFeatureRenderer`/
  `CustomFeatureRenderer`/`TextFeatureRenderer`/`NameTagFeatureRenderer`/`MovingBlockFeatureRenderer`）
- `FeatureRenderPhase`（`SimpleFeatureRenderPhase`/`TranslucentFeatureRenderPhase`）—— 提交分派（solid/translucent）

---

## 2. 渲染 API 改动对照表（1.21.1 → 26.2）

### 2.1 渲染入口 / 缓冲源

| 1.21.1 | 26.2 |
|---|---|
| `MultiBufferSource` + `bufferSource.getBuffer(RenderType)` | `SubmitNodeCollector` + `submitXxx(...)` |
| `VertexConsumer` 手写顶点链 | `consumer.putBakedQuad(pose, quad, QuadInstance)`（default 方法，内部 11 参 addVertex） |
| `ItemRenderer.renderStatic(...)` | `ItemStackRenderState.submit(poseStack, collector, light, overlay, outline)` |
| `ModelRenderer.renderModel(model, poseStack, bufferSource)` | `submitItem`（物品模型）/ `submitBlockModel`（方块模型） |

### 2.2 SubmitNodeCollection 提交方法（RenderState 提交链）

| 方法 | FeatureRenderer | 适用 |
|---|---|---|
| `submitModel(Model, state, poseStack, RenderType, light, overlay, tintedColor, sprite, outline, crumbling)` | ModelFeatureRenderer | **Java Model 类**（LlamaSpitModel 等） |
| `submitBlockModel(poseStack, RenderType, List<BlockStateModelPart>, tintLayers, light, overlay, outline)` | BlockModelFeatureRenderer | JSON 方块模型（单 RenderType 提交） |
| `submitMultiLayerBlockModel(poseStack, List<BlockStateModelPart>, translucent, tintLayers, light, overlay, outline)` | ExtendedBlockModelFeatureRenderer（neoforge） | 方块模型多层自动分派（**注意：管线固定 block item sheet**） |
| `submitItem(poseStack, ItemDisplayContext, light, overlay, outline, tintLayers, List<BakedQuad>, FoilType)` | ItemFeatureRenderer | 物品模型 / 任意 quads（**自动按 quad 的 itemRenderType 分派**，多图层正确） |
| `submitCustomGeometry(poseStack, RenderType, CustomGeometryRenderer)` | CustomFeatureRenderer | 自定义几何（染色包装等） |
| `submitText(...)` / `submitNameTag(...)` / `submitFlame(...)` / `submitLeash(...)` / `submitShadow(...)` | 对应 FeatureRenderer | 文字 / 名称标签 / 火焰 / 拴绳 / 阴影 |
| `submitMovingBlock(poseStack, MovingBlockRenderState, outline)` | MovingBlockFeatureRenderer | 移动方块（下落方块/活塞方块） |

**tintLayers 机制**（`BlockModelFeatureRenderer.putQuad` / `ItemFeatureRenderer.getLayerColorSafe`）：
```java
int tintIndex = quad.materialInfo().tintIndex();
boolean useTintLayer = tintIndex != -1 && tintIndex < tintLayers.length;
instance.setColor(useTintLayer ? ARGB.multiply(baseTintColor, tintLayers[tintIndex]) : baseTintColor);
```
⚠️ **tintLayers 只对带 tintIndex 的 quad 生效**（`material.isTinted()`）。`item/generated` 自动 quad
无 tintIndex → tintLayers 染色无效 → **必须用 `submitCustomGeometry` + VertexConsumer 包装器染色**。

### 2.3 顶点写入（putBakedQuad 内部路径）

```java
// VertexConsumer.putBakedQuad(PoseStack.Pose, BakedQuad, QuadInstance) 的 default 实现:
for (int vertex = 0; vertex < 4; vertex++) {
    int vertexColor = ARGB.multiply(instance.getColor(vertex), quad.bakedColors().color(vertex));
    this.addVertex(x, y, z, vertexColor, u, v, instance.overlayCoords(), light, nx, ny, nz);  // 11 参
}
// 11 参 addVertex → this.addVertex(x,y,z); this.setColor(color); this.setUv(u,v); ...
```
⚠️ **`setColor(int)` 单参版是必经路径**：包装器（如 `TintedVertexConsumer`）若只覆写四参
`setColor(r,g,b,a)` 而单参版透传，**染色必然失效**（Lyra 26.2.6 已修复）。

`QuadInstance`：`setLightCoords/setOverlayCoords/setColor`（-1 = 不染色，白色）。

### 2.4 RenderType 体系

| 1.21.1 | 26.2 |
|---|---|
| `RenderType` 抽象类 + `CompositeState` | `RenderType` **接口** + `RenderTypes` 工厂（`entityTranslucent(texture)` 等） |
| `RenderType.entityTranslucent(Identifier)` | `RenderTypes.entityTranslucent(Identifier)` |
| — | `RenderSetup.builder(RenderPipelines.XXX).withTexture("Sampler0", id).useLightmap().useOverlay().createRenderSetup()`（自定义管线） |
| — | `RenderPipelines`（`GUI_TEXTURED`/`ENTITY_TRANSLUCENT_EMISSIVE`/`ITEM_UNLIT` 等） |

自定义 RenderType 示例（Lyra TrailRenderType）：
```java
RenderType.create("lyra_trail",
    RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE)
        .withTexture("Sampler0", Lyra.id("textures/trail.png"))
        .useLightmap().useOverlay().sortOnUpload()
        .createRenderSetup());
```

### 2.5 BakedQuad / 模型烘焙

| 1.21.1 | 26.2 |
|---|---|
| `net.minecraft.client.renderer.block.model.BakedQuad` | `net.minecraft.client.resources.model.geometry.BakedQuad`（record） |
| `model.getQuads(state, side, random)` / `quad.getSprite()` | `quad.position(i)`/`quad.packedUV(i)`/`quad.direction()`/`quad.materialInfo()` |
| — | `BakedQuad.MaterialInfo(sprite, layer, itemRenderType, tintIndex, shade, lightEmission, ambientOcclusion)` |
| `ModelManager.getModel(ModelResourceLocation)` | `ModelManager.getStandaloneModel(StandaloneModelKey)` |
| `ModelEvent.RegisterAdditional` | `ModelEvent.RegisterStandalone` + `SimpleUnbakedStandaloneModel` |

**QuadCollection 的方向语义**：`getQuads(Direction)` 按方向分类（culled），`getQuads(null)` 只返回
**unculled**。方块模型（elements + faces）的 quad 全部带方向 → `getQuads(null)` **为空**！
必须遍历 6 个方向收集（或直接用接受 `BlockStateModelPart` 的提交方法自动遍历）。

### 2.6 Atlas 拆分（关键坑）

26.2 的 block atlas（`minecraft:textures/atlas/blocks.png`）**不再包含 `item/` 路径的纹理**。
`SimpleModelWrapper.bake` 的 `findNonBlockSprites` 会**拒绝** sprite 不在 block atlas 的模型
（日志：`Rejecting block model ... contains sprites from outside of supported atlas`）。

**应对**：
- 纹理在 **block atlas**（`textures/block/`）→ `SimpleUnbakedStandaloneModel.simpleModelWrapper`
  （返回 `BlockStateModelPart`）
- 纹理在 **items atlas**（`textures/item/`）→ `SimpleUnbakedStandaloneModel.quadCollection`
  （直接烘焙几何，**绕过 atlas 检查**，返回 `QuadCollection`），渲染用 **item 管线**
  （`itemRenderType`，与 items atlas 自洽）

### 2.7 物品模型体系（datagen）

26.2 物品模型 = **两个文件**（缺一不可）：
- `assets/<ns>/items/<id>.json` —— ItemModel 描述（`{"model": {"type": "minecraft:model", "model": "..."}}`）
- `assets/<ns>/models/item/<id>.json` —— 传统模型（`{"parent": "minecraft:item/generated", "textures": {"layer0": "..."}}`）

**datagen 原生生成**（避免手写 JSON）：
```java
ItemModelGenerators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);          // item/generated
ItemModelGenerators.generateFlatItem(item, ModelTemplates.FLAT_HANDHELD_ITEM); // item/handheld
// 内部: template.create(modelLocation, TextureMapping.layer0(item), modelOutput) + itemModelOutput.accept(...)
```

### 2.8 GUI / 文字

| 1.21.1 | 26.2 |
|---|---|
| `GuiGraphics` | `GuiGraphicsExtractor`（`blit(RenderPipelines.GUI_TEXTURED, ...)`/`text()`/`item()`/`fill()`/`pose()` 2D Matrix3x2fStack） |
| `Screen.render(GuiGraphics,...)` / `renderBg` / `renderBackground` | `extractRenderState`/`extractContents`/`extractBackground`（无 renderBg） |
| `keyPressed(int,int,int)` / `charTyped(char,int)` / `mouseClicked(double,double,int)` | `keyPressed(KeyEvent)` / `charTyped(CharacterEvent)` / `mouseClicked(MouseButtonEvent, boolean)` |
| `StateSwitchingButton` | `ImageButton`（+ 自管理状态） |
| `graphics.renderTooltip(font, stack, x, y)` | `graphics.setTooltipForNextFrame(...)` |
| `Font.drawInBatch(...)` | `font.prepareText(...)` → `PreparedText.visit(GlyphVisitor)` → `TextRenderable.render(Matrix4fc pose, consumer, light, flat)` |
| `ClickType` | `ContainerInput` |
| `getTimer().getGameTimeDeltaPartialTick(true)` | `Minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true)` |

### 2.9 其他

| 1.21.1 | 26.2 |
|---|---|
| `LightTexture.FULL_BRIGHT` / `LightTexture.block(x,y)` | `LightCoordsUtil.FULL_BRIGHT` / `LightCoordsUtil.block(...)` |
| `FastColor.ARGB32.*` | `net.minecraft.util.ARGB.*` |
| `Camera.getPosition()` / `getRotation()` | `gameRenderer.gameRenderState().levelRenderState.cameraRenderState.pos` / `.orientation` |
| `getItemRenderer().getModel(...)` | `ModelManager.getItemModel(Identifier)` + `ItemModel.update(ItemStackRenderState, ...)` |
| `player.level().isClientSide`（字段） | `player.level().isClientSide()`（方法） |

---

## 3. RenderState 模式与性能

### 3.1 模式样板（vanilla LlamaSpitRenderer）

```java
public class LlamaSpitRenderer extends EntityRenderer<LlamaSpit, LlamaSpitRenderState> {
    private final LlamaSpitModel model;  // Java Model 类

    public void submit(LlamaSpitRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0, 0.15F, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot));
        collector.submitModel(model, state, poseStack, TEXTURE, state.lightCoords,
                              OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public LlamaSpitRenderState createRenderState() { return new LlamaSpitRenderState(); }

    public void extractRenderState(LlamaSpit entity, LlamaSpitRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.xRot = entity.getXRot(partialTicks);
        state.yRot = entity.getYRot(partialTicks);
    }
}
```

### 3.2 性能收益（RenderState 化的动机）

1. **状态提取与提交解耦**：`extractRenderState` 可在逻辑 tick 阶段计算并**缓存**插值结果，
   提交阶段零逻辑计算（纯矩阵变换 + 顶点写入）
2. **提交批处理**：`FeatureRenderPhase` 按管线/透明度分组，同管线提交可合并绘制调用
   （translucent 还做距离排序）
3. **状态复用**：`BaseRenderState` 可挂扩展数据（`ContextKey`），模型可见性、染色等每帧
   只需更新变化部分
4. **减少重复遍历**：`submitBlockModel`/`submitMultiLayerBlockModel` 原生完成方向遍历与
   putBakedQuad，不再需要渲染器侧手写顶点循环

### 3.3 Java 模型迁移（高性能方向）

26.2 的 `submitModel(Model, state, poseStack, renderType, ...)` 走 **ModelFeatureRenderer**
（Java `Model` 类 + `ModelPart` 层级 + `setupAnim`），是性能最优路径（顶点烘焙与变换最省）。

**JSON 方块模型 → Java Model 的迁移路径**：
1. Blockbench 模型导出为 **Java 类**（Blockbench 支持导出 Java entity model 格式，
   或手写 `ModelPart` 层级——cube + offset）
2. 注册 `ModelLayers`（`ModelLayerLocation`）+ `LayerDefinition`（`CubeListBuilder`）
3. 渲染器持有烘焙后的 `Model`，`submit` 阶段 `collector.submitModel(model, state, poseStack,
   RenderTypes.entityTranslucent(texture), light, overlay, tintedColor, sprite, outline, null)`
4. 纹理走**单独纹理**（`entityTranslucent(Identifier)` 单纹理管线），不再依赖 atlas

**适合迁移的**：静态/低顶点模型（星尘细胞、飞刀、晶簇等 Blockbench elements 模型）。
**不适合的**：动态/程序化几何（激光、拖尾、闪电）——继续用 `submitCustomGeometry`。

---

## 4. 实战踩坑记录（Summoner 迁移）

1. **模型被拒绝**：`SimpleModelWrapper.bake` 拒绝 items atlas 纹理 → 改用 `quadCollection` 注册
2. **quads 为空**：`getQuads(null)` 只返回 unculled → 遍历 6 方向
3. **染色失效**：`putBakedQuad` 走 11 参 addVertex → `setColor(int)` 单参版，包装器必须覆写
4. **tintLayers 限制**：无 tintIndex 的 quad 染色无效 → `submitCustomGeometry` + 包装器
5. **纹理路径错误**：正则替换残留（`item/entity//xxx`）、引用错误（`texture` 占位、错引其他模型）
6. **配方解析失败**：`Item.CODEC_WITH_BOUND_COMPONENTS` 在配方解析时校验 components 绑定，
   而绑定在 `ReloadableServerResources.updateComponentsAndStaticRegistryTags()`（reload 完成后）→
   workaround：`RegisterEvent`（ITEM）后绑定 `DataComponentMap.EMPTY` 占位
7. **datagen 互删**：`DataGenerator.Cached.purgeStaleAndWrite` 删除本次运行未生成的文件 →
   两端 run 加 `--uncached`
8. **datagen 环境 components 未绑定**：不经过 ReloadableServerResources → 同样 RegisterEvent
   绑定 workaround（datagen 与运行时统一）

---

## 5. Lyra 渲染基础设施迁移建议

1. **AttachmentEntityRenderDispatcher** 保持 `SubmitNodeCollector` 提交链（已是 26.2 模式），
   但引入 **RenderState 提取阶段**：新增 `extractRenderState(entity, state, partialTick)` 缓存
   插值结果，`render` 阶段零逻辑
2. **渲染器接口**建议演进为：
   ```java
   interface IAttachmentEntityRenderer<T extends AttachmentEntity> {
       AttachmentEntityRenderState createRenderState();
       void extractRenderState(T entity, AttachmentEntityRenderState state, float partialTick);
       void submit(T entity, AttachmentEntityRenderState state, PoseStack poseStack,
                   SubmitNodeCollector collector, CameraRenderState camera, PathNode visualNode);
   }
   ```
   `AttachmentEntityRenderState extends BaseRenderState`（neoforge 扩展，可挂 ContextKey 数据）
3. **模型渲染提交选择**：
   - JSON 模型（items atlas 纹理）→ `submitItem` 或 `submitBlockModel`（按 itemRenderType 分组）
   - JSON 模型（block atlas 纹理）→ `submitBlockModel` / `submitMultiLayerBlockModel`
   - Java Model → `submitModel`（性能最优，建议渐进迁移）
4. **染色**：统一封装（`RenderUtil` 的 `groupQuadsByRenderType` + 染色包装），
   tintLayers 优先（模型带 tintindex 时），否则包装器
5. **拖尾**：TrailRenderType（`RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE)`）
   已是 26.2 原生，保持

---

## 6. 参考源码

- `net.minecraft.client.renderer.SubmitNodeCollection`（提交链全方法）
- `net.minecraft.client.renderer.feature.BlockModelFeatureRenderer` / `ItemFeatureRenderer` /
  `ModelFeatureRenderer` / `CustomFeatureRenderer`
- `net.minecraft.client.renderer.item.ItemStackRenderState`（`LayerRenderState.submit` → `submitItem`）
- `net.minecraft.world.item.Item`（`CODEC_WITH_BOUND_COMPONENTS`）/ `ItemStack.MAP_CODEC`
- `net.minecraft.server.ReloadableServerResources`（components 绑定时机）
- `net.minecraft.data.DataGenerator$Cached`（purgeStaleAndWrite）/ `Main`（server/client 主类）
- `net.minecraft.client.data.models.ItemModelGenerators`（generateFlatItem）/ `ModelTemplates`
- neoforge：`ExtendedBlockModelFeatureRenderer`、`BaseRenderState`（IRenderStateExtension）

---

## 7. 26.2 渲染管线性能调查（Lyra 实践，2026-08）

> 基于 Lyra/Summoner 静态模型渲染的性能排查实战整理。目标：让 JSON 模型（standalone quads）获得 Java 模型级性能。

### 7.1 管线全貌：submit → phase → featureRenderer → draw → GPU

```
SubmitNodeStorage（Int2ObjectAVLTreeMap<SubmitNodeCollection>，order 分桶）
    → SubmitNodeCollection（15 个 FeatureRenderPhase 字段）
        → submitXxx(...) 每帧创建 Submit record → phase.submit(submit)
    → drainPhases()：遍历非空 phase → FeatureRenderDispatcher.prepareFrame
        → phase.sortInto(output)：按 featureType 分组（合并 batch）输出
        → FeatureRenderer.prepareGroup(context, submits, strictlyOrdered)
            → buildGroup()：getVertexBuilder(renderType) → 顶点写入
    → upload()：所有 draw 一次性 copyToBuffer（staging → GPU vertex buffer）
    → executeGroup()：每 draw 一次 renderType.drawFromBuffer(info)（GPU draw call）
```

### 7.2 关键机制（性能差异根源）

| 机制 | 说明 | 性能影响 |
|---|---|---|
| **BatchableSubmit.batchKey()** | SimpleFeatureRenderPhase 按 key 把提交合并为一个 group → 一个 draw | ★★★ **跨实体 draw 合并**。原版 Model/Item/Custom 的 Submit 均实现（batchKey = renderType）。自定义 Submit 若未实现 → 每实体一个 draw，draw call 与实体数线性 |
| **SimpleFeatureRenderPhase** | 按 featureType 分桶（unbatched + batches），**不排序** | 无排序开销；合并由 batchKey 决定 |
| **TranslucentFeatureRenderPhase** | 距离排序（float 数组 + unstableSort，O(n log n)），输出 strictlyOrdered | 半透明正确性的代价；`submitCustomGeometry` 固定进 Simple 版（不排序） |
| **StagedVertexBuffer** | 每帧共享一个 GPU vertex buffer（256KB 对齐池化，fence 延迟回收），所有 draw 分段；upload 一次 copyToBuffer | 上传开销与**总顶点数**相关，与 draw 数无关；draw 数只影响 **draw call** |
| **ITEM_ENTITY_TARGET** | item sheet 管线（RenderTypes.itemTranslucent 等）先渲染到离屏目标再合成（为 glint 透明服务），每帧 `itemEntityTarget.copyDepthFrom(mainTarget)` | ★★ 离屏渲染 + 每帧深度复制，无 foil 时纯开销 |
| **atlas 类型** | items/block atlas 是**普通 2D 纹理**（非纹理数组） | entity 管线可直接绑定 atlas（`withTexture("Sampler0", TextureAtlas.LOCATION_ITEMS)`），quad 的 packedUV 即 atlas 0-1 坐标 |
| **putBakedQuad** | 每顶点 `new Vector3f()`×2（pos + normal 变换） | ★ JIT 可能标量替换消除，次因 |
| **ModelPart.compile** | scratch Vector3f 复用，零分配 | 参考基线 |

### 7.3 执行顺序（LevelRenderer frame graph）

```
solidTerrain → renderSolidFeatures（executeSolid）
    → [depth 复制到 translucent/itemEntity/particle 目标]
    → executeTranslucent（shadows → translucentModels → nameTags → texts
      → translucentCustomGeometry → shapeOutlines → gizmos → translucentBlocksAndItems → waterMask）
    → executeOutline → translucentTerrain（半透明方块/云）
    → executeTranslucentAfterTerrain（afterTerrain phase）→ weather/clouds
```

⚠️ **feature 半透明 phase（translucentModels/translucentBlocksAndItems）都在半透明方块/云之前渲染**——原版固有顺序：实体先画、方块后画覆盖（透过半透明实体透视方块的根源）。**要方块后正确混合必须挂 afterTerrain**。afterTerrain 是 Simple phase（无内置排序）——实体间遮挡由**渲染类型的深度测试**负责（ENTITY_TRANSLUCENT 默认 depthWrite=true，近实体挡远实体，不依赖排序）。

> 曾尝试 buildGroup 内按 distanceToCameraSq 手动排序，但实体接近时闪烁、且位置距离不代表模型渲染大小——已回退（2026-08 记录）。

### 7.4 性能优化清单（按收益排序，Lyra 已实现 ✓）

1. **BatchableSubmit（batchKey = renderType）** ✓ —— LyraCustomSubmit 实现，同 renderType 的 trail/模型跨实体合并为单个 draw。10 实体 20 draw → 2 draw
2. **管线替换（entity + items atlas）** ✓ —— LyraItemRenderTypes.ENTITY_ATLAS_TRANSLUCENT，跳过 ITEM_ENTITY_TARGET 离屏渲染 + 每帧 copyDepthFrom
3. **手写顶点零分配** ✓ —— RenderUtil.writeQuads 复用 scratch Vector3f + 10 参 addVertex（无 putBakedQuad）
4. **烘焙数据缓存** ✓ —— StandaloneModelKey → quads 列表缓存，引用比较自动失效
5. **框架层（未做，后续方向）** —— AbstractAttachmentEntityRenderer 每帧 new RenderContext/TrailConfig/ModelConfig；TrailConfig.beginRender 每帧分配插值节点列表；modelModify 每帧构造 6 Quaternionf + 2 Matrix；DynamicLightDispatcher 每帧更新。静态化/缓存可进一步降 Java 开销

### 7.5 Lyra 渲染基础设施现状（26.2.6+）

| 类 | 职责 |
|---|---|
| `RenderUtil` | 静态渲染入口：renderStandalone（无染色/染色），缓存 + 手写顶点 + entity-atlas 管线 |
| `LyraItemRenderTypes` | items atlas + entity 管线（无离屏目标） |
| `LyraRenderPhases` | submitSpecial 封装（afterTerrain phase） |
| `LyraCustomSubmit` | TranslucentSubmit + BatchableSubmit（batchKey=renderType） |
| `LyraCustomFeatureRenderer` | 回调执行提交（RegisterFeatureRenderersEvent 注册） |
| `ColorVertexConsumer` | 颜色/透明度拦截包装器（合并原 AlphaBufferSource/TintedVertexConsumer） |

### 7.6 顶点写入快路径（BufferBuilder，26.2 反编译确认）

`BufferBuilder.addVertex(x,y,z,color,u,v,overlay,light,nx,ny,nz)`（11 参）对 **BLOCK/ENTITY 两种格式有快路径**：
- `blockFormat`（DefaultVertexFormat.BLOCK）或 `entityFormat`（DefaultVertexFormat.ENTITY）→ **一次 `beginVertex` + 全部元素 memPut 直写**
- 其他格式 → `super.addVertex` → default 链式（每元素 `beginElement` + 语义检查）——**慢路径**

⚠️ **VertexConsumer 包装器（如 ColorVertexConsumer）没有 11 参 override → 11 参调用落到 default → 逐元素链式慢路径**。实测：染色路径经包装器比直写慢一倍（250 vs 560 帧）。**自定义几何必须手写时把颜色算好直接传 11 参 addVertex，勿用包装器**（包装器只用于原版 putBakedQuad 的 setColor 拦截场景）。

### 7.7 管线深度写（depthWrite）陷阱

| 管线 | depthWrite | 说明 |
|---|---|---|
| `ENTITY_TRANSLUCENT` | **默认 true**（未显式关闭） | 多层半透明模型内层先画写深度 → 外层被剔除 → 半透明丢失、整体不透明 |
| `ENTITY_TRANSLUCENT_EMISSIVE` | 显式 `DepthStencilState(GREATER_THAN_OR_EQUAL, false)` | 半透明正确；FULL_BRIGHT 时与普通 translucent 视觉一致 |
| `RenderSetupBuilder` | **无 withDepthStencilState 方法** | 深度状态只能由管线决定，不能经 RenderSetup 覆盖 |

⚠️ 自建 RenderType 选择管线时确认 depthWrite：半透明几何用 EMISSIVE 系列（或验证管线定义）。实测依据：ENTITY_TRANSLUCENT 渲染"内层不透明 + 外层半透明"模型时外层消失。

### 7.8 关键经验

1. **draw 合并是首要优化**：自定义提交必须实现 `BatchableSubmit`（batchKey = renderType），否则与原版差距在 draw call
2. **管线目标决定 GPU 开销**：无 glint 的模型渲染不需要 item sheet 管线（ITEM_ENTITY_TARGET），entity 管线 + atlas 绑定即可
3. **排序 vs 层级**：排序 phase（Translucent）保证实体间距离序但先于方块；afterTerrain 保证方块序但实体间固定——按场景取舍
4. **渲染器框架开销**：每帧对象分配（RenderContext/插值节点/矩阵）在实体多时是主要 Java 开销，JIT 优化有限
5. **顶点写入走 11 参快路径**：ENTITY/BLOCK 格式直写内存；包装器会降级慢路径（染色实测差 2 倍）
6. **depthWrite 陷阱**：ENTITY_TRANSLUCENT 默认写深度，半透明多层模型外层会被剔除，用 EMISSIVE 管线
