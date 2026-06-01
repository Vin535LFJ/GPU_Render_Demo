# P1 Tasks: MVP Preview Loop

## 目标

打通 MP4 到 OES Texture 到 RenderGraph 到 GLSurfaceView 的最小实时预览链路。

## 任务清单

- [ ] 创建 Android sample app 最小入口。
- [ ] 定义 SDK Facade 最小 API：prepare/play/pause/seek/stop/release。
- [ ] 实现 Runtime 状态机 gate。
- [ ] 建立 DecodeThread。
- [ ] 使用 MediaExtractor + MediaCodec 解码 MP4。
- [ ] 创建 OES texture 与 SurfaceTexture。
- [ ] 建立 RenderThread 与 EGL context。
- [ ] 实现 SurfaceTexture attach/updateTexImage/detach。
- [ ] 实现最小 RenderGraph：OESInputPass + PresentPass。
- [ ] 输出基础 FrameStats。
- [ ] 实现结构化错误码。
- [ ] 验证 release 8 步顺序。

## 验收标准

- 1080p 基准 MP4 可稳定预览。
- play/pause/seek/stop/release 可用。
- seek 必经冻结状态序列。
- 非法状态调用返回错误码。
- updateTexImage 只在 RenderThread。
- release 不崩溃、不黑屏、不泄漏明显资源。

## 不做

- 不做离线导出。
- 不做复杂 Shader 特效。
- 不做完整多层编辑 UI。
- 不做 4K 优化。
