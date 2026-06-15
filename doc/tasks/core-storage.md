# `:core:storage` 最小任务列表

模块职责：Room、FTS、DataStore、图片、Keystore 和双存储槽。

依赖模块：`:core:common`、`:core:model`。

需求：`PRD-DATA-*`、`PRD-SEC-001` 至 `PRD-SEC-003`、`PRD-NFR-004`。

## 任务

- [x] `STORAGE-001` 创建模块并接入 Room、KSP、DataStore、Paging 和 AndroidX Security 基础能力。验证：`:core:storage:compileDebugKotlin`。
- [x] `STORAGE-002` 实现 `collections` Entity、DAO、唯一名称约束和索引。验证：CRUD 仪器测试。
- [x] `STORAGE-003` 实现 `error_items` Entity、DAO、外键和组合索引。验证：CRUD 与级联删除测试。
- [x] `STORAGE-004` 实现 `tags`、`error_item_tags` Entity、DAO 和重名约束。验证：标签关联测试。
- [x] `STORAGE-005` 实现 `tutor_sessions`、`tutor_messages`、`exercises` Entity 和 DAO。验证：会话级联与错题置空测试。
- [x] `STORAGE-006` 实现 `error_item_fts` 与新增、更新、删除同步。验证：FTS 一致性测试。
- [ ] `STORAGE-007` 实现组合筛选 PagingSource 和只读摘要投影。验证：关键词与全部筛选组合测试。
- [ ] `STORAGE-008` 实现错题和练习统计聚合 DAO。验证：`NEEDS_REVIEW` 不进入分母。
- [ ] `STORAGE-009` 创建 schema v1、导出 schema 文件并禁止破坏性迁移。验证：MigrationTestHelper。
- [ ] `STORAGE-010` 实现标准标签资产读取和稳定 ID upsert。验证：重复启动不产生重复标签。
- [ ] `STORAGE-011` 实现 PortablePreferences 与 DevicePreferences 两套 DataStore。验证：迁移字段隔离测试。
- [ ] `STORAGE-012` 实现 Android Keystore AES-256-GCM API Key 存取和清除。验证：密文落盘、明文不落盘测试。
- [ ] `STORAGE-013` 实现图片旋正、裁剪结果压缩、去 EXIF、SHA-256 和原子写入。验证：尺寸、大小和元数据测试。
- [ ] `STORAGE-014` 实现 480 px 缩略图和图片孤儿清理队列。验证：删除失败后重试测试。
- [ ] `STORAGE-015` 实现 `slot_a/slot_b`、活动槽指针、数据库关闭/重开和健康检查。验证：槽切换仪器测试。
- [ ] `STORAGE-016` 实现 `pending_commit` 崩溃恢复和新槽失败回滚。验证：强杀恢复测试。
- [ ] `STORAGE-017` 建立 5000 条错题基准数据生成器和查询计划断言。验证：首次列表、分页、筛选性能测试。

## 模块完成条件

- [ ] 所有 Room、Keystore、图片和槽切换仪器测试通过。
- [ ] `AC-CAP-002`、`AC-NFR-001`、`AC-NFR-003` 的存储侧条件通过。
