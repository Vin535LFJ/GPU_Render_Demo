# ADR-0008: Documentation Single Source of Truth

## Status

Accepted

## Context

仓库旧 Markdown 文档过多，存在重复设计、阶段性草案和已被后续冻结规范推翻的内容。

## Decision

新的事实源限定为：

```text
docs/PROJECT_VISION.md
docs/SYSTEM_ARCHITECTURE.md
docs/ROADMAP.md
docs/ADR/
docs/SPECS/
docs/TASKS/
```

旧文档如与新文档冲突，以新文档和 ADR 为准。

## Consequences

- 不再新增横向重复文档。
- 变更架构先写 ADR。
- 编码任务从 SPECS 和 TASKS 派生。
- 旧文档可删除或归档为历史材料，不作为实施依据。
