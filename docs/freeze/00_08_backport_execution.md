# 00–08 Freeze Backport 执行清单（按顺序执行）

> 目标：仅做 Stage 0 基线统一，不新增功能、不扩架构、不进入实现。
> 执行方式：按文件顺序串行回填；每完成一项即更新 `docs/freeze/checklist.md` 与 `docs/roadmap/project_status.md`。

---

## 执行总规则
1. **唯一入口**：`docs/freeze/checklist.md`。
2. **唯一状态面板**：`docs/roadmap/project_status.md`。
3. 每条回填必须写清四元组：`owner / lifecycle / state transition / failure policy`。
4. 回填内容以 `demo_docs/09` 和 `demo_docs/10` 为权威来源，不新增新能力。
5. 完成判定：C0 冲突清零后，才允许声明 Stage 0 完成。

---

## Step 0：准备（半天）
- [ ] 建立“回填日志”段落于 `project_status.md`（记录日期、修改文件、关闭项编号）。
- [ ] 在 `checklist.md` 中标注本轮计划处理项（例如 A2/D1/E1/G1）。

输出：
- `project_status.md` 出现当日回填计划。

---

## Step 1：先关 C0（高优先级）

### 1.1 文件 `demo_docs/00_开发总设计文档.md`
处理项：
- [ ] C0-1：`C++11` 全量改为 `C++17`（技术栈、语言约束、编码规范引用处）。
- [ ] C0-2：状态机从 `Seeking` 粗粒度替换为冻结 seek 子状态集合。
- [ ] C0-3：AV sync 阈值改为冻结三段式（<20ms / 20~80ms / >80ms）+ 防振荡约束引用。
- [ ] C0-4：补 `release destroy order` 八步固定顺序。
- [ ] C0-5：JNI 部分补“版本化+错误码+callback 线程约束”的占位规范。

完成标准：
- 文档00不再出现 `C++11` 与粗粒度 `Seeking`。
- 00中可直接定位四元组描述。

### 1.2 文件 `demo_docs/01_项目需求规格说明书(SRS).md`
处理项：
- [ ] 将状态机需求从粗粒度状态升级为冻结状态集合（至少包含 SEEK_* / EOS_HOLD / RELEASED）。
- [ ] 增加异常处理需求条目（非法状态转换返回错误码+告警日志）。

完成标准：
- 01 与 10 的状态集合不冲突。

### 1.3 文件 `demo_docs/03_核心模块详细设计文档.md`
处理项：
- [ ] Runtime 状态机章节改为冻结状态与转换链。
- [ ] 增加线程 ownership（Runtime/Decode/Render）边界。
- [ ] 为 seek/recover 补失败策略（timeout->PAUSED）。

完成标准：
- 03 能作为实现前的 Runtime 行为设计单一来源。

### 1.4 文件 `demo_docs/04_接口设计文档.md`
处理项：
- [ ] JNI 增加版本字段策略（例如 `nativeGetContractVersion()` 或版本常量约定说明）。
- [ ] 补错误码映射规范（至少列 `E_INVALID_STATE/E_INVALID_PARAM/E_RUNTIME_CONTRACT_BREACH/E_RECOVER_TIMEOUT`）。
- [ ] 补 callback 线程契约（单一策略：主线程或专用回调线程，必须二选一并固定）。
- [ ] 补接口状态前置条件（哪些状态允许调用）。

完成标准：
- 04 不再只是方法列表，具备可执行契约含义。

---

## Step 2：补冻结缺口（中优先级）

### 2.1 文件 `demo_docs/02_系统架构设计文档 (AD).md`
处理项：
- [ ] 增加 ownership matrix 摘要（UI/Runtime/Decode/Render）。
- [ ] 增加 RenderGraph 生命周期（INIT->...->RELEASED）。
- [ ] 增加 EGL/SurfaceTexture 生命周期归属声明。

### 2.2 文件 `demo_docs/06_音视频同步与渲染流程设计文档.md`
处理项：
- [ ] AV sync 参数改为冻结三段式 + 调速步长约束。
- [ ] Seek 流程改为 REQUESTED->FLUSHING->PRIMING->RECOVERING。
- [ ] 补 flush allowed-state gate 与 recover timeout。
- [ ] 增加 release destroy order 引用。

### 2.3 文件 `demo_docs/08_开发与目录规范文档.md`
处理项：
- [ ] 统一基线版本矩阵（C++17 + NDK/AGP/Kotlin 版本矩阵占位）。
- [ ] 增加线程规范（禁止跨线程 GL/EGL 调用，禁止非 owner 调 codec flush/dequeue）。

---

## Step 3：验收口径统一（中优先级）

### 3.1 文件 `demo_docs/05_性能与稳定性设计规范.md`
处理项：
- [ ] 指标统一到 P50/P95/P99（render/decode/upload/drift/recover）。
- [ ] 明确 AV drift 与 seek recover 的 P95/P99 阈值。
- [ ] 增加错误统计维度（按 error code 分布）。

### 3.2 文件 `demo_docs/07_里程碑与迭代计划文档.md`
处理项：
- [ ] 每里程碑补量化 exit condition（含 P99 与稳定性口径）。
- [ ] 增加 Stage blocker：P0 冻结未完成不得进入实现。

---

## Step 4：同步主控文档（收口）

### 4.1 文件 `README.md`
处理项：
- [ ] 项目定位收敛为 Runtime Engine Demo 表述。
- [ ] 状态机表述与 10 一致（不再写粗粒度 Seeking）。
- [ ] 明确当前处于 Stage 0（如需，可在 README 增“当前状态”段落）。

### 4.2 文件 `docs/freeze/unresolved_conflicts.md`
处理项：
- [ ] 已关闭 C0 项打标关闭（附对应回填文件路径）。
- [ ] 未关闭项必须保留 owner + deadline。

### 4.3 文件 `docs/freeze/checklist.md`
处理项：
- [ ] 将已完成项从 `[ ]` 更新为 `[x]`。

### 4.4 文件 `docs/roadmap/project_status.md`
处理项：
- [ ] 将 open blockers 改为 closed evidence（每项附文件定位）。
- [ ] 状态由 `IN PROGRESS` 改为 `READY FOR STAGE 1 REVIEW`（仅在 C0=0 后）。

---

## 每日执行节奏（防混乱）
- 每天最多处理 **2 个文档**。
- 每次提交只做一类变化（状态机/JNI/指标）避免混改。
- 提交信息模板：
  - `docs(stage0): backport <domain> to <file>`
  - 例如：`docs(stage0): backport seek state machine to 00/03/06`

---

## Done 定义（Stage 0 完成）
- [ ] `unresolved_conflicts.md` 中 C0 全部关闭。
- [ ] `checklist.md` A~H 无未定义 owner 项。
- [ ] `project_status.md` blocker 清零并可追溯到 00–08 回填证据。
- [ ] 评审可基于 00–08 单独执行，无需依赖额外口头解释。
