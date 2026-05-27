# Stage Risk Matrix

| Risk ID | Risk | Stage | Owner | Trigger | Impact | Mitigation | Blocker Rule |
|---|---|---|---|---|---|---|---|
| R-01 | C++ baseline mismatch | 0 | Arch + Native | 文档出现 C++11/C++17 混用 | 编译与行为口径分裂 | 全文统一 C++17 | 未关闭不得进入 Stage1 |
| R-02 | Seek状态机粗粒度 | 0 | Runtime | 仍保留 Seeking 单状态 | 恢复流程不可验证 | 回填 SEEK_* + EOS_HOLD | 未关闭不得进入 Stage1 |
| R-03 | Ownership不唯一 | 0/1 | Runtime+Native | GL/Codec跨线程调用未禁用 | 崩溃/竞态 | ownership matrix落文档 | 未关闭不得进入 Stage2 |
| R-04 | Release顺序不一致 | 0/1 | Native | teardown步骤任意化 | 资源泄漏/黑屏 | 固化8步顺序 | 未关闭不得进入 Stage2 |
| R-05 | JNI错误策略缺失 | 0/1 | Runtime JNI | 无统一错误码/线程约束 | 跨层不可观测 | error_policy回填 | 未关闭不得进入 Stage2 |
| R-06 | 指标口径不足 | 0/1 | Perf owner | 无P99/漂移分布 | 无法阶段验收 | 统一指标模板 | 未关闭不得进入 Stage2 |
