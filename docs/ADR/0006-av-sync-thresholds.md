# ADR-0006: AV Sync Drift Thresholds

## Status

Accepted

## Context

旧文档中存在 `<50ms`、`>100ms` 等粗粒度阈值，无法和指标验收、纠偏策略、防振荡规则统一。

## Decision

统一 drift 策略：

- `|drift| < 20ms`：稳定区，不调速、不丢帧。
- `20ms <= |drift| <= 80ms`：平滑纠偏区，只调速。
- `|drift| > 80ms`：强纠偏区，允许受控丢/追帧。

防振荡：

- 连续 drop 上限为 3 帧。
- 达到上限后进入 200ms 平稳窗口。
- 300ms 内禁止调速/丢帧反复切换超过 2 次。
- seek recover 后 500ms 禁止 drop。

## Consequences

- 删除旧粗阈值。
- 指标以 P95/P99 drift 验收。
- 纠偏行为必须输出 metrics。
