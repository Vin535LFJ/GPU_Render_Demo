# P0 Tasks: Baseline Freeze

## 目标

在不编码、不扩展架构的前提下，把项目文档收敛为可实施的单一事实源。

## 任务清单

- [ ] 确认新文档结构为唯一事实源。
- [ ] 删除或归档旧文档中重复、过期、冲突内容。
- [ ] 全文统一技术基线为 C++17。
- [ ] 全文统一 Runtime 状态机。
- [ ] 全文统一 AV sync drift 阈值。
- [ ] 全文统一 release destroy order。
- [ ] 补齐 JNI contract。
- [ ] 补齐 ownership matrix。
- [ ] 补齐 P50/P95/P99 指标口径。
- [ ] 确认 MVP 不包含导出、复杂特效、NLE、Vulkan、ECS、plugin system、protobuf。

## 验收标准

- docs/PROJECT_VISION.md 能回答项目做什么和不做什么。
- docs/SYSTEM_ARCHITECTURE.md 能回答模块职责与边界。
- docs/ROADMAP.md 能回答分阶段怎么做。
- docs/ADR/ 能解释关键架构决策。
- docs/SPECS/ 能作为编码验收依据。
- 不存在与 C++17、细分 seek 状态机、20/80ms drift、8 步 release order 冲突的主文档。

## 不做

- 不写 Kotlin/C++ 代码。
- 不新增产品需求。
- 不引入新架构。
- 不拆 P1 以后的细粒度工程任务。
