# GPU Render Demo

本仓库的当前事实源已经收敛到 `docs/` 下的新软件工程文档体系。

## 项目一句话描述

这是一个 **Android Native GPU Video Runtime Engine Demo**：用 Kotlin + JNI + C++17 + OpenGL ES 3.x + MediaCodec 打通 MP4 解码、OES 纹理上屏、RenderGraph 渲染、音视频同步，并在稳定后扩展基础特效与离线导出的最小闭环。

## 推荐阅读顺序

1. [`docs/PROJECT_VISION.md`](docs/PROJECT_VISION.md)：项目目标、MVP、非目标、当前阶段。
2. [`docs/SYSTEM_ARCHITECTURE.md`](docs/SYSTEM_ARCHITECTURE.md)：系统架构、线程模型、模块边界、ownership。
3. [`docs/ROADMAP.md`](docs/ROADMAP.md)：P0 到 P4 的阶段计划、blocker、删除/后置设计。
4. [`docs/ADR/`](docs/ADR/)：已接受的架构决策。
5. [`docs/SPECS/`](docs/SPECS/)：运行时、渲染、同步、验收规格。
6. [`docs/VIDEO_TEXTURE_RENDERING_LEARNING_GUIDE.md`](docs/VIDEO_TEXTURE_RENDERING_LEARNING_GUIDE.md)：视频纹理化、OpenGL ES、Shader 和音视频处理学习指南。
7. [`docs/TASKS/`](docs/TASKS/)：按阶段拆分的增量交付任务。

## 当前阶段

当前处于 **P1：MVP Preview Loop 稳定化**。

P0 文档与 Runtime Contract 已基本冻结；当前工作重心是把 P1 的最小预览链路做成可稳定验证的播放闭环：MP4 解码、OES 上屏、基础播放控制、seek/EOS/release 行为和基础指标。

在 P1 验收前，不应进入复杂特效、离线导出或 4K/兼容性优化。
