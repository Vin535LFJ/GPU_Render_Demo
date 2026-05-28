# Runtime Ownership Matrix (Freeze Baseline)

| Domain | Owner | Lifecycle Boundaries | Allowed State(s) | Forbidden Transition/Action | Failure Policy |
|---|---|---|---|---|---|
| Player command ingress | UI thread (issue only) | command emit only | any | direct GL/decode operation | reject & warn |
| State machine transition | Runtime control thread | init->release | all runtime states | multi-thread state mutation | reject transition + error code |
| Decode control | DecodeThread | codec start->stop/release | PREPARED/PLAYING/SEEK_* | render thread flush/dequeue | fatal runtime misuse |
| Frame queue produce | DecodeThread | first decoded frame -> stop/flush | PLAYING/SEEK_PRIMING | cross-thread unsynchronized write | drop frame + error log |
| Frame queue consume | RenderThread | first submit -> quiesce | PLAYING/SEEK_RECOVERING | UI/Runtime direct consume | reject call |
| EGLDisplay/EGLContext/EGLSurface | RenderThread | PREPARED create -> RELEASED destroy | PREPARED..RELEASED | other thread eglMakeCurrent/create/destroy | fatal + mark engine unusable |
| SurfaceTexture attach/detach/updateTexImage | RenderThread | create->release | PREPARED/PLAYING/PAUSED/SEEK_RECOVERING | decode thread updateTexImage | reject & warn |
| OES/GL texture/FBO resources | RenderThread | allocate->free | PREPARED..RELEASED | runtime holds raw GL handles | design violation, block merge |
| RenderGraph params input | Runtime thread (data only) | before render tick | PREPARED/PLAYING | Runtime mutates GL resource directly | reject API |
| AV clock anchor/reset | RenderThread coordination + Audio subsystem | prepare/seek/recover | PLAYING/SEEK_RECOVERING | uncontrolled multi-point reset | warning + force smooth window |
| Export encode pipeline control | Runtime + Encode thread (if present) | export start->stop | export-active states only | share preview GL ownership without boundary | reject start |

## Notes
- This matrix is derived from frozen ownership intent and must be backported to 00–08 with same wording level.
- Any row lacking explicit error code mapping remains open in `error_policy.md`.
