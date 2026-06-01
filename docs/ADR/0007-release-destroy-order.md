# ADR-0007: Release Destroy Order

## Status

Accepted

## Context

Android 视频、SurfaceTexture、EGL 和 GL 资源释放顺序错误会导致黑屏、泄漏、野帧访问或崩溃。旧文档对 release 描述过于概念化。

## Decision

release 必须固定 8 步：

1. stop clock
2. stop render submit
3. flush decoder
4. clear frame queue
5. detach SurfaceTexture
6. destroy GL resource
7. destroy EGL
8. stop thread

## Consequences

- 实现不得跳步或调换顺序。
- context lost 和正常 release 都必须遵守同一原则。
- order violation 必须记录 fatal log，并将 engine 标记为不可继续使用。
