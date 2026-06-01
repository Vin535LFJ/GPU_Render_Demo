# P2 Tasks: Sync and Stability

## 目标

在 P1 最小链路上完成音视频同步、seek recover、EOS、release、前后台切换等稳定性闭环。

## 任务清单

- [ ] 实现 Audio master clock。
- [ ] 实现 monotonic fallback。
- [ ] 输出 videoPTS/audioClock drift。
- [ ] 实现 20/80ms 三段纠偏策略。
- [ ] 实现防振荡和连续 drop 上限。
- [ ] 实现 seek recover 500ms timeout。
- [ ] 实现 EOS_HOLD。
- [ ] 实现前后台切换恢复。
- [ ] 实现 context lost rebuild。
- [ ] 增加长稳和高频 seek 验证。

## 验收标准

- AV drift `P95 <= 40ms`, `P99 <= 80ms`。
- seek recover `P95 <= 350ms`, `P99 <= 500ms`。
- 常态 drop rate `<= 1.0%`。
- 24h loop/monkey crash-free 达标。
- release destroy order trace 可验证。

## 不做

- 不做导出。
- 不做复杂特效。
- 不做模板系统。
