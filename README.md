# Lyra 1.21.1.16 / NeoForge

动态光照使用 Photon 2.2.8，客户端需要 Photon 完整包、LDLib2 2.2.42 和 GeckoLib。Photon 的 `-all.jar` 已包含 KilaGraph；不再需要本地 `light` 动态光 API。

## 点光与线光

保留 `LyraAPI.light(Vec3, int)` 和 `LyraAPI.light(Vec3, float)`，分别接收原版 0～15 等级和归一化强度。满强度映射为 Photon intensity=8、range=7.75，衰减形状遵循 Photon。

```java
LyraAPI.light(position, 0xFF6600, 12f, 8f);
LyraAPI.lineLight(start, end, 0x44AAFF, 8f, 6f, 1.5);
```

仅客户端渲染线程使用，即时调用需要每帧续期。RGB 为 `0xRRGGBB`，强度与范围使用 Photon 单位。

线光沿整条线段分布点光，`intensity` 为每格线长的强度，`spacing` 由调用方保证大于 0。端点采用半权重，改变间距不会增加总发光量；零长度退化为点光。最多 256 个采样点，达到上限时增大间距，仍覆盖整条线。Photon 全局最多保留 1024 个可见光源。

虚拟实体可在客户端初始化时按类型注册光源，不依赖模型绘制：

```java
LyraLights.register(MY_ENTITY_TYPE, (entity, sink, partialTick) -> {
    Vec3 pos = entity.getRenderNode(partialTick).pos();
    sink.next().at(pos.x, pos.y, pos.z)
            .color(1f, 0.3f, 0.05f).intensity(10f).range(8f)
            .shadows(false);
});
```

`sink.next()` 仅当前帧有效。回调中可用 `LyraLights.line(sink, start, end, template, spacing)` 提交线光，模板为 Photon 的 `DynamicLight`。实体不再提交时光源自然过期；切换世界清空即时提交。

## 流动激光

`FlowingLaserRendererHelper.builder()` 或 `LyraAPI.flowingLaser()` 为独立入口，现有 `LaserRendererHelper`、Photon 点光和普通聚光灯保持原有行为：

```java
FlowingLaserRendererHelper.builder()
        .inner(0xFFFFFF, 0.6f)
        .edge(0xFF3355, 1f)
        .from(start)
        .direction(direction)
        .length(32f)
        .spread(8f, 6f)
        .radius(0.02f, 4f, 0.1f)
        .flowSpeed(12f)
        .light(new DynamicLight().intensity(32f).volumetric(1f).shadows(true))
        .render(partialTick);
```

起点是世界坐标，方向自动归一化。长度和三段半径使用方块单位；内部、边缘色使用 `0xRRGGBB`，对应半径是三段半径的截面比例，满足 `0 < 内部半径 < 边缘半径`。上例中最大内部半径约 2.4 格，外缘 4 格。初始半径在前半程过渡到过程半径，后半程过渡到终点半径；`spread` 两个系数必须大于 0，值越大越快扩张、越晚收束，值为 1 时线性变化。`flowSpeed` 单位为格/秒，正数向终点流动，0 静止，负数反向。

默认内部白色/0.6，边缘红色/1，长度 16，半径 0→2→0，扩散与收缩系数均为 6，流速 8。`.light(...)` 继承 Photon 的强度、总色调、阴影、光源半径和体积散射设置，默认强度 32、体积散射 1。位置、朝向、范围和锥角由激光轮廓计算，不取模板的这些字段。

流动纹理由随机错开的梭形脉冲组成，两端收尖，中部较宽，长短、位置和强弱各不相同；外部色构成脉冲主体，内部色勾出亮脊。纹理随流速向前移动并平滑随机抖动，流速为 0 时一并静止。体积积分保留前后遮挡和双色明暗，避免光束厚度增加后把内部纹理累加成一片白色。

推荐在 `LyraLights.register` 的光源回调中调用 `.submit(sink, partialTick)`，与 Photon 当前帧的光照采集同步；在普通渲染回调中调用 `.render(partialTick)` 则和即时点光一样每帧续期。每束激光占用 Photon 的一个光源，复用分簇、屏幕/体素阴影、深度遮挡及体积光合成。表面照明与体积光读取同一份轮廓、颜色和流动参数。可见光束受 Photon 的体积光开关、密度和光影兼容设置控制，关掉体积光仍保留表面照明。此扩展针对 Photon 2.2.8；没有修改依赖 JAR。

## 整体透明度

半透明模型先收集网格，再按距离绘入共享颜色/深度图集。每个模型占用独立区域，消除自身重叠后只应用一次整体透明度。图集装满便批量合成并复用，不再为每个模型分配纹理、读取完整 GL 状态或复制世界深度。

同材质的模型整帧合并上传顶点和索引，连续绘制时只设置一次材质与着色器，每个模型按自己的索引范围绘制。面排序使用复用的基本类型数组，同距时保持原顺序。图集按模型占用面积扩容并跨帧复用，常规分辨率下颜色与深度纹理预算约 64 MiB；更高分辨率至少保留一屏大小。切换世界或重载资源时释放。

模型在世界透明层和 Photon 世界照明后合成，使用硬件深度测试接受遮挡，不写世界深度。颜色、深度、Photon 数据使用批量合成路径；没有光源的分簇跳过法线与光照计算。完全不透明的模型保留普通路径，完全透明的模型跳过。

Geo、JSON 与虚拟实体渲染器已接入；自定义模型可使用相同作用域：

```java
WholeModelTransparency.render(poseStack, buffers, opacity, packedLight, source -> {
    // 提交整个模型，顶点颜色不再乘整体 opacity。
});
```

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

本次优化仅做编译验证，效果与帧率由手动测试确认。任务结束后清理测试类、截图、日志、临时世界与临时运行配置。
