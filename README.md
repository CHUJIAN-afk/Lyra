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

## 整体透明度

半透明模型先收集网格，再按距离绘入共享颜色/深度图集。每个模型占用独立区域，消除自身重叠后只应用一次整体透明度。图集装满便批量合成并复用，不再为每个模型分配纹理、读取完整 GL 状态或复制世界深度。

模型在世界透明层和 Photon 世界照明后合成，使用硬件深度测试接受遮挡，不写世界深度。颜色、深度、Photon 数据使用批量合成路径；没有光源的分簇跳过法线与光照计算。完全不透明的模型保留普通路径，完全透明的模型跳过。

Geo、JSON 与虚拟实体渲染器已接入；自定义模型可使用相同作用域：

```java
WholeModelTransparency.render(poseStack, buffers, opacity, packedLight, source -> {
    // 提交整个模型，顶点颜色不再乘整体 opacity。
});
```

## 构建

```powershell
$env:JAVA_HOME = 'C:\Users\l1518\.jdks\jbr-21.0.11'
.\gradlew.bat compileJava processResources
```

本次优化仅做编译验证，效果与帧率由手动测试确认。任务结束后清理测试类、截图、日志、临时世界与临时运行配置。
