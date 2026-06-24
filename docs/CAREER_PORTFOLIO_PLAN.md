# Career Portfolio Plan: AI Agent Delivery + GPU/AV/OpenGL Engineering

## 1. 结论：先规整文档，再进入下一阶段代码开发

当前项目已经可以运行，下一阶段不建议马上堆功能。更好的顺序是：

1. **先把文档规整成作品集事实源**：让项目清楚表达「需求拆解 → 架构设计 → 代码开发 → 测试上线」的完整闭环。
2. **再把下一阶段任务拆成 AI Agent 可执行任务卡**：每个任务有依赖、检查点、验收标准和测试方式。
3. **最后再开发 P2/P3 能力**：例如 AV sync 稳定性、RenderGraph 特效、导出、性能指标。

这样做的原因是：该岗位不是单纯考察“能不能写代码”，而是考察你是否能用 AI Agent 独立承担从业务目标到上线质量的完整结果。因此，文档需要证明你的工作方式，而不仅仅记录技术细节。

## 2. 项目对外定位

对外不要把本项目描述为“小型 OpenGL demo”，而应该描述为：

> 一个 AI-assisted Android Native GPU Video Runtime 项目：以 AI Agent 协作方式完成需求拆解、架构设计、代码开发、测试验证和阶段上线；技术上覆盖 Android 音视频播放、MediaCodec、SurfaceTexture/OES 纹理、OpenGL ES RenderGraph、Shader 特效、AV sync、JNI/C++ runtime ownership、性能指标与兼容性验证。

这个定位同时服务两类岗位：

1. **AI Agent 全链路交付岗位**：展示你能定义需求、拆分任务、指挥 agent、验证结果、沉淀复用资产。
2. **图像渲染 / 音视频播放器 / 特效 / OpenGL 工程师岗位**：展示你有实际 AV pipeline、GPU texture rendering、shader、RenderGraph、同步和性能经验。

## 3. 岗位职责到项目证据的映射

| 岗位要求 | 项目中应该展示的证据 | 当前/建议文档位置 |
|---|---|---|
| 不依赖别人写好 PRD，能拆解业务目标 | 将“做一个 GPU 视频 runtime”拆成用户故事、阶段目标、验收标准 | `docs/PROJECT_VISION.md`, `docs/ROADMAP.md`, `docs/TASKS/` |
| 通过 AI 拆解问题、定义优先级 | P0-P4 分期、blocker、gate、must-have / won't-do | `docs/ROADMAP.md`, 本文档第 8 节 |
| 输出用户故事与验收标准 | 每个阶段任务包含用户价值、输入、输出、验收标准 | 建议统一改造 `docs/TASKS/` |
| 独立设计核心流程、交互及异常处理规则 | Runtime 状态机、seek/EOS/release、错误码、线程 ownership | `docs/SPECS/runtime_contract.md`, `docs/ADR/` |
| 把需求转化为 Agent 可执行任务 | 任务卡包含 owner、上下文、依赖、检查点、预期文件、测试命令 | 本文档第 6 节，后续 `docs/AI_AGENT_PLAYBOOK.md` |
| 管理依赖、检查点和结果验证 | phase gate、validation spec、metrics、release checklist | `docs/SPECS/validation.md`, `docs/ROADMAP.md` |
| 指挥多个 AI Agent 并行完成开发和本地测试 | Runtime / Decode / Render / Validation / Docs agent 分工 | 本文档第 7 节 |
| 对最终交付质量负责 | TDD、本地测试、人工验收、指标验收、问题修复闭环 | `docs/SPECS/validation.md`, 后续 `docs/VALIDATION_REPORTS/` |
| 沉淀 AI 资产 | prompts、skills、review checklist、debug playbook | 后续 `.ai/` 或 `docs/AI_AGENT_PLAYBOOK.md` |
| 同步生成架构文档 | System Architecture、ADR、Specs | `docs/SYSTEM_ARCHITECTURE.md`, `docs/ADR/`, `docs/SPECS/` |

## 4. 文档改造总框架

为了避免“文档很多但读者抓不住重点”，建议把文档整理成四层。

### 4.1 入口层：让面试官 1 分钟知道项目是什么

保留并强化：

- `README.md`
- `docs/DOCUMENTATION_INDEX.md`

入口层只回答：

- 这个项目是什么？
- 当前已经跑到什么阶段？
- 核心技术栈是什么？
- 面试官应该先读哪几篇？
- 如果要深挖技术，应该去哪看？

### 4.2 交付层：证明你符合岗位定位

核心文档：

- `docs/CAREER_PORTFOLIO_PLAN.md`
- `docs/ROADMAP.md`
- 未来可新增：`docs/AI_AGENT_PLAYBOOK.md`
- 未来可新增：`docs/INTERVIEW_CASE_STUDY.md`

交付层回答：

- 业务目标如何拆成阶段目标？
- 每阶段的优先级和 gate 是什么？
- AI Agent 如何分工？
- 如何验证 agent 的输出？
- 如何从一次交付中沉淀 reusable skill？

### 4.3 技术事实层：证明技术路线是真的

保留：

- `docs/PROJECT_VISION.md`
- `docs/SYSTEM_ARCHITECTURE.md`
- `docs/SPECS/`
- `docs/ADR/`

技术事实层回答：

- Runtime 状态机怎么设计？
- DecodeThread / RenderThread / Runtime Control Thread 的 ownership 怎么划分？
- OES texture、RenderGraph、FBO、shader pass 怎么组织？
- AV sync 阈值、seek recovery、release order 怎么验证？

### 4.4 学习/辅助层：降低噪音，不作为主线事实源

保留但降级：

- `docs/VIDEO_TEXTURE_RENDERING_LEARNING_GUIDE.md`
- `docs/DEBUG_DOCUMENTATION.md`

它们可以用于学习和排障，但不应该成为面试主线。面试主线必须围绕入口层、交付层和技术事实层展开。

## 5. 「需求拆解 → 架构设计 → 代码开发 → 测试上线」闭环表达

建议在 README 和面试 case study 中固定使用下面这条主线。

### 5.1 需求拆解

业务目标：构建一个可运行、可扩展、可验证的 Android GPU 视频 runtime。

拆解为用户故事：

- 作为使用者，我可以打开一个 MP4 并看到稳定预览。
- 作为使用者，我可以 play/pause/seek/release，非法操作有明确错误。
- 作为创作者，我可以在视频上开启基础 shader 效果。
- 作为开发者，我可以看到 FPS、frame time、drop、seek recover 等指标。
- 作为工程负责人，我可以通过 validation report 判断是否能进入下一阶段。

### 5.2 架构设计

核心架构主线：

```text
Kotlin App / SDK Facade
  -> Runtime Controller
  -> DecodeThread: MediaExtractor + MediaCodec
  -> Texture Bridge: SurfaceTexture + OES Texture
  -> RenderThread / Native Engine
  -> RenderGraph: OES Pass -> 2D/FBO Pass -> Effects -> Present/Export
  -> Metrics / Validation
```

核心设计原则：

- UI 不直接操作 GL/Codec。
- Runtime 控制状态机和命令 gate。
- DecodeThread 独占 MediaCodec/MediaExtractor lifecycle。
- RenderThread 独占 EGL、SurfaceTexture updateTexImage、GL resources。
- RenderGraph 同时服务 preview 和未来 export，避免预览/导出效果分裂。

### 5.3 代码开发

下一阶段开发不应该笼统写“继续优化”，而应该拆为 agent 可执行任务：

- Runtime agent：状态机、错误码、非法调用测试。
- Decode agent：prepare、seek、flush、EOS、release。
- Render agent：OES sampling、FBO、shader effects、RenderGraph pass。
- Validation agent：unit tests、instrumentation checklist、metrics report。
- Docs agent：ADR、case study、demo script、release note。

### 5.4 测试上线

测试上线不一定是商业发布，也可以定义为阶段 gate：

- 本地构建通过。
- 单元测试通过。
- 真机/模拟器播放通过。
- seek、pause/resume、EOS、release 通过人工验收。
- 指标满足当前阶段阈值。
- validation report 记录环境、命令、结果、问题和后续动作。

## 6. Agent 可执行任务卡模板

建议把 `docs/TASKS/` 逐步统一成下面格式：

```text
# Task: <任务名>

## Business Goal
这个任务解决什么业务/体验问题。

## User Story
作为 <角色>，我希望 <能力>，以便 <价值>。

## Scope
### In Scope
- 必须完成的内容。

### Out of Scope
- 明确不做的内容，防止 AI 过度发挥。

## Dependencies
- 依赖哪些 task/spec/ADR。

## Agent Ownership
- Runtime agent / Decode agent / Render agent / Validation agent / Docs agent。
- 允许修改哪些文件。
- 禁止修改哪些文件。

## Acceptance Criteria
- 可验证的完成标准。

## Checkpoints
- Checkpoint 1: 设计确认。
- Checkpoint 2: 最小实现。
- Checkpoint 3: 测试与修复。
- Checkpoint 4: 文档和验收报告。

## Verification
- 自动化测试命令。
- 人工验证步骤。
- 需要采集的指标。

## Review Checklist
- 状态机是否被绕过？
- 线程 ownership 是否被破坏？
- release 顺序是否安全？
- 是否有测试或明确的环境限制说明？
```

这个模板本身就能证明你具备“把需求转化为 Agent 可执行任务，管理依赖、检查点和结果验证”的能力。

## 7. 多 Agent 编排方案

下一阶段建议按模块并行，而不是让多个 agent 同时改同一批文件。

| Agent | Owner 范围 | 典型任务 | 验收方式 |
|---|---|---|---|
| Runtime agent | Kotlin runtime / state machine | command gate、error code、lifecycle | unit tests、非法调用用例 |
| Decode agent | MediaExtractor / MediaCodec | prepare、flush、seek、EOS、release | 播放、seek、EOS 人工验收 |
| Render agent | C++/OpenGL ES / RenderGraph | OES pass、FBO、shader effects | 画面正确、GL error、frame time |
| Validation agent | tests / scripts / reports | 测试命令、指标采集、validation report | CI、本地报告、截图/录屏 |
| Docs agent | docs / ADR / case study | 架构说明、面试脚本、release note | 文档与实现一致性 review |

协作规则：

1. 每个 agent 只拥有一个清晰模块。
2. 每个任务必须写明可修改文件和 forbidden changes。
3. 每个任务必须先读相关 specs/ADR。
4. 每个阶段结束必须有 validation report。
5. human owner 负责最终架构一致性和质量判断。

## 8. 下一阶段技术路线

### P1.5：文档与交付体系整理

目标：把当前可运行 demo 包装成能面试展示的工程项目。

交付物：

- `docs/DOCUMENTATION_INDEX.md`
- 更新后的 `docs/CAREER_PORTFOLIO_PLAN.md`
- 统一格式后的 `docs/TASKS/`
- 未来 `docs/AI_AGENT_PLAYBOOK.md`
- 未来 `docs/INTERVIEW_CASE_STUDY.md`

验收标准：

- 面试官能在 5 分钟内理解项目定位、当前状态、技术路线和交付方式。
- 每个下一阶段任务都能直接交给 AI Agent 执行。
- 文档不再互相重复主线内容。

### P2：播放稳定性与 AV Sync

目标：把“能播放”提升为“稳定播放器 runtime”。

重点：

- Audio master clock。
- drift 计算与纠偏。
- seek recovery。
- pause/resume。
- EOS hold。
- release destroy order 验证。

面试价值：播放器、音视频 SDK、runtime stability。

### P3：RenderGraph 特效与导出

目标：把“视频上屏”提升为“图像渲染/特效 pipeline”。

重点：

- OES to 2D pass。
- FBO pass。
- LUT / blur / vignette / transition。
- preview/export 复用 RenderGraph。
- EncoderSurface + MediaMuxer 导出。

面试价值：图像渲染、OpenGL、特效、短视频编辑。

### P4：性能与兼容性

目标：证明生产化能力。

重点：

- P50/P95/P99 frame time。
- dropped frames。
- memory peak。
- GL capability probe。
- 720p/1080p/4K 分级策略。
- compatibility matrix。

面试价值：高级工程质量、线上稳定性、性能优化。

## 9. README 应该怎么改

README 不要写成长文档，建议只保留这些内容：

1. 30 秒项目定位。
2. 当前已完成能力。
3. 当前阶段和下一阶段。
4. 技术栈。
5. 架构图。
6. 快速运行方式。
7. 文档入口：面试官、开发者、技术深挖三条路径。
8. Demo 截图/GIF 和指标摘要。

## 10. 简历和面试表达

简历可以写：

- 使用 Kotlin + JNI + C++17 + OpenGL ES + MediaCodec 构建 Android Native GPU Video Runtime，完成 MP4 解码、OES 纹理上屏、RenderGraph 渲染和播放控制闭环。
- 以 AI Agent 工作流推进项目：从业务目标拆解、用户故事、验收标准、架构 ADR、任务卡、并行开发、TDD 验证到阶段验收报告。
- 设计 Runtime 状态机、线程 ownership、seek/EOS/release 异常处理规则，保证音视频 runtime 的可验证稳定性。
- 规划并推进 Shader 特效、AV sync、离线导出、性能指标和兼容性矩阵，覆盖图像渲染、播放器和 OpenGL 工程能力。

面试时可以这样讲：

> 这个项目不是单纯写一个播放器 demo。我把它当成一次 AI Agent 驱动的完整交付来做：先定义业务目标和用户故事，再冻结 Runtime contract、线程 ownership 和 RenderGraph 架构，然后把功能拆给不同 agent 并行实现，最后用单测、真机验证和指标报告做阶段 gate。技术上它覆盖 MediaCodec、SurfaceTexture/OES、OpenGL ES、Shader、AV sync 和未来导出；工程上它展示的是我如何用 AI 完成从需求到上线的闭环。

## 11. 暂不修改代码时的最小行动清单

如果当前阶段只做文档规整，建议按顺序完成：

1. 建立 `docs/DOCUMENTATION_INDEX.md`，先解决“文档太多，不知道看哪里”的问题。
2. 重写 `docs/CAREER_PORTFOLIO_PLAN.md`，让它围绕岗位职责和交付闭环，而不是泛泛职业规划。
3. 更新 `README.md`，把文档入口放清楚。
4. 下一次再统一 `docs/TASKS/` 格式，让每个任务都能直接交给 AI Agent 执行。
5. 再下一次补 `docs/AI_AGENT_PLAYBOOK.md` 和 `docs/INTERVIEW_CASE_STUDY.md`。
