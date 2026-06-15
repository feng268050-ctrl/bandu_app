# `:transfer:protocol` 最小任务列表

模块职责：纯 JVM Protobuf 协议、SRP 配对、会话加密和迁移状态机。

依赖模块：`:core:common`、`:core:model`。

需求：`PRD-DEV-004` 至 `PRD-DEV-018`、`PRD-SEC-005`、`PRD-SEC-006`。

## 任务

- [x] `PROTOCOL-001` 创建纯 JVM 模块，接入 Protobuf Lite 和 Nimbus SRP 2.1.0。验证：`:transfer:protocol:test`。
- [x] `PROTOCOL-002` 定义 `protocol_version=1`、消息类型和通用错误码。验证：未知版本拒绝测试。
- [x] `PROTOCOL-003` 定义 Hello、Pairing、Identity、Transfer、Chunk、Verify、Commit Protobuf 消息。验证：代码生成和 round-trip 测试。
- [x] `PROTOCOL-004` 定义 Collection、ErrorItem、Tag、Session、Message、Exercise、PortablePreferences 记录消息。验证：round-trip 测试。
- [x] `PROTOCOL-005` 实现 6 位码生成、5 分钟过期和失败计数策略。验证：时钟驱动测试。
- [x] `PROTOCOL-006` 实现 `SrpEngine` Nimbus 封装和 RFC 5054 固定参数。验证：RFC 测试向量。
- [x] `PROTOCOL-007` 实现 SRP M1/M2 验证和 HKDF 临时密钥派生。验证：错误码、过期码和篡改测试。
- [x] `PROTOCOL-008` 实现身份交换、签名持有证明和指纹生成。验证：身份替换测试。
- [x] `PROTOCOL-009` 实现已配对 ECDH、transcript 签名和方向密钥派生。验证：双方密钥一致测试。
- [ ] `PROTOCOL-010` 实现 AES-256-GCM 帧编码、AAD、sequence 和 2 MiB 上限。验证：篡改、重放、乱序、重复 nonce 测试。
- [ ] `PROTOCOL-011` 实现 Manifest、FileEntry、记录流和 SHA-256 校验。验证：缺文件和 hash 错误测试。
- [ ] `PROTOCOL-012` 实现 1 MiB chunk、bitmap、缺块请求和 Ack 状态。验证：40% 中断续传状态测试。
- [ ] `PROTOCOL-013` 实现配对状态机。验证：所有合法/非法转移测试。
- [ ] `PROTOCOL-014` 实现迁移 Offer、Transfer、Verify、Commit 状态机。验证：取消、拒绝、重复消息测试。
- [ ] `PROTOCOL-015` 对配对码和共享 secret 在使用后执行数组清零。验证：可观察 buffer 测试。

## 模块完成条件

- [ ] `:transfer:protocol:test` 全部通过。
- [ ] RFC 向量、属性测试和协议安全审查项无阻塞问题。
