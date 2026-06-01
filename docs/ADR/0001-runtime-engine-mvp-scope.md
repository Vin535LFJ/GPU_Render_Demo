# ADR-0001: Runtime Engine MVP Scope

## Status

Accepted

## Context

旧文档同时出现“音视频特效编辑器原型”“Runtime Engine Demo”“完整编辑能力预留”等多种表述，容易把 MVP 推向完整 NLE 或商用 SDK。

## Decision

MVP 定位为 **Android Native GPU Video Runtime Engine Demo**，只打通实时预览最小闭环：

```text
MP4 -> MediaCodec -> SurfaceTexture -> OES Texture -> RenderGraph -> GLSurfaceView
```

## Consequences

- P1 不做完整编辑 UI。
- P1 不做导出。
- P1 不做复杂特效。
- 所有扩展能力必须排到 P2/P3/P4 或未来设计。
