# Rendering Pipeline Spec

## 目标

定义 MVP 渲染链路：MediaCodec 输出帧经 SurfaceTexture / OES Texture 进入 RenderGraph，并显示到 GLSurfaceView。

## 输入

- MediaCodec output Surface 对应的 SurfaceTexture。
- OES texture id。
- SurfaceTexture timestamp。
- Viewport size。
- Runtime render command。

## 输出

- GLSurfaceView 上的预览画面。
- Render metrics：render time、upload/updateTexImage time、present time、late frame。
- 错误事件：GL error、EGL error、SurfaceTexture error。

## 依赖

- RenderThread 持有 EGL context。
- DecodeThread 提供 codec output Surface。
- Runtime 控制 render submit gate。
- Engine Core 提供 shader、VAO/VBO、RenderGraph。

## 不负责什么

- 不控制 MediaCodec flush/dequeue。
- 不实现复杂 Shader 特效。
- 不实现离线导出。
- 不绕过 Runtime 状态机。
- 不在非 RenderThread 操作 GL/EGL/SurfaceTexture。

## RenderGraph 生命周期

```text
INIT -> CONFIGURED -> RUNNING -> RECONFIGURING -> RUNNING -> RELEASED
```

## MVP Pass

| Pass | 输入 | 输出 | 说明 |
|---|---|---|---|
| OESInputPass | OES external texture | 2D RGBA texture or direct sampled output | 只做 OES 采样/转换 |
| PresentPass | 2D texture or OES sampled result | default framebuffer | 显示到 GLSurfaceView |

## Pass Contract

每个 pass 必须声明：

- 输入纹理类型。
- 输出纹理类型。
- 尺寸。
- internal format。
- alpha 语义。
- 是否依赖 viewport。

## SurfaceTexture 生命周期

```text
CREATED -> ATTACHED -> ACTIVE -> DETACHED -> RELEASED
```

规则：

- OES texture 创建后创建 SurfaceTexture。
- RenderThread eglMakeCurrent 后 attach。
- pause 时保持 attach，不 release。
- resume 时继续 updateTexImage。
- release 或 context lost 前 detach。
- detach 后释放引用。

## Context Lost Rebuild

context lost 后必须重建：

1. OES texture。
2. SurfaceTexture。
3. shader program。
4. FBO。
5. VAO/VBO。

禁止复用旧 GL id。

## 验收标准

- 1080p MP4 能稳定上屏。
- updateTexImage 只在 RenderThread。
- pause/resume 不释放 SurfaceTexture。
- release 时 detach 发生在 GL destroy 前。
- context lost 后不复用旧 GL id。
- OES pass 不包含业务特效逻辑。
