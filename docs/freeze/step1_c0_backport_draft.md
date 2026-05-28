# Step 1（先关 C0）逐段落改写草案

> 目的：为 `demo_docs/00/01/03/04` 提供可直接回填的文本草案。
> 约束：仅对齐 09/10 冻结口径，不新增功能、不扩展架构。

---

## A. `demo_docs/00_开发总设计文档.md` 改写草案

### A1. 技术栈段落（替换）
**替换原文（含 C++11 的位置）为：**

```md
**技术栈**：Kotlin + JNI + C++17 + OpenGL ES 3.x + MediaCodec
**核心目标**：实现 MediaCodec+SurfaceTexture+OpenGL ES Shader+时间线特效完整闭环，支持实时预览与离线导出，保证**所见即所得**。

> Stage 0 冻结约束：本文档仅回填运行时冻结口径，不引入新架构与新功能。
```

### A2. Runtime 状态机段落（替换）
**将“Idle → Prepared → Playing → Paused → Seeking → Stopped → Idle”替换为：**

```md
1. **播放状态机（冻结）**
   状态集合：
   `IDLE / PREPARED / PLAYING / PAUSED / SEEK_REQUESTED / SEEK_FLUSHING / SEEK_PRIMING / SEEK_RECOVERING / EOS_HOLD / STOPPED / RELEASED`

   冻结转换：
   - `IDLE -> PREPARED -> PLAYING`
   - `PLAYING -> PAUSED -> PLAYING`
   - `PLAYING/PAUSED -> SEEK_REQUESTED -> SEEK_FLUSHING -> SEEK_PRIMING -> SEEK_RECOVERING -> PLAYING/PAUSED`
   - `PLAYING -> EOS_HOLD -> PAUSED/STOPPED`
   - `任意非RELEASED状态 -> STOPPED -> RELEASED`

   非法转换处理：
   - 返回 `E_INVALID_STATE`
   - 记录状态机告警日志
   - 禁止隐式自动纠正
```

### A3. AV Sync 阈值段落（替换）
**将“<50ms / >100ms”替换为：**

```md
### 6.1 时钟同步策略（冻结）
- 主时钟：默认 AudioClock，无音频使用系统单调时钟
- 漂移定义：`drift = videoPTS - audioClock`
- 区间策略：
  1. `|drift| < 20ms`：稳定区，不调速不丢帧
  2. `20ms <= |drift| <= 80ms`：平滑纠偏区，仅调速
  3. `|drift| > 80ms`：强纠偏区，允许受控丢/追帧
- 防振荡：
  - 连续 drop 上限 3 帧；达到上限后进入 200ms 平稳窗口（仅调速）
  - 300ms 内禁止“调速->丢帧->调速”重复切换超过 2 次
  - seek recover 后前 500ms 禁止 drop，仅允许调速
```

### A4. Release 顺序段落（新增）
**在稳定性/生命周期章节新增：**

```md
### 运行时释放顺序（冻结）
严格顺序（不可改动）：
1. stop clock
2. stop render submit
3. flush decoder
4. clear frame queue
5. detach SurfaceTexture
6. destroy GL resource
7. destroy EGL
8. stop thread

失败策略：
- 任一步骤失败记录 `E_RUNTIME_CONTRACT_BREACH`（fatal）
- 引擎标记为不可继续运行，要求进入 RELEASED 终态收口
```

### A5. JNI 契约段落（新增）
**在“5.2 JNI 接口”后新增：**

```md
### 5.2.1 JNI 运行时契约（冻结）
- Owner：Runtime JNI layer
- Lifecycle：`nativeInit` 创建 handle；`nativeRelease` 后 handle 失效
- State transition：JNI 调用受状态机 gate；非法状态调用返回 `E_INVALID_STATE`
- Failure policy：
  - JNI 返回结构化错误码，不以 Java 异常作为运行时控制流
  - callback 线程策略固定且单一（主线程或专用回调线程二选一，禁止混用）
  - 违反线程契约返回 `E_RUNTIME_CONTRACT_BREACH`
```

---

## B. `demo_docs/01_项目需求规格说明书(SRS).md` 改写草案

### B1. 播放状态机需求（替换）
**将 2.6 中粗粒度状态替换为：**

```md
### 2.6 状态与统计（冻结需求）
- 播放状态机：
  `IDLE / PREPARED / PLAYING / PAUSED / SEEK_REQUESTED / SEEK_FLUSHING / SEEK_PRIMING / SEEK_RECOVERING / EOS_HOLD / STOPPED / RELEASED`
- 非法状态转换：必须返回错误码并记录告警日志，禁止隐式自动纠正
- 统计项：帧耗时、FPS、缓存命中率、延迟帧比例、AV drift 分布、seek recover 耗时分布
```

### B2. 异常矩阵需求（新增）
**在非功能需求后新增：**

```md
## 3.4 异常与失败策略需求（冻结）
- owner：各子系统必须有唯一 owner（Runtime/Decode/Render/JNI）
- lifecycle：异常处理不得破坏冻结生命周期顺序
- state transition：异常触发状态跳转必须显式定义
- failure policy：
  - `E_INVALID_STATE`：保持原状态
  - `E_RECOVER_TIMEOUT`：降级到 `PAUSED`
  - `E_RUNTIME_CONTRACT_BREACH`：标记 fatal，进入 stop/release 收口路径
```

---

## C. `demo_docs/03_核心模块详细设计文档.md` 改写草案

### C1. Runtime 状态机（替换）
```md
### 2.1 播放状态机（冻结）
状态集合：
`IDLE / PREPARED / PLAYING / PAUSED / SEEK_REQUESTED / SEEK_FLUSHING / SEEK_PRIMING / SEEK_RECOVERING / EOS_HOLD / STOPPED / RELEASED`

转换规则：
- `IDLE -> PREPARED -> PLAYING`
- `PLAYING -> PAUSED -> PLAYING`
- `PLAYING/PAUSED -> SEEK_REQUESTED -> SEEK_FLUSHING -> SEEK_PRIMING -> SEEK_RECOVERING -> PLAYING/PAUSED`
- `PLAYING -> EOS_HOLD -> PAUSED/STOPPED`
- `Any non-RELEASED -> STOPPED -> RELEASED`

非法转换：返回 `E_INVALID_STATE` + warning log，状态保持不变
```

### C2. 线程 ownership（新增）
```md
### 2.4 线程与资源 ownership（冻结）
- UI线程：仅发控制命令（play/pause/seek/stop/release）
- Runtime控制线程：状态迁移校验与命令编排
- DecodeThread：MediaCodec dequeue/flush/seekTo
- RenderThread：EGL/GL/SurfaceTexture ownership、render submit、frame queue consume

禁止项：
- 非 RenderThread 调用 `eglMakeCurrent/updateTexImage/SurfaceTexture attach-detach`
- 非 DecodeThread 调用 codec `flush/dequeue`

失败策略：违反 ownership 触发 `E_RUNTIME_CONTRACT_BREACH`（fatal）
```

### C3. Seek recover 失败策略（新增）
```md
### 3.x Seek Recover 约束（冻结）
- recover 条件：至少 1 帧有效视频帧 + AudioClock 重锚点完成
- recover 超时：500ms
- 超时后策略：返回 `E_RECOVER_TIMEOUT`，状态降级为 `PAUSED`
```

---

## D. `demo_docs/04_接口设计文档.md` 改写草案

### D1. JNI 接口版本化（新增）
```md
## 2.1 JNI 版本化契约（冻结）
- 增加契约版本查询接口：
  `native fun nativeGetContractVersion(): Int`
- 版本语义：主版本不兼容变更；次版本兼容扩展
- Runtime 启动时校验版本，不匹配返回 `E_RUNTIME_CONTRACT_BREACH`
```

### D2. 错误码映射（新增）
```md
## 2.2 JNI 错误码契约（冻结）
- `E_INVALID_STATE`
- `E_INVALID_PARAM`
- `E_RUNTIME_CONTRACT_BREACH`
- `E_RECOVER_TIMEOUT`
- `E_DECODE_FAILURE`
- `E_GL_CONTEXT_LOST`

返回约束：
- 所有 JNI 控制接口必须可返回结构化错误码
- 不以异常替代错误码语义
```

### D3. Callback 线程契约（新增）
```md
## 1.3 Callback 线程契约（冻结）
- 回调线程策略：固定为单一线程模型（建议主线程）
- `onPrepared/onProgress/onCompleted/onError` 必须在同一约定线程发出
- 违反线程契约按 `E_RUNTIME_CONTRACT_BREACH` 处理并记录告警
```

### D4. 接口状态前置条件（新增）
```md
## 1.4 控制接口状态前置条件（冻结）
- `prepare`: 仅 `IDLE`
- `play`: `PREPARED/PAUSED/SEEK_RECOVERING(ready)`
- `pause`: 仅 `PLAYING`
- `seek`: `PLAYING/PAUSED`
- `stop`: 任意非 `RELEASED`
- `release`: 仅 `STOPPED`

非法调用：返回 `E_INVALID_STATE`，状态不变
```

---

## E. 回填后联动更新（同一批提交内）
- 更新 `docs/freeze/checklist.md`：勾选 A2/D1/E1 等对应项
- 更新 `docs/freeze/unresolved_conflicts.md`：关闭 C0-1~C0-5（附回填文件）
- 更新 `docs/roadmap/project_status.md`：将对应 blocker 标注为已关闭并附证据路径

