# `:feature:stats` 最小任务列表

模块职责：错题统计和 AI 练习统计展示。

依赖模块：`:domain`、`:core:model`、`:core:designsystem`。

需求：`PRD-STAT-*`、`AC-STAT-001`。

## 任务

- [x] `STATS-001` 创建 Feature 模块和错题/练习双分区 Contract。验证：模块编译。
- [x] `STATS-002` 展示错题总数、已掌握数和掌握率。验证：空数据和正常数据测试。
- [x] `STATS-003` 展示错题学科分布和最近 6 个月趋势。验证：月份补零测试。
- [x] `STATS-004` 展示练习总数、正确率和活跃天数。验证：统计值测试。
- [x] `STATS-005` 展示练习学科、难度和月度趋势。验证：图表截图测试。
- [ ] `STATS-006` 在 UI 明确“待复核不计入正确率”。验证：`AC-STAT-001`。
- [ ] `STATS-007` Repository Flow 变化时局部刷新，不持久化远程统计。验证：ViewModel Flow 测试。
- [ ] `STATS-008` 添加无网络状态下完整展示测试。验证：Fake Repository 独立测试。

## 模块完成条件

- [ ] `:feature:stats:testDebugUnitTest` 和图表测试通过。
- [ ] 页面不发起网络请求。
