# 视频纹理化、OpenGL ES 渲染与音视频处理学习指南

本文面向本项目当前的 Android GPU 视频预览 Demo，目标是帮助你从“数据如何从 MP4 变成屏幕像素”入手，系统理解视频纹理化、图像渲染、OpenGL ES、Shader 编程、音视频解码与同步的核心流程、原理和难点。

## 1. 先看项目在做什么

项目当前定位是 **Android Native GPU Video Runtime Engine Demo**：用 Kotlin、MediaCodec、SurfaceTexture、OpenGL ES 和 RenderGraph 打通 MP4 解码、OES 纹理上屏、基础播放控制、seek/EOS/release 行为，并为后续特效与离线导出预留架构。

当前 MVP 的最短链路可以概括为：

```text
assets/test.mp4
  -> MediaExtractor 拆分视频/音频轨
  -> MediaCodec 解码视频到 Surface
  -> SurfaceTexture 接收解码帧并绑定到 GL_TEXTURE_EXTERNAL_OES
  -> RenderThread 调用 updateTexImage()
  -> Fragment Shader 采样 samplerExternalOES
  -> glDrawArrays 绘制全屏矩形
  -> GLSurfaceView 显示

同时：
assets/test.mp4
  -> MediaExtractor 拆分音频轨
  -> MediaCodec 解码音频到 PCM
  -> AudioTrack.write() 播放声音
```

项目代码中最关键的学习入口：

| 模块 | 文件 | 你应该重点看什么 |
|---|---|---|
| Activity/生命周期/按钮控制 | `app/src/main/kotlin/com/example/render_native_demo/MainActivity.kt` | `GLSurfaceView`、Renderer 创建、播放/暂停/seek/release 编排 |
| 视频解码 | `app/src/main/kotlin/com/example/render_native_demo/decode/DecodeThread.kt` | `MediaExtractor` 选轨、`MediaCodec` 输入输出队列、PTS 节奏控制、seek/flush |
| 音频解码 | `app/src/main/kotlin/com/example/render_native_demo/decode/AudioDecoder.kt` | 音频轨解码、PCM 输出、`AudioTrack` 播放 |
| OES 纹理桥接 | `app/src/main/kotlin/com/example/render_native_demo/render/RenderRenderer.kt` | OES texture、`SurfaceTexture`、`onFrameAvailable`、`updateTexImage` |
| 渲染图与 Shader | `app/src/main/kotlin/com/example/render_native_demo/render/RenderGraph.kt` | OES 输入 pass、present pass、顶点/片元 shader、全屏绘制 |
| 架构说明 | `docs/SYSTEM_ARCHITECTURE.md` | 线程模型、模块边界、资源 ownership |
| 渲染规格 | `docs/SPECS/rendering_pipeline.md` | RenderGraph、SurfaceTexture 生命周期、context lost 重建 |
| 同步规格 | `docs/SPECS/av_sync.md` | drift 阈值、平滑纠偏、seek recover 规则 |

## 2. 视频纹理化是什么

**视频纹理化**就是把视频解码器输出的一帧帧图像，当作 GPU 可以采样的纹理来使用。传统 CPU 路径可能是：

```text
解码器输出 YUV/RGBA buffer -> CPU 拷贝 -> glTexImage2D 上传到 GPU -> shader 采样
```

这个路径的问题是 CPU 拷贝和纹理上传成本高，1080p/4K 视频很容易造成卡顿。

Android 更推荐的预览路径是：

```text
MediaCodec 解码器 -> Surface -> SurfaceTexture -> OES external texture -> shader 采样
```

这里 `Surface` 是解码器的输出目标，`SurfaceTexture` 是连接 Android 图像生产者和 OpenGL 纹理消费者的桥，`GL_TEXTURE_EXTERNAL_OES` 是专门用于外部图像源的纹理类型。这样视频帧通常可以在系统图形缓冲区/GPU 侧流转，减少 CPU 搬运。

### 2.1 本项目的视频纹理化链路

1. 渲染线程创建 OES texture：`glGenTextures`、`glBindTexture(GL_TEXTURE_EXTERNAL_OES)`、设置过滤和 wrap 参数。
2. 用该 texture id 创建 `SurfaceTexture`。
3. 用 `Surface(surfaceTexture)` 创建给 MediaCodec 的解码输出 Surface。
4. `DecodeThread` 配置 MediaCodec 时把这个 Surface 传入 `configure(format, surface, null, 0)`。
5. MediaCodec 每 release 一个 output buffer 且 `render=true`，解码帧就进入 Surface/BufferQueue。
6. `SurfaceTexture` 收到帧可用回调，渲染线程在下一次 `onDrawFrame` 中调用 `updateTexImage()`，把最新 BufferQueue 图像更新到 OES texture。
7. RenderGraph 的 fragment shader 用 `samplerExternalOES` 采样该纹理并画到屏幕。

## 3. 为什么是 OES external texture，而不是普通 2D texture

普通 2D 纹理使用 `GL_TEXTURE_2D` 和 `sampler2D`，数据通常由应用主动上传。视频解码器输出的图像来自 Android 图形系统外部缓冲区，因此使用：

- 纹理 target：`GL_TEXTURE_EXTERNAL_OES`
- shader 扩展：`#extension GL_OES_EGL_image_external : require`
- sampler 类型：`samplerExternalOES`

它的优点是：

- 避免手动 CPU 拷贝和 `glTexImage2D` 上传。
- 可以直接采样解码器、相机等外部图像源。
- 更适合实时预览。

它的限制是：

- 不能像普通 2D texture 那样随意作为 FBO color attachment。
- shader 采样类型不同，不能直接用 `sampler2D`。
- 部分纹理参数和 mipmap 行为受限。
- 多 pass 特效通常需要先把 OES 采样结果渲染到普通 2D texture/FBO，再继续处理。

因此实际工程常见结构是：

```text
OES texture
  -> OESInputPass: samplerExternalOES 采样
  -> RGBA 2D texture/FBO
  -> EffectPass A/B/C: sampler2D 处理
  -> PresentPass: 上屏
```

本项目当前 MVP 直接把 OES sample 交给 PresentPass 显示，后续扩展特效时应补上 OES 到 2D 的中间 pass。

## 4. OpenGL ES 图像渲染基础

OpenGL ES 渲染一个视频帧的本质是：把一个覆盖屏幕的矩形送入 GPU，让 fragment shader 对矩形上的每个像素采样视频纹理。

### 4.1 坐标系统

本项目的顶点数据每个顶点包含 4 个 float：

```text
x, y, u, v
```

- `x, y` 是裁剪空间坐标，范围通常是 `[-1, 1]`。
- `u, v` 是纹理坐标，范围通常是 `[0, 1]`。

四个顶点使用 `GL_TRIANGLE_STRIP` 绘制成全屏矩形：

```text
(-1,-1) ---- ( 1,-1)
   |             |
   |             |
(-1, 1) ---- ( 1, 1)
```

GPU 会对三角形内部自动插值纹理坐标，片元着色器只需要拿到当前像素对应的 `vTexCoord` 去采样纹理。

### 4.2 顶点 Shader 做什么

顶点 shader 接收每个顶点的位置和纹理坐标：

```glsl
attribute vec4 aPosition;
attribute vec2 aTexCoord;
varying vec2 vTexCoord;
void main() {
    gl_Position = aPosition;
    vTexCoord = aTexCoord;
}
```

它的职责是：

- 输出 `gl_Position`，决定顶点在屏幕上的位置。
- 把纹理坐标传给 fragment shader。

当前项目没有矩阵变换、裁剪、旋转、缩放，所以顶点 shader 非常简单。

### 4.3 片元 Shader 做什么

片元 shader 使用 OES sampler 采样视频纹理：

```glsl
#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform samplerExternalOES uTexture;
varying vec2 vTexCoord;
void main() {
    gl_FragColor = texture2D(uTexture, vTexCoord);
}
```

它的职责是：

- 对当前屏幕像素查找视频纹理颜色。
- 把颜色写入 framebuffer。

这就是“把视频帧贴到屏幕矩形上”的核心。

## 5. RenderGraph 的意义

RenderGraph 是把渲染过程拆成多个 pass 的组织方式。即使当前 MVP 只有两个概念 pass，也值得学习：

```text
OesInputPass -> PresentPass
```

- `OesInputPass`：声明输入是 OES external texture。
- `PresentPass`：绑定 shader、设置顶点属性、绑定纹理、执行 draw call。

未来如果加入滤镜、转场、水印、字幕、离线导出，可以扩展为：

```text
VideoOesInputPass
  -> ColorConvertPass
  -> LutPass
  -> BlurPass
  -> SubtitlePass
  -> CompositePass
  -> PresentPass / EncoderPass
```

RenderGraph 的核心价值：

- 明确每个 pass 的输入/输出纹理类型和尺寸。
- 避免业务特效逻辑堆在一个 shader 或一个 renderer 类里。
- 让预览和导出复用同一套渲染逻辑。
- 便于做 FBO/纹理池、性能统计和错误定位。

## 6. 视频解码流程

Android 视频解码通常包含两个对象：

- `MediaExtractor`：读取容器文件，拆出视频/音频轨，提供压缩包数据和 PTS。
- `MediaCodec`：解码压缩数据。视频可以输出到 ByteBuffer，也可以直接输出到 Surface。

本项目使用 Surface 输出模式，关键流程是：

```text
MediaExtractor.setDataSource(filePath)
  -> 遍历 track，选择 mime 以 video/ 开头的轨道
  -> MediaCodec.createDecoderByType(mime)
  -> codec.configure(format, outputSurface, null, 0)
  -> codec.start()

循环：
  -> dequeueInputBuffer()
  -> extractor.readSampleData(inputBuffer)
  -> queueInputBuffer(pts)
  -> extractor.advance()

  -> dequeueOutputBuffer(bufferInfo)
  -> 根据 bufferInfo.presentationTimeUs 控制播放节奏
  -> releaseOutputBuffer(index, render=true)
  -> 解码帧进入 SurfaceTexture
```

`releaseOutputBuffer(index, true)` 非常关键：`true` 表示把该输出帧渲染到 codec 配置时传入的 Surface。如果传 `false`，这帧会被丢弃，不会进入 OES 纹理。

### 6.1 PTS 和播放节奏

视频每帧都有 PTS，即 presentation timestamp，表示这帧应该在时间轴上的哪个时刻展示。例如 30fps 视频大约每 33.33ms 一帧。

如果解码很快，不做节奏控制就会瞬间把所有帧送给 Surface，播放变成快进。本项目在释放 output buffer 前，根据第一帧 PTS 和单调时钟建立 anchor：

```text
第一帧：firstVideoPtsUs = 当前帧 PTS
       playbackStartElapsedUs = 当前系统单调时间

后续帧目标展示时间：
dueUs = playbackStartElapsedUs + (presentationTimeUs - firstVideoPtsUs)
```

如果当前时间还没到 `dueUs`，解码线程就 sleep 一小段时间。这个策略简单有效，但它以视频时钟/单调时钟为主，不是真正的音频主时钟。

## 7. 音频解码与播放流程

音频路径和视频类似，但输出目标不是 Surface，而是 PCM 数据：

```text
MediaExtractor 选择 audio/ 轨
  -> MediaCodec 解码压缩音频
  -> dequeueOutputBuffer 得到 PCM buffer
  -> AudioTrack.write(pcm)
  -> 扬声器播放
```

`AudioTrack` 初始化时要关心：

- sample rate，例如 44100Hz 或 48000Hz。
- channel count，例如 mono/stereo。
- PCM 格式，例如 `ENCODING_PCM_16BIT`。
- buffer size，太小容易 underrun，太大延迟高。

### 7.1 为什么真实工程常用音频主时钟

人耳对音频抖动非常敏感，音频设备也有自己的播放缓冲和硬件时钟。因此播放器一般以音频播放进度为主时钟：

```text
audioClock = AudioTrack 已播放帧数 / sampleRate + anchor
videoDrift = videoPts - audioClock
```

然后根据 drift 决定视频是正常显示、稍微调速、丢帧追赶，还是等待音频。本项目文档中的 AV Sync Spec 已经定义了后续要使用的 drift 区间：稳定区、小幅平滑纠偏区和强纠偏区。

## 8. 音视频同步的重点难点

音视频同步难点不在“能播放”，而在长时间播放、seek、pause/resume、设备差异下仍然稳定。

### 8.1 常见问题

| 问题 | 原因 | 解决方向 |
|---|---|---|
| 音画逐渐不同步 | 使用两个独立时钟，误差累积 | 统一 timeline，以 audio clock 为主 |
| seek 后短时间花屏或旧帧闪现 | codec/SecurityTexture/queue 未清干净 | seek state machine、flush、清 frame queue、recover 窗口 |
| pause/resume 后视频快进 | 暂停期间没有修正 anchor | resume 时补偿 pause duration |
| 视频过快 | 没按 PTS 等待 | release output 前按 PTS 节奏控制 |
| 视频过慢 | 解码/render 跟不上 | drop late frame、降低特效复杂度、优化 pipeline |
| SurfaceTexture 多线程崩溃 | updateTexImage 不在 GL owner 线程 | 明确 RenderThread ownership |

### 8.2 本项目当前和目标状态

当前实现已经有：

- 视频 PTS 到单调时钟的基础节奏控制。
- pause/resume 时对视频 anchor 做暂停时长补偿。
- seek 时 flush codec 并让 extractor seek 到关键帧。
- 音频单独解码并写入 AudioTrack。

后续要增强的是：

- 音频主时钟，而不是视频/单调时钟主导。
- 统一的 seek recover 状态机。
- drift 指标统计 P50/P95/P99。
- 可控丢帧、平滑调速和防振荡策略。

## 9. Shader 编程学习路线

建议按下面顺序学习和实践：

### 阶段 1：能上屏

- 顶点坐标、纹理坐标。
- `glUseProgram`、`glGetAttribLocation`、`glVertexAttribPointer`。
- `glActiveTexture`、`glBindTexture`、`glUniform1i`。
- `glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)`。

### 阶段 2：理解 OES 与 2D texture

- `samplerExternalOES` 与 `sampler2D` 的区别。
- OES 采样 pass。
- FBO 渲染到 2D texture。
- 多 pass 渲染。

### 阶段 3：做基础图像效果

常见 fragment shader 效果：

- 灰度：`gray = dot(rgb, vec3(0.299, 0.587, 0.114))`
- 亮度/对比度/饱和度。
- 色温、色调、LUT。
- 模糊、锐化、边缘检测。
- alpha 混合、水印、字幕。

### 阶段 4：性能优化

- 减少 pass 数量和纹理读写。
- 避免高精度计算滥用。
- 控制 FBO 尺寸。
- 纹理池复用。
- 统计 GPU/CPU 耗时。

## 10. OpenGL ES 调试重点

渲染类 bug 经常表现为黑屏、绿屏、上下颠倒、只显示一角或崩溃。建议按顺序排查：

1. EGL context 是否创建，GL 调用是否在渲染线程。
2. OES texture id 是否非 0。
3. `SurfaceTexture` 是否创建成功，MediaCodec Surface 是否 valid。
4. `onFrameAvailable` 是否触发。
5. `updateTexImage()` 是否在 RenderThread 调用。
6. shader 是否编译/链接成功。
7. attribute/uniform location 是否有效。
8. 纹理 target 是否匹配：OES 必须用 `GL_TEXTURE_EXTERNAL_OES`。
9. fragment shader 是否声明 OES 扩展。
10. 顶点坐标和纹理坐标是否正确，是否需要使用 `SurfaceTexture.getTransformMatrix()`。

> 注意：真实项目中通常应使用 `SurfaceTexture.getTransformMatrix()` 修正视频纹理的裁剪、旋转、上下翻转等变换；当前 MVP 还没有接入该矩阵，所以遇到方向/裁剪问题时这是重点扩展点。

## 11. 生命周期和线程 ownership

这类项目最容易出问题的是资源生命周期。原则是：谁创建/拥有资源，谁在正确线程销毁。

| 资源/动作 | 正确 owner | 错误做法 |
|---|---|---|
| MediaCodec configure/start/dequeue/flush | DecodeThread | UI 或 RenderThread 直接操作 codec |
| SurfaceTexture updateTexImage | RenderThread | DecodeThread/UI 调用 |
| GL texture/shader/FBO | RenderThread | 跨线程保存 GL id 并调用 |
| Runtime 状态迁移 | Runtime Control Thread | 各线程随意改状态 |
| AudioTrack write | Audio decode thread | 多线程同时写 |

release 推荐顺序：

```text
stop clock
  -> stop render submit
  -> flush decoder
  -> clear frame queue
  -> detach/release SurfaceTexture
  -> destroy GL resources
  -> destroy EGL
  -> stop threads
```

本项目 `MainActivity.release()` 已按文档中的 8 步顺序记录并执行基础清理，这是学习播放器稳定性的重点。

## 12. 从本项目继续学习的实践任务

按难度递增：

1. **接入 `SurfaceTexture.getTransformMatrix()`**
   - 在 `updateTexImage()` 后读取 4x4 纹理矩阵。
   - 顶点 shader 中用矩阵变换纹理坐标。
   - 解决部分视频方向/裁剪问题。

2. **增加一个灰度滤镜 shader**
   - 在 fragment shader 中把 RGB 转灰度。
   - 通过按钮开关滤镜。

3. **拆出真正的 OES -> 2D pass**
   - 创建 FBO 和 2D RGBA texture。
   - OESInputPass 输出普通 2D texture。
   - PresentPass 改为采样 `sampler2D`。

4. **增加性能指标**
   - 统计 `updateTexImage` 耗时、draw 耗时、FPS。
   - 打印 P50/P95/P99。

5. **实现音频主时钟同步**
   - 从 AudioTrack 或写入样本数估算 audio clock。
   - 比较 video PTS 与 audio clock。
   - 按 drift 阈值等待、丢帧或平滑调整。

6. **实现离线导出雏形**
   - 用同一 RenderGraph 渲染到 Encoder Surface。
   - MediaCodec 编码 H.264/H.265。
   - MediaMuxer 合成 MP4。

## 13. 推荐知识地图

```text
Android 媒体基础
  -> MediaExtractor / MediaCodec / MediaFormat / AudioTrack
  -> Surface / SurfaceTexture / BufferQueue

OpenGL ES 基础
  -> EGL / GL context / texture / buffer / shader / framebuffer
  -> OES external texture / FBO / multi-pass rendering

Shader 编程
  -> GLSL ES 语法
  -> 顶点 shader / fragment shader
  -> 颜色空间 / 卷积 / LUT / blending

播放器工程
  -> PTS / DTS / timebase
  -> audio clock / video clock / drift correction
  -> seek / flush / EOS / pause-resume / release

性能与稳定性
  -> thread ownership
  -> zero-copy-ish pipeline
  -> frame drop strategy
  -> context lost rebuild
  -> metrics and validation
```

## 14. 学习时最应该抓住的核心概念

1. **SurfaceTexture 是视频帧进入 OpenGL 的桥。** MediaCodec 只负责把解码帧送到 Surface，真正把帧变成可采样纹理的是 SurfaceTexture 和 OES texture。
2. **`updateTexImage()` 是消费新视频帧的动作。** 它必须在持有 GL context 的渲染线程调用。
3. **Shader 只是按纹理坐标取色。** 视频渲染的最小 shader 非常简单，复杂效果都是在此基础上逐步叠加。
4. **OES texture 适合输入，不适合复杂多 pass。** 做特效通常要先转成普通 2D texture。
5. **播放器稳定性的本质是时间和生命周期。** 解码、渲染、音频播放都能跑不难，难的是同步、seek、pause/resume、release 和异常恢复。
6. **RenderGraph 是可扩展性的基础。** 它让预览、特效和未来导出共享一套渲染管线，避免越写越乱。
