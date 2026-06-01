# ADR-0003: Thread and Resource Ownership

## Status

Accepted

## Context

MediaCodec、SurfaceTexture、EGL、OpenGL ES 都对线程和上下文有严格约束。旧文档中只强调 GL 单线程，但没有完整 owner/forbidden call set。

## Decision

采用唯一 owner 模型：

| 资源/动作 | Owner |
|---|---|
| State transition | Runtime Control Thread |
| MediaCodec / MediaExtractor | DecodeThread |
| EGL / GL resources | RenderThread |
| SurfaceTexture attach/detach/updateTexImage | RenderThread |
| Frame queue produce | DecodeThread |
| Frame queue consume | RenderThread |

## Consequences

- UI 线程只发命令。
- Runtime 不直接持有或操作 GL id。
- DecodeThread 不调用 updateTexImage。
- RenderThread 不直接 flush/dequeue MediaCodec。
- 违反 owner 的调用必须视为错误。
