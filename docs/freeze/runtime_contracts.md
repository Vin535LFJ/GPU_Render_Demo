# Runtime Contract Checklist

## Contract Principles
1. Contract-first: Runtime行为以冻结契约为准，不以实现便利改动。
2. Single owner: 每项运行时资源/动作必须有唯一 owner。
3. State-gated: 所有命令均受状态机 gate。
4. Explicit failure: 每个失败都有错误码、日志级别、恢复策略。

## 1) Control Command Contracts

### 1.1 prepare
- Owner: Runtime control thread orchestrates; RenderThread/DecodeThread execute owned substeps.
- Lifecycle: `IDLE -> PREPARED` only.
- State transition: invalid source state => reject.
- Failure policy: return invalid-state error; no implicit stop+reprepare.

### 1.2 play
- Owner: Runtime triggers clock & submit enable; RenderThread owns submit path.
- Lifecycle: allowed from `PREPARED/PAUSED/SEEK_RECOVERING` (after recovery condition met).
- State transition: to `PLAYING`.
- Failure policy: if recover precondition unmet, keep paused and return recover-not-ready.

### 1.3 pause
- Owner: Runtime control thread.
- Lifecycle: `PLAYING -> PAUSED`.
- State transition: freeze clock, block new submit.
- Failure policy: no-op forbidden; return invalid-state on non-playing.

### 1.4 seek(targetUs)
- Owner: Runtime issues state changes; DecodeThread owns flush/seekTo; RenderThread owns queue gates/recover.
- Lifecycle: `PLAYING/PAUSED -> SEEK_* -> PLAYING/PAUSED`.
- State transition: must pass REQUESTED->FLUSHING->PRIMING->RECOVERING in order.
- Failure policy: timeout(500ms) => degrade to `PAUSED`, emit recover-timeout error.

### 1.5 stop
- Owner: Runtime orchestration + Render/Decode owned teardown actions.
- Lifecycle: any non-RELEASED -> STOPPED.
- State transition: quiesce submit first.
- Failure policy: partial failure escalates to fatal release-required.

### 1.6 release
- Owner: RenderThread owns GL/EGL/SurfaceTexture final destruction.
- Lifecycle: `STOPPED -> RELEASED` only.
- State transition: must follow strict destroy order.
- Failure policy: if order interrupted, log fatal + mark engine unusable.

## 2) Resource Ownership Contracts
- EGLDisplay/EGLContext/EGLSurface: owner=RenderThread only.
- SurfaceTexture attach/detach/updateTexImage: owner=RenderThread only.
- MediaCodec control (flush/dequeue): owner=DecodeThread only.
- State transitions: owner=Runtime control thread only.
- Frame queue consume: owner=RenderThread; produce=DecodeThread.

## 3) JNI Contracts
- JNI method calls are state-gated and non-reentrant on same engine handle.
- JNI callback thread contract must be explicit (main-thread or dedicated callback thread, one policy only).
- Engine handle lifecycle: create once in init, invalid after release.
- Failure policy: JNI returns structured error code; Java exception not used as runtime control flow.

## 4) AV Sync Contracts
- Drift zones and correction policy strictly follow frozen thresholds.
- Recovery windows after seek/re-anchor prohibit aggressive drop policy.
- Failure policy: oscillation guard breach => force smooth-only window and warning metric.

## 5) Validation Contracts
- Each contract must map to testable runtime evidence: logs/counters/traces.
- Exit criterion for Stage 0: all contracts mapped into 00–08 with no contradiction.
