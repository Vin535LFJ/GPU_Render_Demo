# Roadmap

## 路线原则

1. 先冻结 Runtime Contract，再写代码。
2. 先完成最小预览链路，再做同步优化。
3. 先稳定播放和 seek，再做特效和导出。
4. 每个阶段必须有可运行或可验证输出。
5. 不关闭 blocker 不进入下一阶段。

## 阶段总览

| 阶段 | 名称 | 目标 | 是否编码 |
|---|---|---|---|
| P0 | Baseline Freeze | 统一文档、关闭 C0 blocker、冻结 Runtime Contract | 否 |
| P1 | MVP Preview Loop | 打通 MP4 到 OES 到 RenderGraph 到屏幕 | 是 |
| P2 | Sync & Stability | 完成 AV sync、seek recover、release 稳定性 | 是 |
| P3 | Effects & Export | 增加基础特效与离线导出 | 是 |
| P4 | Performance & Compatibility | 1080p 稳定、4K 降级、机型兼容 | 是 |

## P0：Baseline Freeze

- **目标**：形成单一事实源，关闭阻塞冲突。
- **输入**：旧 README、demo_docs、docs/freeze、docs/roadmap。
- **输出**：PROJECT_VISION、SYSTEM_ARCHITECTURE、ROADMAP、ADR、SPECS、TASKS。
- **必须完成**：统一 C++17、状态机、AV sync 阈值、release order、JNI contract、指标口径、ownership。
- **明确不做**：不编码、不新增功能、不扩架构、不拆复杂实现细节。
- **验收标准**：所有 C0 blocker 有关闭策略；MVP 范围清晰；旧冲突设计被删除或归档。
- **风险**：继续保留多套事实源导致实现返工。
- **Gate**：Runtime Contract Spec 通过后才能进入 P1。

## P1：MVP Preview Loop

- **目标**：打通最小实时预览链路。
- **输入**：冻结后的 Runtime Contract 与 Architecture。
- **输出**：可运行 sample，MP4 解码到 OES Texture 并通过 RenderGraph 上屏。
- **必须完成**：prepare/play/pause/seek/stop/release、DecodeThread、RenderThread、OES pass、FrameStats。
- **明确不做**：不做导出、不做复杂特效、不做完整多层编辑、不做 4K 优化。
- **验收标准**：1080p 基准素材可播放；基础 seek 可恢复；非法状态调用有错误码；release 不黑屏不崩溃。
- **风险**：SurfaceTexture 和 EGL 生命周期处理不当导致黑屏或崩溃。
- **Gate**：最小链路稳定且 metrics 可采集后进入 P2。

## P2：Sync & Stability

- **目标**：让播放、seek、pause/resume、EOS、release 可稳定验证。
- **输入**：P1 最小链路。
- **输出**：AV drift 纠偏、seek recover、EOS_HOLD、release destroy order 测试。
- **必须完成**：Audio master clock、drift 三段策略、防振荡、高频 seek、前后台切换、错误恢复。
- **明确不做**：不扩复杂特效；不实现完整导出。
- **验收标准**：AV drift P95/P99 达标；seek recover P95/P99 达标；长稳无崩溃。
- **风险**：纠偏策略振荡；状态竞争；资源释放顺序被破坏。
- **Gate**：同步和稳定性指标达标后进入 P3。

## P3：Effects & Export

- **目标**：在稳定 Runtime 上增加基础效果和离线导出。
- **输入**：P2 稳定链路。
- **输出**：基础调色、羽化/阴影/简单转场、Offscreen FBO、EncoderSurface、MP4 导出。
- **必须完成**：预览/导出复用 RenderGraph；导出进度；取消；失败错误码。
- **明确不做**：不做模板系统；不做复杂 NLE；不做重型 AI。
- **验收标准**：预览和导出帧级差异在阈值内；导出文件可播放；取消和失败路径可观测。
- **风险**：预览/导出分裂；FBO 复用错误；编码兼容问题。
- **Gate**：导出一致性和稳定性达标后进入 P4。

## P4：Performance & Compatibility

- **目标**：优化性能并建立兼容策略。
- **输入**：P3 功能闭环。
- **输出**：1080p 稳定基线、4K 降级策略、GPU compatibility matrix。
- **必须完成**：texture/FBO pool 优化、P50/P95/P99 指标、memory peak、crash-free、机型分层。
- **明确不做**：不牺牲 Runtime Contract 换取局部性能。
- **验收标准**：核心指标达标；降级策略可配置；兼容问题有记录和回归用例。
- **风险**：为单机型特例破坏通用生命周期和 ownership。
- **Gate**：可进入后续产品化评估。

## 当前 Blocker

| Blocker | 关闭条件 |
|---|---|
| C++11/C++17 冲突 | 全文统一为 C++17 |
| 粗粒度 Seeking | 全文统一为细分 seek 状态机 |
| AV sync 阈值不一致 | 全文统一为 20ms/80ms 三段策略 |
| release 顺序不完整 | 全文统一为 8 步 destroy order |
| JNI contract 不完整 | 补齐 versioning、错误码、线程、handle、非重入 |
| 指标口径不统一 | 统一 P50/P95/P99、drift、drop、recover、crash-free |
| ownership 不完整 | 所有资源有唯一 owner 和 forbidden call set |
| 旧文档冲突 | 被替代内容删除或归档为非事实源 |

## 可以删除的设计

- C++11 技术基线。
- 粗粒度 `Seeking` 单状态描述。
- `<50ms` / `>100ms` 的旧 AV sync 粗阈值。
- 模糊 release 描述。
- 只列 JNI 方法、不定义 contract 的接口描述。
- 把当前项目描述成完整 NLE 或完整商用编辑器的表述。
- 在 P0/P1 引入 Vulkan、protobuf、ECS、plugin system、重型 AI 的设计。

## 未来再考虑的设计

- Vulkan 后端。
- 完整多轨编辑模型。
- 模板系统。
- ECS 或 plugin system。
- Protobuf 协议化。
- 复杂 AI 特效。
- 完整 colorspace 管线。
- 完整 GPU compatibility matrix。
