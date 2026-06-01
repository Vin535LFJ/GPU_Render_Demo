# System Architecture

## 架构原则

- **YAGNI**：MVP 不需要的架构不引入。
- **KISS**：先打通最短可运行链路，再逐步增强。
- **SOLID**：模块职责单一，跨层依赖通过接口或命令表达。
- **Clean Architecture**：业务控制、运行时调度、Native 渲染内核分层。
- **Specification Driven Development**：编码前先冻结可验证规格。
- **Incremental Delivery**：每个阶段都有可运行、可验收输出。

## 总体分层

```text
App / Sample
  -> SDK Facade (Kotlin)
  -> Runtime (Kotlin + JNI)
  -> Engine Core (C++17 + OpenGL ES)
```

### SDK Facade

- 面向 sample app 和未来业务调用方。
- 暴露最小播放控制 API。
- 不直接持有 GL、EGL、MediaCodec 资源。
- 不实现 Runtime 状态机细节。

### Runtime

- 负责状态机、命令 gate、线程编排、JNI 调用、错误映射、指标汇聚。
- 不直接执行 GL 调用。
- 不直接绕过 DecodeThread 操作 MediaCodec。

### Engine Core

- 负责 OpenGL ES 渲染内核、OES 纹理采样、RenderGraph、FBO/texture 资源、Native metrics。
- 不负责 Android UI 生命周期决策。
- 不负责业务层模板逻辑。

## 最小预览链路

```text
Audio/Monotonic Clock
  -> Runtime Tick
  -> DecodeThread dequeue / SurfaceTexture frame available
  -> RenderThread updateTexImage
  -> OES to 2D pass
  -> Present pass
  -> GLSurfaceView
```

P1 阶段允许先使用 monotonic clock 或简化 audio anchor，但接口必须为 Audio master clock 留出位置。

## 未来导出链路

```text
ExportClock fixed step
  -> Timeline Tick
  -> RenderGraph Offscreen FBO
  -> EncoderSurface
  -> MediaCodec Encoder
  -> MediaMuxer
```

导出不进入 MVP。设计约束是未来导出必须复用同一 RenderGraph，避免预览与导出效果分裂。

## 线程模型

| 线程 | 负责 | 不负责 |
|---|---|---|
| UI / Business Thread | 发起 prepare/play/pause/seek/stop/release 命令 | 不执行状态迁移细节；不操作 GL/Codec |
| Runtime Control Thread | 校验状态机、编排跨线程命令、汇聚错误与指标 | 不直接调用 GL；不直接 dequeue/flush codec |
| DecodeThread | MediaExtractor、MediaCodec configure/start/flush/dequeue、关键帧 seek | 不消费 GL texture；不调用 updateTexImage |
| RenderThread | EGL、SurfaceTexture attach/detach/updateTexImage、RenderGraph、present | 不控制 MediaCodec 生命周期 |
| Callback Thread | 按统一策略回调状态、错误、指标 | 不反向重入 Runtime 控制命令 |

## 模块职责边界

### 1. SDK Facade

- **目标**：提供最小、稳定、状态可观测的 Kotlin API。
- **输入**：媒体 URI/path、播放控制命令、surface/view 生命周期事件。
- **输出**：状态回调、错误回调、基础指标回调。
- **依赖**：Runtime Controller。
- **不负责什么**：不操作 MediaCodec；不操作 GL；不实现 RenderGraph；不吞掉 Runtime 错误。
- **验收标准**：非法调用返回结构化错误；release 后 handle 不可再用；回调线程策略一致。

### 2. Runtime Controller

- **目标**：作为控制平面，保证所有命令经过状态机 gate。
- **输入**：SDK 命令、线程事件、错误事件、指标事件。
- **输出**：状态迁移、DecodeThread 命令、RenderThread 命令、JNI 结构化结果。
- **依赖**：DecodeThread、RenderThread、Native Engine JNI、Metrics。
- **不负责什么**：不执行 GL 调用；不直接持有 GL handle；不绕过 owner 线程。
- **验收标准**：状态转换符合 Runtime Spec；非法转换有错误码和 warning log；seek 必须经过 REQUESTED/FLUSHING/PRIMING/RECOVERING。

### 3. Decode Module

- **目标**：把压缩视频流解码到 SurfaceTexture 绑定的 Surface。
- **输入**：媒体源、seek target、flush/stop/release 命令。
- **输出**：解码帧可用事件、EOS 事件、decode metrics、错误事件。
- **依赖**：MediaExtractor、MediaCodec、Runtime Controller、SurfaceTexture 的 producer Surface。
- **不负责什么**：不调用 updateTexImage；不消费 GL texture；不决定 UI 状态。
- **验收标准**：seek 时只在 SEEK_FLUSHING/STOPPED 范围 flush；EOS 进入 EOS_HOLD；flush 后 frame queue 清理可观测。

### 4. Texture Bridge

- **目标**：桥接 MediaCodec 输出 Surface 与 GL OES Texture。
- **输入**：OES texture id、SurfaceTexture frame available 事件、RenderThread tick。
- **输出**：可采样的 OES Texture 和时间戳。
- **依赖**：RenderThread EGL context、SurfaceTexture、MediaCodec output Surface。
- **不负责什么**：不做业务特效；不做 codec 控制；不跨线程 attach/detach。
- **验收标准**：attach/detach/updateTexImage 只在 RenderThread；context lost 后重建 OES texture 和 SurfaceTexture；禁止复用旧 GL id。

### 5. RenderGraph

- **目标**：以 pass 图方式执行最小 OES 采样、转换、合成和 present。
- **输入**：OES texture、viewport、render params、future layer inputs。
- **输出**：屏幕 framebuffer 或未来 offscreen FBO。
- **依赖**：Engine Core、Shader Program、FBO/Texture Pool、RenderThread。
- **不负责什么**：不调度 DecodeThread；不决定播放状态；MVP 不实现复杂特效编排。
- **验收标准**：生命周期为 INIT/CONFIGURED/RUNNING/RECONFIGURING/RELEASED；每个 pass 声明输入输出格式；OES 输入 pass 不叠加业务特效。

### 6. Clock / AV Sync

- **目标**：为播放、seek recover 和未来动画帧提供统一时间基准。
- **输入**：audio timestamp、video PTS、monotonic fallback、seek target。
- **输出**：当前 timeline time、drift、纠偏决策、sync metrics。
- **依赖**：Runtime Controller、Decode metrics、Render metrics。
- **不负责什么**：不直接 drop GL 资源；不直接 flush codec；不改变状态机。
- **验收标准**：drift 使用冻结三段阈值；seek recover 后 500ms 禁止 drop；纠偏防振荡可观测。

### 7. Metrics / Validation

- **目标**：让 MVP 是否达标可量化判断。
- **输入**：render/decode/upload 时间、FPS、drop、drift、seek recover、memory、crash-free 信息。
- **输出**：FrameStats、日志、阶段验收报告。
- **依赖**：Runtime、DecodeThread、RenderThread、Engine Core。
- **不负责什么**：不改变运行时行为；不隐藏错误。
- **验收标准**：P50/P95/P99 口径一致；覆盖播放、pause/resume、seek、EOS、release。

### 8. Export Pipeline（后置）

- **目标**：在 P3 阶段复用 RenderGraph 进行离屏渲染并编码 MP4。
- **输入**：ExportClock、Timeline、RenderGraph 输出帧、音频流。
- **输出**：MP4 文件、导出进度、取消/失败状态。
- **依赖**：RenderGraph、Offscreen FBO、MediaCodec Encoder、MediaMuxer。
- **不负责什么**：不进入 MVP；不单独维护一套与预览不同的渲染逻辑。
- **验收标准**：预览/导出一致性可量化；取消和失败有明确状态。

## 资源 Ownership Matrix

| 资源/动作 | 唯一 Owner | 禁止 |
|---|---|---|
| State transition | Runtime Control Thread | UI/Decode/Render 直接改状态 |
| MediaCodec configure/start/flush/dequeue | DecodeThread | RenderThread 或 UI 直接操作 codec |
| EGLDisplay/EGLContext/EGLSurface | RenderThread | 其他线程 eglMakeCurrent |
| SurfaceTexture attach/detach/updateTexImage | RenderThread | DecodeThread/UI 调用 updateTexImage |
| GL texture/FBO/shader | RenderThread / Engine Core | Runtime 持有 GL id 并跨线程使用 |
| Frame queue produce | DecodeThread | RenderThread 生产 decode frame |
| Frame queue consume | RenderThread | DecodeThread 消费渲染帧 |
| Callback dispatch | Callback Thread policy | 任意线程无约束回调 |

## Release Destroy Order

release 必须遵循固定顺序：

1. stop clock
2. stop render submit
3. flush decoder
4. clear frame queue
5. detach SurfaceTexture
6. destroy GL resource
7. destroy EGL
8. stop thread

任何实现不得为了机型差异绕过该顺序。
