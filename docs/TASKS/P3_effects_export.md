# P3 Tasks: Effects and Export

## 目标

在稳定 Runtime 上增加基础 Shader 效果和离线导出。

## 任务清单

- [ ] 增加基础调色 pass。
- [ ] 增加羽化/阴影/简单转场 pass。
- [ ] 增加 offscreen FBO 输出路径。
- [ ] 增加 EncoderSurface。
- [ ] 增加 MediaCodec Encoder。
- [ ] 增加 MediaMuxer。
- [ ] 实现导出进度。
- [ ] 实现导出取消。
- [ ] 验证预览/导出一致性。

## 验收标准

- 导出 MP4 可播放。
- 预览与导出复用同一 RenderGraph。
- 导出取消和失败有错误码。
- 预览/导出一致性指标可输出。

## 不做

- 不做完整 NLE。
- 不做模板系统。
- 不做重型 AI。
