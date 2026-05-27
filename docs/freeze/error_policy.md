# Runtime Error Policy (Freeze Backport)

## 1. Error Taxonomy
- `E_INVALID_STATE`：命令在当前状态不允许。
- `E_INVALID_PARAM`：参数无效（如 seek target 越界、null handle）。
- `E_RUNTIME_CONTRACT_BREACH`：ownership/lifecycle/order 违反冻结契约。
- `E_RECOVER_TIMEOUT`：seek recover 超时。
- `E_DECODE_FAILURE`：MediaCodec关键调用失败。
- `E_GL_CONTEXT_LOST`：GL/EGL上下文丢失。
- `E_RESOURCE_EXHAUSTED`：纹理/FBO/内存不足。
- `E_EXPORT_FAILURE`：导出链路失败。

## 2. Subsystem Failure Policy

### 2.1 State machine
- Owner: Runtime control thread.
- Policy: invalid transition => return `E_INVALID_STATE`, write warning log, keep source state unchanged.

### 2.2 Decode / MediaCodec
- Owner: DecodeThread.
- Policy:
  - recoverable dequeue issues => retry with bounded backoff.
  - flush outside allowed states => `E_RUNTIME_CONTRACT_BREACH`.
  - fatal codec failure => force STOPPED and require release path.

### 2.3 GL/EGL/SurfaceTexture
- Owner: RenderThread.
- Policy:
  - cross-thread GL call => `E_RUNTIME_CONTRACT_BREACH` (fatal).
  - context lost => trigger rebuild lifecycle; rebuild failure => STOPPED + release required.

### 2.4 AV sync
- Owner: RenderThread + sync controller.
- Policy:
  - drift oscillation risk => enforce smooth-only window + warning metric.
  - recover timeout 500ms => `E_RECOVER_TIMEOUT`, transition to PAUSED.

### 2.5 JNI bridge
- Owner: Runtime JNI layer.
- Policy:
  - invalid handle/released handle call => `E_INVALID_STATE`.
  - callback thread contract breach => `E_RUNTIME_CONTRACT_BREACH`.
  - no exception-as-flow-control.

## 3. Logging & Observability Policy
- Every runtime error必须记录：error code、owner模块、state、command、thread。
- Fatal errors必须触发阶段 blocker（不得进入下一 stage）。
- Warning errors进入稳定性看板，并纳入 P95/P99 统计窗口。

## 4. Open Backport Items
- 00–08 尚未定义统一错误码表。
- 00–08 尚未定义 callback 线程违反时的处理。
- 00–08 尚未定义 fatal 后是否允许自动重建（建议 Stage0 明确禁止自动隐式重建）。
