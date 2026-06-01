# AV Sync Spec

## 目标

定义音视频同步和 seek recover 的最小可实现策略，保证 MVP 后续可稳定扩展到动画帧和导出。

## 输入

- Audio timestamp 或 monotonic fallback。
- Video PTS。
- SurfaceTexture timestamp。
- seek targetUs。
- Decode/render metrics。

## 输出

- timeline time。
- drift：`videoPTS - audioClock`。
- correction decision。
- drop/dup/smooth-adjust metrics。

## 依赖

- Runtime 状态机。
- DecodeThread PTS。
- RenderThread frame consumption。
- Metrics。

## 不负责什么

- 不直接 flush codec。
- 不直接销毁 GL 资源。
- 不绕过 seek recover 状态机。
- 不在 P1 阶段实现复杂变速编辑。

## Drift 策略

| 区间 | 条件 | 行为 |
|---|---|---|
| 稳定区 | `|drift| < 20ms` | 不调速，不丢帧 |
| 平滑纠偏区 | `20ms <= |drift| <= 80ms` | 只调速 |
| 强纠偏区 | `|drift| > 80ms` | 允许受控丢/追帧 |

## Smooth Adjust

- 视频慢时，播放速率可提升到 `1.00 ~ 1.03`。
- 视频快时，播放速率可降低到 `0.97 ~ 1.00`。
- 单次速率变更步长不超过 `0.005`。
- 每 100ms 最多调整一次。

## 防振荡

- 连续 drop 上限：3 帧。
- 达到上限后强制进入 200ms 平稳窗口。
- 300ms 内禁止“调速 -> 丢帧 -> 调速”重复切换超过 2 次。
- seek recover 后前 500ms 禁止 drop，只允许调速。

## Recover

- seek 成功后以 `targetUs` 重置 anchor。
- 若音频设备重建，以第一帧可用 audio timestamp 重新锚定。
- 重新锚定后 300ms 内使用平滑区规则，禁用强纠偏。

## 验收标准

- drift metrics 输出 P50/P95/P99。
- `P95 <= 40ms`，`P99 <= 80ms`。
- seek recover 后 500ms 内无 drop。
- 连续 drop 不超过 3 帧。
- 防振荡触发可观测。
