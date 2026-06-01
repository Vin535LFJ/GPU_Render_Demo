# ADR-0002: Technology Baseline

## Status

Accepted

## Context

旧文档中存在 C++11 与 C++17 混用，导致工具链、ABI、标准库特性和实现约束不一致。

## Decision

统一技术基线为：

- Kotlin
- JNI
- C++17
- OpenGL ES 3.x
- MediaCodec
- SurfaceTexture
- MediaExtractor
- MediaCodec Encoder + MediaMuxer（P3 后置）

## Consequences

- C++11 表述删除。
- 不在 MVP 引入 Vulkan、protobuf、ECS、plugin system。
- 后续 Native 代码和 CMake/NDK 文档均以 C++17 为准。
