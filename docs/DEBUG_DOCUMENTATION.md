# GPU Render Demo 调试文档

## 目录

1. [问题汇总与解决方案](#1-问题汇总与解决方案)
2. [核心开发进展与现存难点](#2-核心开发进展与现存难点)
3. [文档一致性核查](#3-文档一致性核查)
4. [项目复盘分析](#4-项目复盘分析)
5. [后续开发方案建议](#5-后续开发方案建议)

---

## 1. 问题汇总与解决方案

### 1.1 编译构建类问题

| 问题描述 | 错误信息 | 根本原因 | 解决方案 | 涉及文件 |
|---------|---------|---------|---------|---------|
| Gradle/JDK版本不兼容 | `Your build is currently configured to use incompatible Java 17.0.7 and Gradle 6.8.3` | AGP、Gradle、Kotlin版本矩阵不匹配 | 升级Gradle到8.10、AGP到8.4.2、Kotlin到1.9.24，配置Java 17路径 | `gradle-wrapper.properties`、`app/build.gradle`、`gradle.properties` |
| AAPT2守护进程启动失败 | `AAPT2 aapt2-4.2.2-7147631-windows Daemon #0: Daemon startup failed` | Windows缺少Universal C Runtime | 在`gradle.properties`中添加`android.enableAapt2Daemon=false` | `gradle.properties` |
| JNI链接错误 | `UnsatisfiedLinkError: No implementation found for void ... nativeInit()` | 未加载native库 | 在`RuntimeController`中添加`System.loadLibrary("render_native_demo")` | `RuntimeController.kt` |
| native库找不到 | `couldn't find "librender_native_demo.so"` | 缺少externalNativeBuild配置 | 在`app/build.gradle`中添加完整的cmake配置 | `app/build.gradle` |
| 仓库配置冲突 | `Build was configured to prefer settings repositories over project repositories` | `settings.gradle`与`build.gradle`仓库配置冲突 | 移除`build.gradle`中的`allprojects`仓库配置 | `build.gradle` |

### 1.2 代码逻辑类问题

| 问题描述 | 错误信息 | 根本原因 | 解决方案 | 涉及文件 |
|---------|---------|---------|---------|---------|
| 函数缺少实现体 | `Function 'prepare' without a body must be abstract` | `Player.kt`中`prepare()`方法缺失实现 | 补充方法体实现 | `Player.kt` |
| 帧可用回调未触发 | `SurfaceTexture.OnFrameAvailableListener`未调用 | 初始化顺序问题，SurfaceTexture创建后未正确注册监听器 | 调整初始化顺序，先注册监听器再创建SurfaceTexture | `RenderThread.kt` / `RenderRenderer.kt` |
| 渲染目标错误 | 渲染到PbufferSurface而非WindowSurface | SurfaceView的EGL上下文与RenderThread的EGL上下文隔离 | 使用GLSurfaceView替代SurfaceView，在其渲染线程中执行渲染 | `MainActivity.kt`、`RenderRenderer.kt` |
| 画面上下颠倒 | 视频画面垂直翻转 | 顶点UV坐标顺序错误 | 反转UV坐标的y轴 | `RenderRenderer.kt` |
| 线程安全问题 | `frameAvailable`变量在多线程间不可见 | 缺少`@Volatile`修饰符 | 添加`@Volatile`修饰符确保可见性 | `RenderRenderer.kt` |
| 帧更新时机错误 | 画面闪一下后卡住 | 只有收到新帧时才渲染，没有保持最后一帧 | 修改渲染逻辑，即使没有新帧也持续渲染 | `RenderRenderer.kt` |
| MediaExtractor初始化失败 | `Failed to instantiate extractor` | 直接使用AssetFileDescriptor | 将资产文件复制到缓存目录，使用文件路径 | `MainActivity.kt` |

### 1.3 运行时类问题

| 问题描述 | 错误信息 | 根本原因 | 解决方案 | 涉及文件 |
|---------|---------|---------|---------|---------|
| 蓝屏（无视频渲染） | 屏幕显示蓝色背景 | SurfaceTexture异步初始化导致MediaCodec的surface为null | 添加pending请求和状态标志（`isSurfaceTextureReady`、`isDecoderPrepared`） | `Player.kt` / `MainActivity.kt` |
| OpenGL无上下文错误 | `call to OpenGL ES API with no current context` | GL资源初始化时EGL上下文未激活 | 使用PbufferSurface确保EGL上下文处于活动状态 | `RenderThread.kt` |
| 缓冲区使用标志错误 | `buffer descriptor with invalid usage bits 0x2000` | `SurfaceTexture`构造函数参数错误 | 移除`HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE`参数 | `RenderThread.kt` |
| 颜色空间动态变化 | `BAD CODEC: Output format changed unexpectedly` | MediaCodec解码过程中动态调整输出格式 | 添加格式变化处理逻辑，记录日志但不中断解码 | `DecodeThread.kt` |
| BufferQueue被放弃 | `BufferQueueProducer: dequeueBuffer: BufferQueue has been abandoned` | Surface生命周期管理不当 | 确保Surface在正确时机创建和销毁 | `MainActivity.kt` |

---

## 2. 核心开发进展与现存难点

### 2.0 2026-06-08 接手修复记录

本轮接手后，当前阶段重新确认为 **P1：MVP Preview Loop 稳定化**，不是 P0，也还不能视为 P1 完成。

已修复/调整：

1. **视频播放节奏**：`DecodeThread` 按 video PTS 建立 playback clock 后再 release output buffer，避免解码线程无节流快速 drain 到 EOS，造成“播放几帧就卡住”。
2. **帧回调消费**：`RenderRenderer` 将单个 `frameAvailable` Boolean 改为 pending frame 计数，降低多次 `OnFrameAvailable` 被压成一次的风险。
3. **seek 恢复**：seek 后重置 decode clock，并重新调度 decode loop。
4. **UI 状态**：自动 `prepareAndPlay` 后同步 `isPlaying/isPaused`，避免 Play 按钮重复触发 `RuntimeController.play()` 并产生 `Invalid state: PLAYING`。
5. **release 幂等**：Activity release 增加防重入，GL 资源销毁排到 GLSurfaceView render thread，避免二次直接释放。
6. **native 构建**：恢复 `externalNativeBuild`，debug APK 已能构建 C++17 native library。

仍需验证/继续：

1. 真机重新采集日志，确认帧输出是否按约 5s 素材时长播放，而不是 2s 内 drain 到 EOS。
2. EOS_HOLD、Audio master clock、AV drift 纠偏仍未完成。
3. RenderGraph 仍未拆为正式 `OESInputPass + PresentPass`。
4. release 8 步顺序需要真机 trace 验证。

### 2.1 核心开发进展

#### 已完成功能

1. **视频解码链路**
   - 使用`MediaExtractor`提取视频轨道
   - 使用`MediaCodec`硬解码H.264视频
   - 支持SurfaceTexture作为解码输出

2. **渲染链路**
   - 创建OES纹理与SurfaceTexture
   - 实现OpenGL ES渲染管线
   - 使用GLSurfaceView进行屏幕渲染

3. **状态机控制**
   - C++层实现Runtime状态机（IDLE → PREPARED → PLAYING）
   - 通过JNI暴露给Kotlin层
   - 使用互斥锁保证线程安全

4. **音频播放**
   - 创建独立的音频解码器
   - 使用AudioTrack播放PCM音频
   - 与视频解码并行运行

5. **架构重构**
   - 从独立RenderThread重构为GLSurfaceView.Renderer模式
   - 确保所有GL操作在正确的线程执行

### 2.2 现存难点

| 难点 | 描述 | 影响 |
|-----|------|-----|
| **AV同步** | 视频和音频解码独立运行，未实现同步机制 | 音视频不同步 |
| **Seek功能** | 未实现seek操作 | 无法跳转播放位置 |
| **暂停功能** | 未实现pause操作 | 无法暂停播放 |
| **EOS处理** | 视频播放结束后未正确处理 | 播放完成后行为不确定 |
| **资源释放顺序** | 未严格遵循8步destroy order | 可能导致资源泄漏或崩溃 |
| **帧统计** | FrameStats存在但未完整实现 | 无法量化评估播放质量 |

---

## 3. 文档一致性核查

### 3.1 核查范围

| 文档路径 | 状态 | 一致性评估 |
|---------|------|-----------|
| `docs/PROJECT_VISION.md` | 已冻结 | ✅ 与实际实现一致 |
| `docs/SYSTEM_ARCHITECTURE.md` | 已冻结 | ✅ 架构分层符合规范 |
| `docs/ROADMAP.md` | 已冻结 | ✅ 阶段划分清晰 |
| `docs/TASKS/P0_baseline_freeze.md` | 已冻结 | ✅ 任务清单完整 |
| `docs/TASKS/P1_mvp_preview_loop.md` | 进行中 | ⚠️ 部分任务未完成 |
| `docs/SPECS/` | 已冻结 | ✅ 规格文档完整 |
| `docs/ADR/` | 已冻结 | ✅ 架构决策记录完整 |

### 3.2 偏差分析

#### P1任务清单完成情况

| 任务 | 完成状态 | 备注 |
|-----|---------|-----|
| 创建Android sample app最小入口 | ✅ | 已完成 |
| 定义SDK Facade最小API | ⚠️ | 部分完成（缺少pause/seek） |
| 实现Runtime状态机gate | ✅ | C++层已实现 |
| 建立DecodeThread | ✅ | 已完成 |
| 使用MediaExtractor+MediaCodec解码MP4 | ✅ | 已完成 |
| 创建OES texture与SurfaceTexture | ✅ | 已完成 |
| 建立RenderThread与EGL context | ✅ | 使用GLSurfaceView实现 |
| 实现SurfaceTexture attach/updateTexImage/detach | ✅ | 已完成 |
| 实现最小RenderGraph | ⚠️ | 简化实现，未使用完整RenderGraph |
| 输出基础FrameStats | ⚠️ | 存在但未完整实现 |
| 实现结构化错误码 | ✅ | 已定义ErrorCode |
| 验证release 8步顺序 | ❌ | 未实现 |

---

## 4. 项目复盘分析

### 4.1 P0/P1阶段任务完工进度

#### P0阶段（Baseline Freeze）
- **状态**: ✅ 已完成
- **成果**: 文档冻结完成，技术基线统一为C++17，状态机、AV sync阈值、release order等已定义

#### P1阶段（MVP Preview Loop）
- **状态**: ⚠️ 部分完成（约60%）
- **完成项**:
  - MP4解码链路
  - OES纹理渲染
  - 基础状态机
  - 音频播放
- **未完成项**:
  - pause/seek功能
  - 完整RenderGraph
  - FrameStats完整实现
  - release 8步顺序验证

### 4.2 剩余Bug修复优先级

| Bug | 优先级 | 紧急程度 | 描述 |
|-----|-------|---------|------|
| AV同步缺失 | 🔴 高 | ⚠️ 中 | 音视频不同步影响核心体验 |
| pause/seek缺失 | 🔴 高 | ⚠️ 中 | 核心控制功能缺失 |
| EOS处理不当 | 🟡 中 | ⚠️ 中 | 播放结束行为不确定 |
| release顺序不完整 | 🟡 中 | 🟢 低 | 可能导致资源泄漏 |
| FrameStats不完整 | 🟢 低 | 🟢 低 | 影响可观测性 |

### 4.3 视频纹理渲染功能开发范围

#### 已实现范围
- SurfaceTexture创建与配置
- OES纹理创建与绑定
- 帧可用回调处理
- 纹理更新与渲染

#### 需求细则差异
- 原始规划：完整RenderGraph（OESInputPass + PresentPass）
- 当前实现：简化渲染路径，直接绘制OES纹理

### 4.4 P2阶段后开发功能

根据ROADMAP，以下功能原定规划于P2阶段之后开发：

| 功能 | 计划阶段 | 当前状态 |
|-----|---------|---------|
| AV sync完整实现 | P2 | 未实现 |
| seek recover | P2 | 未实现 |
| release稳定性 | P2 | 部分实现 |
| 离线导出 | P3 | 未实现 |
| 基础特效 | P3 | 未实现 |
| 4K优化 | P4 | 未实现 |

---

## 5. 后续开发方案建议

### 5.1 方案对比

| 方案 | 描述 | 优点 | 缺点 |
|-----|------|-----|-----|
| **方案A** | 继续落地视频纹理渲染新功能 | 功能扩展快 | 可能引入新bug，存量问题未解决 |
| **方案B** | 优先修复存量bug，暂缓新功能 | 稳定现有功能 | 进度慢，功能扩展停滞 |
| **方案C** | 遵照原阶段划分，调整排期 | 符合原始规划 | 需要重新评估排期 |

### 5.2 推荐方案：方案B（优先修复存量bug）

#### 技术依据

1. **核心功能完整性**：当前缺少pause/seek等核心控制功能，用户体验不完整
2. **稳定性风险**：release顺序未验证，可能导致崩溃或资源泄漏
3. **可观测性不足**：FrameStats不完整，无法量化评估播放质量
4. **架构一致性**：部分实现偏离原始设计（如简化的RenderGraph）

#### 人力成本考量

1. **修复成本**：修复现有bug所需工作量小于开发新功能
2. **风险成本**：继续开发新功能可能引入更多bug，增加后续维护成本
3. **验证成本**：修复后可快速验证，而新功能需要完整测试周期

#### 战略目标佐证

1. **MVP完整性**：P1阶段目标是打通最小预览链路，当前缺少核心控制功能
2. **稳定性优先**：根据ROADMAP原则，"不关闭blocker不进入下一阶段"
3. **可验证输出**：修复后可形成完整的MVP验证基线

### 5.3 推荐开发计划

#### 短期目标（1-2周）
1. ✅ 实现pause功能
2. ✅ 实现seek功能（基础版本）
3. ✅ 完善EOS处理
4. ✅ 实现release 8步顺序

#### 中期目标（2-3周）
1. ✅ 完善FrameStats
2. ✅ 实现基础AV同步（audio master clock）
3. ✅ 完善RenderGraph结构
4. ✅ 添加错误码和日志增强

#### 长期目标（进入P2阶段）
1. ✅ 实现完整AV sync drift纠偏策略
2. ✅ 实现seek recover机制
3. ✅ 进行长稳测试和指标验证

---

## 附录：关键代码参考

### A.1 RenderRenderer核心渲染逻辑
```kotlin
// 文件路径: app/src/main/kotlin/com/example/render_native_demo/render/RenderRenderer.kt
fun render() {
    if (frameAvailable) {
        surfaceTexture?.updateTexImage()
        frameAvailable = false
    }
    // 绘制OES纹理...
}
```

### A.2 MainActivity初始化流程
```kotlin
// 文件路径: app/src/main/kotlin/com/example/render_native_demo/MainActivity.kt
// GLSurfaceView配置、Renderer绑定、解码器初始化
```

### A.3 Runtime状态机（C++）
```cpp
// 文件路径: app/src/main/cpp/runtime/RuntimeController.cpp
// 状态迁移逻辑、JNI接口
```

---

**文档版本**: v1.0  
**创建日期**: 2026-06-02  
**适用项目**: GPU_Render_Demo
