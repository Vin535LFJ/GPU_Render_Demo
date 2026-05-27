# Validation Strategy

## Validation Dimensions
1. Contract completeness
2. Conflict closure
3. Metrics readiness
4. Stage-gate compliance

## Validation Method
- 文档审计：逐条映射 09/10 -> 00–08。
- 合同审计：每项检查 owner/lifecycle/state/failure 四元组。
- 指标审计：P50/P95/P99 + drift + drop + stability 口径完整性。
- blocker 审计：存在 blocker 则阶段不可退出。

## Evidence Pack
- `docs/freeze/checklist.md`
- `docs/freeze/unresolved_conflicts.md`
- `docs/freeze/runtime_contracts.md`
- `docs/freeze/error_policy.md`

## Exit Criteria by Stage
- Stage 0: C0 conflicts closed.
- Stage 1: contracts validated and measurable.
- Stage 2: blockers cleared and milestone gates signed.
