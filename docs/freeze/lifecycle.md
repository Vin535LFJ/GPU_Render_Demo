# Runtime Lifecycle Freeze Checklist

## 1. Engine Lifecycle
`IDLE -> PREPARED -> PLAYING/PAUSED/SEEK_* /EOS_HOLD -> STOPPED -> RELEASED`

- Owner: Runtime control thread (state), RenderThread (GL/EGL resources), DecodeThread (codec resources).
- Failure policy: illegal state entry must fail with explicit code; never auto-correct.

## 2. MediaCodec Lifecycle Contract
1. create/configure/start only after PREPARED entry preconditions met.
2. dequeue loop only in PLAYING and seek recovery stages as defined.
3. flush allowed only during SEEK_FLUSHING and STOPPED teardown path.
4. EOS enters EOS_HOLD, blocks new decode input.
5. stop/release before or during RELEASED path under strict order.

Open backport gaps:
- 00–08未定义 flush allowed-state gate。
- 00–08未定义 EOS_HOLD 到 pause/stop 的策略。

## 3. SurfaceTexture Lifecycle Contract
`CREATED -> ATTACHED -> ACTIVE -> DETACHED -> RELEASED`

- Owner: RenderThread only.
- Context lost rebuild fixed order:
  OES texture -> SurfaceTexture -> shader program -> FBO -> VAO/VBO.
- Failure policy: old GL id reuse is forbidden; treat as fatal runtime contract breach.

## 4. RenderGraph Lifecycle Contract
`INIT -> CONFIGURED -> RUNNING -> RECONFIGURING -> RUNNING -> RELEASED`

- Owner: Native Engine (RenderThread).
- Resize/export resolution change must go through RECONFIGURING.
- Failure policy: incompatible FBO key reuse forbidden.

## 5. Release Lifecycle Contract
Strict order:
1) stop clock
2) stop render submit
3) flush decoder
4) clear frame queue
5) detach SurfaceTexture
6) destroy GL resources
7) destroy EGL
8) stop thread

- Owner: Runtime orchestrates; RenderThread/DecodeThread execute owned steps.
- Failure policy: step failure logs fatal and engine enters non-recoverable release failure state.
