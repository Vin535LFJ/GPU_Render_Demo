# Validation Spec

## 目标

定义每个阶段可执行、可量化的验收标准，避免使用“流畅”“稳定”等主观描述作为完成标准。

## 功能验收

| 项目 | 标准 |
|---|---|
| prepare | 合法素材进入 PREPARED；非法素材返回错误码 |
| play | 从 PREPARED/PAUSED 进入 PLAYING |
| pause | PLAYING 进入 PAUSED；最后一帧保留 |
| seek | 必经四段 seek 状态；recover 成功或 timeout 降级 PAUSED |
| stop | 停止 submit，codec flush，queue clear |
| release | 严格 8 步 destroy order |
| EOS | 进入 EOS_HOLD，保留最后有效帧 |

## 性能验收

| 指标 | 目标 |
|---|---|
| FPS | 1080p 预览 `P50 >= 58`, `P95 >= 55` |
| Frame drop | 常态播放 `<= 1.0%` |
| AV drift | `P95 <= 40ms`, `P99 <= 80ms` |
| Seek recover | `P95 <= 350ms`, `P99 <= 500ms` |
| Memory peak | 1080p 连续播放 30min 峰值波动 `<= 15%` |
| Crash-free | 24h monkey/循环播放 `100%` |

## 稳定性场景

- 正常播放 30min。
- 高频 seek。
- pause/resume。
- 前后台切换。
- EOS 后 seek/play/stop。
- release 后重复调用控制命令。
- 损坏流或不支持格式。
- context lost/recreate。

## 指标输出要求

- render time P50/P95/P99。
- decode time P50/P95/P99。
- updateTexImage time P50/P95/P99。
- frame drop count/rate。
- AV drift P50/P95/P99。
- seek recover time P50/P95/P99。
- error code count。
- state transition trace。

## 阶段 Exit Criteria

### P0

- Runtime Contract Spec 完整。
- C0 blocker 有关闭结论。
- 文档单一事实源建立。

### P1

- MVP 预览链路可运行。
- 状态机、release、基础错误码可验证。
- FrameStats 可输出。

### P2

- AV sync 与 seek recover 指标达标。
- 长稳、前后台、EOS、release 测试通过。

### P3

- 基础特效可运行。
- 离线导出可生成 MP4。
- 预览/导出一致性可量化。

### P4

- 1080p 稳定达标。
- 4K 降级策略可验证。
- 兼容矩阵建立。
