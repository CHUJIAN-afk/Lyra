# Lyra 1.21.1.23 / NeoForge

Lyra 自带动态光照和特效合成管线，不再需要 Photon、LDLib2 或 KilaGraph。Registrate 继续以 jarjar 内置。GeckoLib 仅为可选联动，使用 Geo 虚拟实体渲染器的子模组需要自行声明 GeckoLib 4.9.3 前置。

## 动态光照

在客户端渲染回调中提交当帧光源，下一帧自动过期：

```java
LyraAPI.light().point().at(pos.x, pos.y, pos.z)
        .color(255, 100, 30).intensity(8f).range(7.75f)
        .shadows(false);
LyraAPI.light().spot(20f, 35f).at(pos.x, pos.y, pos.z)
        .direction(0f, -1f, 0f).color(1f, 0.5f, 0.2f)
        .intensity(12f).range(10f);
```

`color(int, int, int)` 使用 0～255，`color(float, float, float)` 使用线性强度；位置、范围和光源半径以方块为单位，聚光角度以度为单位。光源在 `AFTER_SKY` 清空，在 `AFTER_LEVEL` 收集并照明；请在这两个阶段之间提交，不要缓存 `light()` 返回的池化对象。切换世界与重载资源会释放 GPU 资源。

光源按距离和强度排序，默认最多 128 个可见光源，每个 64 像素分块保留最多 32 个。表面光照从场景深度重建位置和法线，`volumetric` 控制沿视线积分的体积散射强度；`shadows(true)` 使用屏幕深度遮挡，无法还原屏幕外遮挡物。Iris 阴影通道不会续期光源。

## HDR 与 bloom

自定义特效可以使用 `HdrTarget.resize(target, width, height, useDepth)` 复用 RGBA16F 目标，颜色纹理使用线性过滤，创建或缩放后恢复调用方的帧缓冲、视口与绘制状态。绘制完成后调用 `EffectPipeline.bloom(target)` 获取原图与辉光的合成结果；该方法采用亮度软阈值提取、多级降采样和逐级上采样，结束后恢复调用方状态。透明度保留原图 alpha，辉光不会增加遮挡。`EffectPipeline.composite(result)` 按预乘透明度绘入主目标；`drawFullscreenQuad()` 为自定义合成着色器提供全屏四边形。

客户端配置提供 `dynamic_lights`、`max_lights`、`bloom`、`bloom_with_shaders`、`bloom_levels`、`bloom_threshold`、`bloom_strength`。Bloom 默认使用 5 级降采样，阈值为 1.001、强度为 0.7，开启光影包时仍可生效；已有配置保留用户设置。

## GeckoLib 可选接入

`AttachmentEntity` 不引用 GeckoLib。安装 GeckoLib 后，条件 Mixin 为其注入 `GeoAnimatable`、实例动画缓存与 tick 实现。没有 GeckoLib 时不会加载这个 Mixin。

Geo 渲染器继续继承 `AbstractAttachmentEntityGeoRenderer<T>`。子实体可以声明 `registerControllers(AnimatableManager.ControllerRegistrar)`；由于接口在运行时注入，此方法不加 `@Override`，创建控制器时使用 `new AnimationController<>((GeoAnimatable) this, ...)`。没有动画的实体不必声明此方法。通用渲染颜色改用 `first.lyra.client.render.Color`，只在 Geo 渲染器边界转换为 GeckoLib 的颜色。

## 整体透明度

半透明模型先收集网格，再按距离绘入共享颜色/深度图集。每个模型占用独立区域，消除自身重叠后只应用一次整体透明度。图集装满便批量合成并复用，不再为每个模型分配纹理、读取完整 GL 状态或复制世界深度。

同材质的模型整帧合并上传顶点和索引，连续绘制时只设置一次材质与着色器，每个模型按自己的索引范围绘制。面排序使用复用的基本类型数组，同距时保持原顺序。图集按模型占用面积扩容并跨帧复用，常规分辨率下颜色与深度纹理预算约 64 MiB；更高分辨率至少保留一屏大小。切换世界或重载资源时释放。

模型在世界透明层和 Lyra 世界照明后合成，使用硬件深度测试接受遮挡，不写世界深度。颜色、深度、Lyra 光源数据使用批量合成路径；没有光源时跳过光照计算。完全不透明的模型保留普通路径，完全透明的模型跳过。

Geo、JSON 与虚拟实体渲染器已接入；自定义模型可使用相同作用域：

```java
WholeModelTransparency.render(poseStack, buffers, opacity, packedLight, source -> {
    // 提交整个模型，顶点颜色不再乘整体 opacity。
});
```

## Iris 模型阴影

Iris 为可选联动，未安装时跳过阴影 Mixin。安装 Iris 1.8 及以上并启用支持实体阴影的光影包后，Lyra 会在 Iris 的实体阴影通道中提交虚拟实体模型，使用该通道的视图、相机坐标和缓冲，不依赖玩家画面是否看得到模型。

阴影使用模型正常透明度，跳过第一人称近距离淡出；完全透明的模型不投影。Geo、JSON、`LyraModelRenderer.virtualEntity` 及原版物品模型复用现有渲染代码，JSON 的物品图集材质在阴影通道转为直接写入阴影缓冲的实体材质，半透明模型不进入屏幕透明度图集。半透明阴影的混合与颜色由光影包决定。

阴影通道跳过上下文拖尾和残影，过滤拖尾材质及非模型顶点格式，不续期即时动态光。默认 `renderShadowModel` 会调用已有的 `renderModel` 与 `render`，兼容模型仍放在 `render` 中的旧渲染器；若 `render` 中含自绘着色器、粒子等附加操作，可覆写 `renderShadowModel(PoseStack, MultiBufferSource)`，仅提交需要投影的模型。

## 模型残影

在附件实体渲染器的 `createContext` 中设置可选的残影上下文：

```java
return super.createContext(entity, visualNode, partialTick, packedLight)
        .afterimage(new AfterimageContext()
                .color(Color.ofOpaque(0x66CCFF))
                .alpha(0.35f)
                .lifetime(8));
```

`color` 叠乘模型原有颜色，默认白色；`alpha` 是初始透明度，默认 0.35；`lifetime` 以 tick 为单位，默认 10 tick（0.5 秒）。颜色自身的 alpha 也参与透明度。每个渲染帧保存当前模型的姿态、材质、光照和世界位置，之后线性淡出，不重新计算动画，也不持有实体引用。停止设置上下文或移除实体后，已经生成的残影继续消散；游戏时间停止时暂停生成和计时。

只捕获 `renderModel` 和 `LyraModelRenderer` 的模型提交，不重放轨迹、附加渲染、粒子或光源。残影复用整体透明度管线，自己重叠的模型块只应用一次透明度。上下文写入后再修改颜色、透明度或寿命，只影响之后生成的残影。切换世界和重载资源时清空。

## 路径关键帧

普通实体可实现 `IPathEntity` 接入同一套路径关键帧与字段同步，或仅实现 `ISyncEntity` 注册同步字段。在实体 NBT 方法中委托 `saveSyncFields` / `loadSyncFields`，已注册字段自动保存和恢复。完整实体、客户端翻滚变换和继承接法见 [普通实体接入示例](docs/entity-path-sync.md)。

`PathNode` 继续作为逻辑关键帧，位置、碰撞、历史节点和 `PlannedPath.run` 回调沿用原有逐 tick 推进。可在服务端为逻辑区间添加 `RenderKeyframe`，客户端仅在渲染时使用，不增加逻辑 tick 或触发回调。

曲线路径可直接采样：

```java
PlannedPath path = PlannedPath.sample("arc", 10, 4, t ->
        new PathNode(entity.calculateBezierPoint(t, start, control, end), 0f, 0f, 0f));
entity.setPath(path);
```

`10` 为逻辑区间数量，`4` 为每个区间的渲染分段数；两者必须为正数。采样函数接收整条路径的 0～1 进度，在服务端计算真实位置和朝向，每个区间额外采样 3 个中间点。起始节点保留旧路径的消费时序，设定后在第一次推进时消费一次；后续每 tick 消费一个逻辑节点。

已有逻辑节点也可手动补充渲染关键帧：

```java
PlannedPath path = new PlannedPath(nodes)
        .render(1, List.of(new RenderKeyframe(0.5f, midpoint)));
entity.setPath(path);
```

`render` 的下标指目标逻辑节点；上例在 `nodes[0]` 到 `nodes[1]` 之间插入中点。帧按 `partialTick` 严格递增排列，时间取值大于 0、小于 1；两个端点由系统补齐。`setPlannedPath` 同样支持渲染关键帧，且保留从当前位置开始推进的原有用法。

两类关键帧均随服务端实体同步分发，渲染区间带有完整端点，客户端按时间分段插值位置，朝向复用 `PathNode.lerp` 的四元数插值。一个客户端 tick 收到多个更新时使用最后一个区间；没有新更新时停留在逻辑终点，避免重复播放旧区间。模型、光源和残影通过现有 `getRenderNode` 自动使用渲染关键帧。

旧 `PathNode`、`PlannedPath` 构造器、`setPath`、`setPlannedPath` 和回调接口继续可用；没有渲染关键帧时仍使用原有插值。`setPath(PlannedPath)` 现在也会设置待推进路径，与列表重载保持一致。

## 镜头抖动

```java
LyraAPI.shakeCamera(level, position, 24f, 0.6f);
```

仅服务端调用有效，广播给同维度作用范围内的玩家。`position` 为震源世界坐标，`range` 为大于 0 的作用半径，单位为格；`strength` 为 0～1。客户端收到后，按玩家到震源的距离线性衰减，震源处为完整强度，半径边缘为 0。

连续震动累加强度，上限为 1；每个客户端世界 tick 保留 85%，低于 0.001 时归零，帧间插值平滑衰减。只影响第一人称镜头，最大俯仰振幅 3°、翻滚振幅 5°，不改变玩家朝向。第三人称仍正常衰减，游戏暂停时停止计时，切换世界时清空。

## 构建

```powershell
$env:JAVA_HOME = 'C:\Users\l1518\.jdks\jbr-21.0.11'
.\gradlew.bat compileJava processResources
```

构建产物位于 `build/libs`，可通过 `publishToMavenLocal` 供本地子模组使用。
