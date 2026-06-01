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
6. [`docs/TASKS/`](docs/TASKS/)：按阶段拆分的增量交付任务。

## 当前阶段

当前处于 **P0：Baseline Freeze**。

P0 的最高优先级是统一文档与 Runtime Contract，关闭 C++ 基线、seek 状态机、AV sync 阈值、release order、JNI contract、指标口径、ownership 等 blocker。

在 P0 完成前，不应进入功能编码。
