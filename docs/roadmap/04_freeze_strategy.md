# Freeze Strategy

## Strategy
1. Authority-first: 以 09/10 为冻结权威，00–08 仅做回填与统一，不新增能力。
2. Contract decomposition: 每条冻结项必须拆成 owner/lifecycle/state/failure。
3. Conflict-first closure: 先关 C0，再处理 C1。
4. Stage gate governance: blocker 未清零不得跨 stage。

## Freeze Workflow
- Step 1: 文档差异扫描（README + 00–10）
- Step 2: 冲突分级（C0/C1）
- Step 3: 合同化回填（00–08）
- Step 4: 评审签字（架构/运行时/原生）
- Step 5: 阶段退出判定

## Freeze Definition of Done
- 关键域（thread/EGL/codec/seek/release/JNI/error/sync/frame/perf）全部具备：
  - 唯一 owner
  - 生命周期
  - 状态转换
  - 失败策略
