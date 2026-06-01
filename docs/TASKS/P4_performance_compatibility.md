# P4 Tasks: Performance and Compatibility

## 目标

在功能闭环后优化性能并建立兼容策略。

## 任务清单

- [ ] 建立 1080p 性能基线。
- [ ] 建立 FBO pool 和 texture pool 优化。
- [ ] 建立 memory peak 监控。
- [ ] 建立 4K 降级策略。
- [ ] 建立 GPU compatibility matrix。
- [ ] 建立机型分层策略。
- [ ] 建立兼容问题回归素材集。

## 验收标准

- 1080p FPS `P50 >= 58`, `P95 >= 55`。
- frame drop `<= 1.0%`。
- memory peak 波动 `<= 15%`。
- 4K 降级策略可配置并可验证。

## 不做

- 不为了单机型绕过 Runtime Contract。
- 不牺牲 release order 或 thread ownership 换取性能。
