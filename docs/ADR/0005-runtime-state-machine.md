# ADR-0005: Runtime State Machine

## Status

Accepted

## Context

粗粒度 `Seeking` 无法表达 flush、prime、recover 的生命周期，导致非法调用、恢复超时、EOS、release 都不可验证。

## Decision

Runtime 使用冻结状态机：

- `IDLE`
- `PREPARED`
- `PLAYING`
- `PAUSED`
- `SEEK_REQUESTED`
- `SEEK_FLUSHING`
- `SEEK_PRIMING`
- `SEEK_RECOVERING`
- `EOS_HOLD`
- `STOPPED`
- `RELEASED`

非法转换返回错误码并记录 warning log，不允许隐式纠正。

## Consequences

- 旧文档中的单一 `Seeking` 删除。
- seek 必须经过 REQUESTED -> FLUSHING -> PRIMING -> RECOVERING。
- recover timeout 后降级到 PAUSED 并上报错误。
