# Runtime Freeze Backport - Unresolved Conflicts (00–08 vs 09/10/README)

## Scope
- Inputs: `README.md`, `demo_docs/00~10`.
- Baseline authority: `demo_docs/10_运行时冻结规范文档.md` + `demo_docs/09_开发执行框架文档.md`.
- Constraint: no new features, no architecture expansion.

## C0 - Blocking Conflicts (must resolve before any implementation)

### C0-1 Language baseline mismatch
- Conflict:
  - `00` declares `C++11`.
  - `09/10/README/08` declare or imply `C++17` freeze baseline.
- Impact: ABI/toolchain/library behavior may diverge; build & runtime assumptions inconsistent.
- Owner: Architecture owner + Native owner.
- Lifecycle: build baseline declaration.
- State transition: N/A.
- Failure policy: block P1 start until unified to C++17.

### C0-2 Seek state model mismatch
- Conflict:
  - `01/03/00` use coarse `Seeking` state.
  - `10` freezes multi-stage seek states (`SEEK_REQUESTED/FLUSHING/PRIMING/RECOVERING`) and `EOS_HOLD/RELEASED`.
- Impact: runtime control-plane ambiguity; illegal transition handling cannot be deterministic.
- Owner: Runtime owner.
- Lifecycle: playback control lifecycle.
- State transition: direct conflict with frozen machine.
- Failure policy: illegal transitions must return error code + warning log (per doc10); missing in 00–03.

### C0-3 AV sync threshold mismatch
- Conflict:
  - `00/06/README` describe <50ms and >100ms coarse thresholds.
  - `10` freezes three-zone thresholds: <20ms / 20~80ms / >80ms + anti-oscillation.
- Impact: drift correction implementation and KPI cannot be validated consistently.
- Owner: AV sync owner.
- Lifecycle: render/decode scheduling loop.
- State transition: influences `PLAYING` and `SEEK_RECOVERING` behavior.
- Failure policy: undefined if not frozen; must use doc10 rules.

### C0-4 Release order under-specified vs strict frozen order
- Conflict:
  - `00/06` only mention flush/recover conceptually.
  - `10` defines strict 8-step destroy order.
- Impact: leak/crash risk at shutdown/context-lost path.
- Owner: Native runtime owner.
- Lifecycle: stop/release lifecycle.
- State transition: `STOPPED -> RELEASED`.
- Failure policy: any order violation should be fail-fast log + abort release continuation policy decision (missing in 00–08).

### C0-5 JNI contract incompleteness
- Conflict:
  - `04/00` define JNI method list only.
  - `09` requires JNI versioning, error code, callback thread constraints; `10` requires strict state-based invalid-call rejection.
- Impact: cross-language behavior nondeterministic; cannot guarantee runtime contracts.
- Owner: Runtime JNI owner.
- Lifecycle: API call lifecycle.
- State transition: invalid state call handling undefined.
- Failure policy: missing standardized error mapping and callback threading policy.

## C1 - Major Gaps / Missing Freeze (non-blocking only if explicitly deferred by P0 decision)

### C1-1 Thread ownership not fully backported
- `02/03/08` mention GL single-thread only, but do not freeze Runtime/Decode/Render ownership matrix and forbidden call set like doc10.
- Missing owner/lifecycle/failure policy granularity.

### C1-2 EGL/SurfaceTexture lifecycle not frozen in 00–08
- `00/06` mention flow but not state lifecycle (`CREATED->...->RELEASED`) and context-lost rebuild order.

### C1-3 MediaCodec lifecycle constraints incomplete
- `06` has seek flush steps but lacks EOS behavior freeze, flush trigger scope, recover timeout.

### C1-4 Frame ownership undefined
- No doc in 00–08 defines decoded frame queue ownership, enqueue/dequeue thread boundary, reclaim conditions.

### C1-5 Error policy incomplete
- `05` says JNI exception handling but lacks runtime-level error taxonomy, retry/drop/fail-fast policy by subsystem.

### C1-6 Metrics definition inconsistent granularity
- `05/07` use P50/P95 and coarse stability targets.
- `09` requires P50/P95/P99 + drift/drop specific acceptance.

### C1-7 Scope wording conflict with current project positioning
- `README/00/01` describe "音视频特效编辑器原型" narrative; current frozen定位是 "Android Native GPU Video Runtime Engine Demo".
- Not architecture conflict, but execution communication conflict.

## Conflict Closure Rule
A conflict is "closed" only when:
1. A single frozen statement exists in 00–08.
2. It maps to owner + lifecycle + state transition + failure policy.
3. It references corresponding authority section in doc09/10.
