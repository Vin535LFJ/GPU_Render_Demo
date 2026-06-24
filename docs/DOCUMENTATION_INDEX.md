# Documentation Index

## 1. 文档规整目标

本仓库的文档不应该只是“资料越多越好”，而应该服务两个目标：

1. **交付证明**：证明项目按「需求拆解 → 架构设计 → 代码开发 → 测试上线」闭环推进。
2. **面试证明**：证明候选人能够用 AI Agent 独立定义需求、拆解优先级、编排开发、验证质量，并对最终结果负责。

因此，文档分为四层：入口层、交付层、技术事实层、归档/学习层。面试或协作时优先阅读入口层和交付层；只有需要证明技术细节时再进入技术事实层。

## 2. 推荐阅读路径

### 2.1 面向招聘方 / 面试官

1. `README.md`：30 秒了解项目定位、当前阶段和阅读入口。
2. `docs/CAREER_PORTFOLIO_PLAN.md`：理解项目如何对应 AI Agent 交付岗位与图像渲染/音视频/OpenGL 岗位。
3. `docs/PROJECT_VISION.md`：理解项目边界、MVP 和非目标。
4. `docs/ROADMAP.md`：理解阶段计划和下一步开发。
5. `docs/SYSTEM_ARCHITECTURE.md`：理解核心架构和线程/模块边界。

### 2.2 面向开发者 / AI Agent

1. `docs/CAREER_PORTFOLIO_PLAN.md`：确认本阶段要交付的业务目标、验收标准和 agent 分工方式。
2. `docs/SPECS/`：读取当前任务相关规格。
3. `docs/TASKS/`：选择当前阶段任务卡。
4. `docs/ADR/`：确认不可随意推翻的架构决策。
5. 代码实现与本地验证。

### 2.3 面向技术深挖

1. `docs/SYSTEM_ARCHITECTURE.md`：总架构。
2. `docs/SPECS/runtime_contract.md`：Runtime API、状态机、错误码、线程约束。
3. `docs/SPECS/rendering_pipeline.md`：渲染管线和 RenderGraph。
4. `docs/SPECS/av_sync.md`：音视频同步策略。
5. `docs/SPECS/validation.md`：测试与指标口径。
6. `docs/ADR/`：关键架构决策记录。

## 3. 文档分层与职责

| 层级 | 文档 | 作用 | 是否作为事实源 |
|---|---|---|---|
| 入口层 | `README.md` | 项目入口、当前阶段、最短阅读路径 | 是 |
| 入口层 | `docs/DOCUMENTATION_INDEX.md` | 文档地图，降低文档过多带来的阅读成本 | 是 |
| 交付层 | `docs/CAREER_PORTFOLIO_PLAN.md` | 岗位匹配、交付闭环、AI Agent 工作流、下一阶段规划 | 是 |
| 交付层 | `docs/ROADMAP.md` | P0-P4 阶段计划、gate、blocker | 是 |
| 技术事实层 | `docs/PROJECT_VISION.md` | 项目目标、MVP、非目标 | 是 |
| 技术事实层 | `docs/SYSTEM_ARCHITECTURE.md` | 系统架构、线程模型、模块边界 | 是 |
| 技术事实层 | `docs/SPECS/` | 可验证规格：runtime、rendering、AV sync、validation | 是 |
| 技术事实层 | `docs/ADR/` | 已接受的架构决策 | 是 |
| 执行层 | `docs/TASKS/` | 分阶段任务卡和验收口径 | 是 |
| 学习/辅助层 | `docs/VIDEO_TEXTURE_RENDERING_LEARNING_GUIDE.md` | 学习材料和知识路线 | 否，辅助材料 |
| 学习/辅助层 | `docs/DEBUG_DOCUMENTATION.md` | Debug 经验和排障记录 | 否，辅助材料 |

## 4. 文档维护规则

1. **README 只放入口信息**：不承载长篇技术细节。
2. **CAREER_PORTFOLIO_PLAN 只放岗位匹配和交付闭环**：不替代具体技术规格。
3. **ROADMAP 只放阶段计划和 gate**：不重复具体实现方案。
4. **SPECS 只放可验证规则**：每条规格应该能被代码、测试、日志或人工验收验证。
5. **ADR 只记录不可轻易推翻的决策**：不要把 TODO 写成 ADR。
6. **TASKS 只放可执行任务**：每张任务卡都要有依赖、验收标准和验证方式。
7. **学习材料不作为事实源**：如果学习材料与 SPECS/ADR 冲突，以 SPECS/ADR 为准。

## 5. 下一步文档规整建议

在暂不修改代码的前提下，优先完成以下文档改造：

1. 将 `README.md` 改成更强的项目门面：项目一句话、当前状态、可运行能力、技术栈、文档入口。
2. 将 `docs/CAREER_PORTFOLIO_PLAN.md` 作为核心“岗位作品集说明书”。
3. 将 `docs/TASKS/` 统一为 agent 可执行任务卡格式。
4. 将 `docs/SPECS/validation.md` 补齐为本地测试、人工验收、指标验收三类。
5. 后续新增 `docs/INTERVIEW_CASE_STUDY.md` 和 `docs/VALIDATION_REPORTS/`，用于面试展示和阶段验收。
