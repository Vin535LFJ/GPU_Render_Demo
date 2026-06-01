# ADR-0004: RenderGraph Reuse for Preview and Export

## Status

Accepted

## Context

项目需要未来支持实时预览和离线导出。如果预览和导出使用两套渲染逻辑，会出现“所见非所得”和维护成本翻倍。

## Decision

RenderGraph 是预览和未来导出的统一渲染路径：

- 预览输出到 GLSurfaceView / default framebuffer。
- 导出输出到 Offscreen FBO / EncoderSurface。
- OES 输入 pass 只做 OES 到 2D 的转换，不叠加业务特效。

## Consequences

- P1 只实现最小预览 RenderGraph。
- P3 导出必须复用 RenderGraph，不允许另起一套效果逻辑。
- 每个 pass 必须声明输入纹理格式、输出纹理格式、尺寸和 alpha 语义。
