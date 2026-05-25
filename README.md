# Android 音视频特效编辑 Demo 计划文档（GPU Native）

## 1. 项目目标与定位

本 Demo 聚焦于 **MediaCodec + SurfaceTexture + OpenGL ES Shader + A/V 时间线特效编辑** 的完整闭环，目标是做出一个可实时预览、可离线导出的 Android 音视频特效编辑器原型。

### 1.1 目标能力

1. 视频纹理化播放链路：`MediaCodec -> SurfaceTexture -> OES Texture -> GL Render`
2. 2D 帧动画多层合成：`Background / Character / FX / Overlay`
3. 音视频与帧动画统一时钟同步：`Audio master clock`
4. Shader 特效链：基础调色 + 边缘羽化/阴影 + 可扩展转场
5. 离屏渲染：同一渲染图支持实时预览与离线导出
6. 性能目标：1080p 稳定，向 4K 提供可控降级策略

### 1.2 非目标（首期不做）

- 不做完整 NLE 编辑器（复杂轨道编辑 UI、海量模板系统等）
- 不做 Vulkan 首版实现（架构可预留）
- 不做过重 AI 特效（美颜分割、超分等）

---

## 2. 整体架构（对齐 SDK / Runtime / Engine）

### 2.1 三层结构

1. **SDK Facade（Kotlin）**
   - 对外 API：`PlayerView / Player / Clip / Config / Callback`
   - 生命周期绑定（Activity/Fragment）
   - 业务只与该层交互

2. **Runtime（Kotlin + JNI）**
   - Timeline 调度
   - 资源请求编排
   - 播放状态机（Idle/Prepared/Playing/Paused/Seeking/Stopped）
   - 渲染命令桥接到 Native

3. **Engine Core（C++）**
   - OpenGL ES 渲染内核
   - 纹理缓存、FBO 管理、Shader 管理
   - RenderGraph 执行
   - 统计指标输出（render/decode/upload/late frame）

### 2.2 模块图（逻辑）

```text
App
 └─ SDK Facade (Kotlin)
     ├─ Editor Controller
     ├─ Timeline API
     └─ JNI Bridge
          └─ Native Engine (C++)
              ├─ Decode Module (MediaCodec)
              ├─ Texture Bridge (SurfaceTexture/OES)
              ├─ RenderGraph (FBO Passes)
              ├─ Layer Composer
              ├─ Audio Clock Sync
              └─ Export Pipeline (Encoder/Muxer)
```

---

## 3. 播放与渲染主链路

## 3.1 实时预览链路

`AudioClock -> TimelineTick -> LayerResolve -> RenderGraph -> Present`

步骤：

1. Audio 时钟给出当前播放时间 `t`
2. Timeline 根据 `t` 计算：视频帧索引、动画帧索引、各 layer 可见性、特效参数
3. 视频轨解码输出到 `SurfaceTexture`（OES 外部纹理）
4. 帧动画层从 Atlas/纹理缓存取采样区域
5. RenderGraph 执行多 pass（调色/羽化/阴影/转场）
6. 合成结果输出到屏幕

## 3.2 离线导出链路（离屏渲染）

`AudioClock(or ExportClock) -> TimelineTick -> RenderGraph(Offscreen FBO) -> EncoderSurface -> MediaMuxer`

步骤：

1. 导出时使用固定步进时钟（避免实时抖动）
2. 同一 Timeline 与同一 RenderGraph 在离屏 FBO 重放
3. 最终帧写入编码输入 Surface
4. H.264/H.265 + AAC 复用为 MP4

关键原则：**预览与导出复用同一渲染图，避免“所见非所得”**。

---

## 4. Layer 与特效系统设计

### 4.1 Layer 合成顺序

`Background -> Back FX -> Shadow -> Character -> Front FX -> Overlay`

### 4.2 Layer 最小数据结构

- `layerId`
- `textureRef`（视频/OES 或 atlas 子图）
- `transform`（pos/scale/rotation）
- `opacity`
- `blendMode`
- `visibleRange`（startUs/endUs）
- `materialRef`（shader program + uniforms）

### 4.3 Shader 特效链（首批）

1. 基础调色：亮度、对比度、饱和度、色温
2. 边缘羽化：软边 alpha 过渡
3. 阴影：角色接触阴影/投影简化版
4. 转场：淡入淡出、滑动遮罩（可扩展）

---

## 5. A/V + 帧动画时钟同步策略

## 5.1 主时钟策略

- 默认：`Audio master clock`
- 无音频场景：`Monotonic clock fallback`

## 5.2 同步规则

- 视频解码按 PTS 贴近主时钟
- 帧动画按 `t` 直接计算目标帧 index
- 偏差控制：
  - 小偏差：微调播放速率或延迟提交
  - 大偏差：丢帧/追帧，优先保持音画主观同步

## 5.3 Seek 处理

- 停止渲染提交
- flush 解码器
- 重建关键帧附近状态
- 重置 timeline cursor
- 恢复 render loop

---

## 6. 关键技术选型

- 平台：Android（优先 Android 12+，兼容至业务设定 minSdk）
- 渲染：OpenGL ES 3.x（首版按 3.0/3.2 能力子集实现）
- 视频：MediaExtractor + MediaCodec
- 纹理桥接：SurfaceTexture + OES External Texture
- 音频：AudioTrack / MediaCodec（按轨道需求）
- 导出：MediaCodec Encoder + MediaMuxer
- 语言：Kotlin + C++17（JNI）

---

## 7. 性能预算与 1080p/4K 策略

## 7.1 1080p 目标

- 预览 30/60fps 稳定（按机型能力）
- 渲染主线程帧耗时稳定在预算内
- 掉帧率可观测、可回归

## 7.2 4K 策略（可降级）

1. 减少同时启用的高耗 pass
2. 降低 FBO 工作分辨率（动态分辨率）
3. 限制层数与复杂混合
4. 关闭高阶阴影/羽化精度
5. 回退到 1080p 导出并保持效果正确

## 7.3 内存与资源策略

- 纹理池 + FBO 池复用
- LRU 缓存与水位控制
- 禁止每帧创建/销毁大对象
- GL 线程禁止磁盘 I/O 与重型解析

---

## 8. 里程碑计划（MVP -> 可演示）

### M1：工程骨架（1 周）

- SDK module + sample app
- JNI init/release
- GLSurfaceView + 基础 render loop

### M2：视频纹理化播放（1~2 周）

- MediaCodec 解码到 SurfaceTexture
- OES 纹理显示
- play/pause/seek

### M3：帧动画多层合成（1~2 周）

- atlas 加载
- layer 合成顺序
- timeline 基础控制

### M4：A/V 主时钟同步（1 周）

- Audio clock 驱动
- 漂移监控与修正

### M5：Shader 特效链（1~2 周）

- 调色 + 羽化/阴影 + 简单转场
- 特效参数实时调节

### M6：离线导出（1~2 周）

- 离屏渲染
- 编码/封装导出 MP4
- 导出进度与取消

### M7：性能优化与 4K 策略（持续）

- 指标采集
- 1080p 稳定性
- 4K 降级策略验证

---

## 9. 验收指标（建议首批）

1. 功能正确性
   - 视频、音频、帧动画可统一时间线播放
   - 特效区间生效正确
   - 预览与导出一致

2. 性能指标
   - FPS（P50/P95）
   - render/decode/upload 耗时（P50/P95）
   - late frame ratio
   - cache hit ratio
   - 内存峰值与水位命中次数

3. 稳定性
   - 长时间播放无明显泄漏
   - 高频 seek/暂停恢复无黑屏和崩溃

---

## 10. 建议新仓库目录（用于后续实现）

```text
av-gpu-demo/
├─ app/                      # demo 业务壳
├─ av-sdk/                   # 对外 SDK facade
├─ av-runtime/               # Kotlin runtime + JNI bridge
├─ av-engine-native/         # C++ engine core
├─ shaders/                  # GLSL
├─ assets-sample/            # 示例素材
├─ tools/                    # 资源预处理脚本
├─ docs/
│  ├─ architecture/
│  ├─ specs/
│  ├─ perf/
│  └─ roadmap/
└─ README.md
```

---

## 11. 下一步建议（开工清单）

1. 先把本计划文档复制到新仓库 `docs/roadmap/`。
2. 先落 M1、M2，尽快看到“视频纹理化播放闭环”。
3. 立刻加入 FrameStats，避免“能跑但不可测”。
4. 每完成一个里程碑就固化一份 Spec（输入、输出、验收、风险）。
5. 在第二周结束前做一次 1080p 压测基线，作为后续优化对照。

