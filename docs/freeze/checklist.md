# Freeze Backport Checklist (Stage 0)

## Usage
- Status: `[ ]` open, `[x]` done, `[-]` accepted defer.
- Every item必须落到 00–08 文档之一，且包含：owner/lifecycle/state transition/failure policy。

## A. Baseline & Scope Alignment
- [ ] A1. 项目定位统一为 "Android Native GPU Video Runtime Engine Demo"（README/00/01/07）。
- [ ] A2. 技术基线统一为 Kotlin + JNI + C++17 + OpenGL ES + MediaCodec（00 与 09/10 对齐）。
- [ ] A3. 明确 Stage 0 禁止实现与禁止扩架构（00/07/08 增加执行约束引用）。

## B. Ownership Freeze Backport
- [ ] B1. Thread ownership matrix 回填到 02/03/08（UI/Runtime/Decode/Render）。
- [ ] B2. EGL/GL ownership 回填到 02/03/06（only RenderThread create/destroy/makeCurrent/updateTexImage）。
- [ ] B3. Frame ownership 回填（decode output queue owner、render consume owner、reclaim owner）。

## C. Lifecycle Freeze Backport
- [ ] C1. MediaCodec lifecycle（init/configure/start/dequeue/flush/stop/release）边界与允许状态。
- [ ] C2. SurfaceTexture lifecycle + context-lost rebuild order。
- [ ] C3. RenderGraph lifecycle（INIT/CONFIGURED/RUNNING/RECONFIGURING/RELEASED）。
- [ ] C4. Release destroy order 固化到 00/06/05。

## D. State & Transition Freeze Backport
- [ ] D1. Seek state machine 精细状态回填 00/01/03/06。
- [ ] D2. 非法状态转换处理规则（error code + warning log，禁止隐式纠正）。
- [ ] D3. EOS_HOLD 行为与退出路径冻结。

## E. Contract Freeze Backport
- [ ] E1. JNI contract：版本化、线程约束、参数结构、错误码映射。
- [ ] E2. Runtime command contract：play/pause/seek/stop/release precondition/postcondition。
- [ ] E3. Callback contract：回调线程与时序保证。

## F. Error Policy Freeze Backport
- [ ] F1. 错误分级（fatal/recoverable/transient/user-input-invalid）。
- [ ] F2. 子系统失败策略（decode/render/sync/jni/export）。
- [ ] F3. 超时策略（seek recover timeout / decode stall timeout / export timeout）。

## G. Metrics & Validation Freeze Backport
- [ ] G1. P50/P95/P99 指标口径回填 05/07。
- [ ] G2. AV drift/纠偏触发率/连续丢帧上限指标回填。
- [ ] G3. 最小链路成功率与稳定性验收定义（样本集、时长、场景）。

## H. Conflict Closure
- [ ] H1. `docs/freeze/unresolved_conflicts.md` C0 清零。
- [ ] H2. C1 仅保留明确 defer 且有 owner+deadline。
- [ ] H3. 架构评审签字记录（文档层）。
