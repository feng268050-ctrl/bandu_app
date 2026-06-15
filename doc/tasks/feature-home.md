# `:feature:home` 最小任务列表

模块职责：首页欢迎区和四个功能卡片。

依赖模块：`:domain`、`:core:model`、`:core:designsystem`。

需求：`PRD-HOME-*`。

## 任务

- [x] `HOME-001` 创建 Feature 模块、Route、Screen、ViewModel、Contract。验证：`:feature:home:compileDebugKotlin`。
- [x] `HOME-002` 定义昵称加载、空昵称和加载失败 UiState。验证：ViewModel 测试。
- [x] `HOME-003` 实现应用名称和欢迎文案，不显示账号、公告、退出和管理员入口。验证：Compose 测试。
- [x] `HOME-004` 实现上传/拍摄、查看题集、标签、统计四卡片。验证：截图测试。
- [x] `HOME-005` 四卡片点击分别发出 NavigationIntent。验证：Fake 收集测试。
- [x] `HOME-006` 添加 200% 字体和窄屏布局测试。验证：无截断。
- [ ] `HOME-007` 添加 `AC-HOME-001`、`AC-HOME-002` 验收测试。验证：Feature 独立 JVM/Compose 测试。

## 模块完成条件

- [ ] `:feature:home:testDebugUnitTest` 通过。
- [ ] 模块不依赖其他 Feature。
