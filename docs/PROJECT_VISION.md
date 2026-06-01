# Project Vision

## 一句话描述

这是一个 **Android Native GPU Video Runtime Engine Demo**：用 Kotlin + JNI + C++17 + OpenGL ES 3.x + MediaCodec 打通 MP4 解码、OES 纹理上屏、RenderGraph 渲染、音视频同步，并在稳定后扩展基础特效与离线导出的最小闭环。

## 项目真正目标

项目目标不是做一个完整剪辑产品，而是验证并沉淀一条可实现、可观测、可扩展的 Android GPU Native 音视频 Runtime 链路。

核心闭环：

```text
MP4 / MediaExtractor
  -> MediaCodec Decoder
  -> SurfaceTexture
  -> OES Texture
  -> RenderGraph
  -> GLSurfaceView preview
```

稳定后扩展导出闭环：

```text
Timeline / ExportClock
  -> RenderGraph Offscreen FBO
  -> EncoderSurface
  -> MediaCodec Encoder
  -> MediaMuxer
  -> MP4
```

## MVP 最小闭环

MVP 只要求完成实时预览最小链路：

1. 加载一个本地 MP4 素材。
2. 使用 MediaExtractor + MediaCodec 解码视频。
3. 通过 SurfaceTexture 输出到 OES Texture。
4. 在 RenderThread 中使用 OpenGL ES 绘制 OES Texture。
5. 通过 RenderGraph 的最小 pass 输出到 GLSurfaceView。
6. 支持 prepare / play / pause / seek / stop / release。
7. 输出基础 FrameStats、状态机日志、错误码。
8. 能稳定运行 1080p 基准素材。

## MVP 范围

### 必须做

- Android sample app 最小入口。
- Kotlin SDK Facade 的最小控制 API。
- Runtime control thread 与状态机 gate。
- DecodeThread 解码与 flush/seek 控制。
- RenderThread GL/EGL/SurfaceTexture ownership。
- OES Texture 到屏幕的最小 RenderGraph。
- 基础 AV clock anchor 与 seek recover。
- 基础错误码、日志、FrameStats。

### 暂不做

- 完整 NLE 编辑器。
- 多轨复杂编辑 UI。
- 海量模板系统。
- Vulkan。
- Protobuf。
- ECS。
- Plugin system。
- 重型 AI 特效，例如美颜、分割、超分。
- 商业化 SDK 完整封装。
- P0/P1 阶段的离线导出完整实现。
- P0/P1 阶段的复杂 Shader 特效链。

## 已确定的设计

| 领域 | 已确定决策 |
|---|---|
| 技术基线 | Kotlin + JNI + C++17 + OpenGL ES 3.x + MediaCodec |
| 架构分层 | SDK Facade / Runtime / Engine Core |
| MVP 链路 | MP4 -> MediaCodec -> SurfaceTexture -> OES Texture -> RenderGraph -> GLSurfaceView |
| 线程归属 | UI 只发命令；Runtime 管状态；DecodeThread 管 codec；RenderThread 管 GL/EGL/SurfaceTexture |
| 状态机 | 使用细分 seek 状态，不再使用粗粒度 `Seeking` |
| GL 资源 | GL/EGL/SurfaceTexture 只能由 RenderThread 创建、使用、销毁 |
| 渲染 | RenderGraph 作为预览与未来导出的统一渲染路径 |
| 销毁 | release 必须遵循固定销毁顺序 |
| 指标 | 使用 P50/P95/P99、drift、drop、recover、crash-free 等量化指标 |

## 仍在讨论或后置的设计

这些设计不进入 MVP，不阻塞最小闭环：

- GPU compatibility matrix 的完整机型分层。
- 4K 策略的实际优化实现。
- FBO pool 的精细化复用策略。
- 完整 colorspace 策略。
- SurfaceTexture 机型差异专项适配。
- 多 Layer 动画与复杂合成能力。
- 离线导出的完整编码链路。
- Shader 特效链的具体特效集合。

## 当前阶段最应该做的事情

当前最应该做的是 **P0 文档与 Runtime Contract 冻结**：关闭 C++ 基线、seek 状态机、AV sync 阈值、release order、JNI contract、指标口径等阻塞冲突，然后再进入 P1 MVP 编码。

## 文档使用规则

1. 本目录是新的单一事实源。
2. ADR 记录不可轻易改变的架构决策。
3. SPECS 记录可验收的规格。
4. TASKS 记录按阶段增量交付的任务。
5. 旧文档中与本目录冲突的内容以本目录为准。
6. 新增需求必须先进入 ROADMAP 的后置范围，不能直接进入 MVP。
