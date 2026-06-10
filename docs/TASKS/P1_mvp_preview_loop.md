# P1 Tasks: MVP Preview Loop

## 目标

打通 MP4 到 OES Texture 到 RenderGraph 到 GLSurfaceView 的最小实时预览链路。

## 任务清单

- [x] 创建 Android sample app 最小入口。
- [x] 定义 SDK Facade 最小 API：prepare/play/pause/seek/stop/release。
- [x] 实现 Runtime 状态机 gate。
- [x] 建立 DecodeThread。
- [x] 使用 MediaExtractor + MediaCodec 解码 MP4。
- [x] 创建 OES texture 与 SurfaceTexture。
- [x] 建立 RenderThread 与 EGL context。
- [x] 实现 SurfaceTexture attach/updateTexImage/detach 的 MVP 路径。
- [x] 实现最小 RenderGraph：OESInputPass + PresentPass。
- [x] 输出基础 FrameStats。
- [x] 实现结构化错误码。
- [ ] 验证 release 8 步顺序。

## 当前修复记录

- 已恢复 Gradle externalNativeBuild，C++17 runtime native library 会进入 debug APK。
- 已按 video PTS 为 DecodeThread 增加播放节奏控制，避免无节流 drain 到 EOS。
- 已让 seek 重置播放时钟并重新调度解码循环。
- 已修正自动 prepare/play 后 UI 播放状态不同步的问题。
- 已将 release 做成幂等，并避免在非 GL 线程直接二次销毁 GL 资源。
- 已拆出最小 RenderGraph：`OesInputPass` 负责 OES 输入声明，`PresentPass` 负责绘制到 default framebuffer。
- 已收紧 release：第 3 步实际调用 DecodeThread flush/stop，播放进度回调只移除自身 runnable。
- 已修复播放结束后点击 Play 重播时旧 EOS 状态竞态。

## 仍未关闭

- release 8 步顺序仍需真机日志验证。
- EOS_HOLD 与 AV sync 属于下一轮稳定性工作，当前只完成 P1 简化行为。

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
