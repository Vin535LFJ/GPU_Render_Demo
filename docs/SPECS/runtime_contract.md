# Runtime Contract Spec

## 目标

定义 MVP 编码前必须遵守的 Runtime 行为契约，确保状态、线程、资源、错误和指标可验证。

## 控制状态

Runtime 状态集合：

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

## 状态转换

允许转换：

```text
IDLE -> PREPARED -> PLAYING
PLAYING -> PAUSED -> PLAYING
PLAYING/PAUSED -> SEEK_REQUESTED -> SEEK_FLUSHING -> SEEK_PRIMING -> SEEK_RECOVERING -> PLAYING/PAUSED
PLAYING -> EOS_HOLD -> PAUSED/STOPPED
any non-RELEASED -> STOPPED -> RELEASED
```

非法转换：

- 返回结构化错误码。
- 记录 warning log。
- 不允许隐式纠正。
- 不允许自动 stop + reprepare。

## 控制命令规格

| 命令 | 输入 | 输出 | 允许状态 | 不负责什么 | 验收标准 |
|---|---|---|---|---|---|
| prepare | media source, surface config | PREPARED or error | IDLE | 不自动 play | 资源准备完成；RenderThread/DecodeThread 子步骤可观测 |
| play | none | PLAYING or error | PREPARED/PAUSED/SEEK_RECOVERING | 不绕过 recover 条件 | 时钟推进；render submit 开启 |
| pause | none | PAUSED or error | PLAYING | 不 release 资源 | 时钟冻结；禁止新 render submit |
| seek | targetUs | PLAYING/PAUSED or error | PLAYING/PAUSED | 不直接跳到 PLAYING | 必经四段 seek 状态；500ms 超时降级 PAUSED |
| stop | none | STOPPED or error | any non-RELEASED | 不销毁全部资源 | submit 静默；codec flush；queue clear |
| release | none | RELEASED or error | STOPPED | 不允许 release 后复用 handle | 严格 8 步 destroy order |

## Seek / Flush / Recover

- `SEEK_REQUESTED`：接收 seek，立即阻断新 render submit。
- `SEEK_FLUSHING`：DecodeThread 执行 codec.flush 和 extractor.seekTo。
- `SEEK_PRIMING`：清空旧 frame queue，等待至少 1 帧有效视频帧。
- `SEEK_RECOVERING`：重锚 AudioClock，进入受控恢复窗口。
- Recover 条件：至少 1 帧有效视频帧 + clock anchor 完成。
- Recover timeout：500ms 未恢复则进入 PAUSED 并上报 recover-timeout。

## EOS

- Decode EOS 后进入 `EOS_HOLD`。
- 保留最后有效帧。
- 禁止继续 dequeue input。
- 等待用户 seek/play/stop。

## JNI Contract

- JNI 方法必须 state-gated。
- 同一 engine handle 上 JNI 调用非重入。
- engine handle 在 init/create 后有效，release 后永久无效。
- JNI 返回结构化错误码。
- Java exception 不作为 runtime 控制流。
- callback 线程策略必须唯一：主线程或专用 callback thread 二选一，不允许混用。
- JNI versioning 必须能拒绝不兼容调用方。

## 错误策略

| 错误类型 | 策略 |
|---|---|
| Invalid state | 返回错误码 + warning log，不改变状态 |
| Recover timeout | 降级 PAUSED + error callback |
| Codec fatal | stop submit，进入 STOPPED 或 fatal release-required |
| GL/EGL context lost | 停 submit，按 rebuild 规则重建资源 |
| Release order violation | fatal log，engine 标记不可继续使用 |
| Callback reentry | 拒绝或排队，禁止同步重入 Runtime 控制路径 |

## 验收标准

- 所有控制命令都有合法/非法状态测试。
- seek 状态序列可通过日志验证。
- release 顺序可通过 trace 验证。
- JNI 错误码稳定且可文档化。
- 状态机错误不会被吞掉或隐式修复。
