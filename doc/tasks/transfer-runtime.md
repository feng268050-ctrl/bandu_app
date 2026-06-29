# `:transfer:runtime` 最小任务列表

模块职责：Android NSD、TCP、前台任务、迁移包流式读写和原子导入协调。

依赖模块：`:transfer:protocol`、`:core:storage`、`:core:common`。

需求：`PRD-DEV-*`、`PRD-NFR-007`、`PRD-SEC-007`。

## 任务

- [x] `RUNTIME-001` 创建 Android Library 模块和 Runtime public API。验证：`:transfer:runtime:compileDebugKotlin`。
- [x] `RUNTIME-002` 实现本机设备 UUID、Keystore P-256 身份和公钥指纹存取。验证：重启保持、重置变化测试。
- [ ] `RUNTIME-003` 实现 `_bandu-tiji._tcp.` NSD 注册、TXT 和手动发现。验证：两模拟器发现测试。
- [x] `RUNTIME-004` 实现发现生命周期，离开页面停止注册和扫描。验证：生命周期仪器测试。
- [x] `RUNTIME-005` 实现仅绑定局域网接口的 TCP Server/Client 和连接超时。验证：不监听公网地址测试。
- [x] `RUNTIME-006` 集成首次 SRP 配对和双方身份确认。验证：错误码、过期码、限流测试。
- [x] `RUNTIME-007` 实现 TrustedPeer DataStore、后续双向认证和解除配对。验证：身份变化视为新设备。
- [x] `RUNTIME-008` 实现源端稳定快照、Manifest 和 `records.pbstream` 流式导出。验证：记录数和 hash 测试。
- [x] `RUNTIME-009` 实现图片/缩略图 1 MiB 分块发送，内存保持常量。验证：5000 条数据内存基准。
- [x] `RUNTIME-010` 实现目标端 resume bitmap 持久化和 24 小时过期。验证：进程重启续传测试。
- [x] `RUNTIME-011` 实现非活动槽批量导入、外键/hash/图片/FTS 健康检查。验证：损坏包拒绝测试。
- [x] `RUNTIME-012` 实现双方最终确认、活动槽切换、API Key 清除和失败回滚。验证：提交阶段强杀测试。
- [ ] `RUNTIME-013` 实现 `dataSync` Foreground Service、通知和可观察进度。验证：后台传输仪器测试。
- [x] `RUNTIME-014` 实现取消、网络中断、空间不足和协议错误的清理/可恢复策略。验证：故障注入测试。
- [ ] `RUNTIME-015` 添加完整双设备 E2E：配对、40% 断网、续传、替换、源保留。验证：`AC-DEV-001` 至 `AC-DEV-006`。

## 模块完成条件

- [ ] 双设备 E2E 和 5000 条迁移基准通过。
- [ ] 无用户操作时不扫描、不注册、不启动迁移服务。
