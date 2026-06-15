# `:app` 最小任务列表

模块职责：Android 工程入口、依赖装配、类型安全导航、底部导航和进程级状态恢复。

依赖模块：全部 Feature、`:data`、AI Provider、`:transfer:runtime`。

需求：`PRD-NAV-*`、`PRD-NFR-001`、`PRD-NFR-002`。

完成规则：每个复选框应单独提交；完成代码和对应验证后才可勾选。

## 任务

- [x] `APP-001` 创建 `android/` 根工程、Wrapper、`settings.gradle.kts`、根 `build.gradle.kts` 和 `libs.versions.toml`，录入设计文档固定版本。验证：`cd android && ./gradlew help`。
- [x] `APP-002` 在 `settings.gradle.kts` 注册 22 个模块，并为 `:app` 创建最小 Android Application 配置。验证：`./gradlew projects` 显示全部模块。
- [x] `APP-003` 配置包名 `com.bandu.tiji`、`minSdk 31`、`compileSdk/targetSdk 36`、JDK 17、竖屏和版本信息。验证：`./gradlew :app:processDebugMainManifest`。
- [x] `APP-004` 添加 Manifest 权限与 `dataSync` 前台服务声明，不添加存储读取权限和遥测 SDK。验证：检查 merged manifest。
- [x] `APP-005` 创建 `BanduTijiApplication`、Hilt 启动入口和 `MainActivity` 单 Activity Compose 外壳。验证：`:app:assembleDebug`。
- [x] `APP-006` 定义首页、设备、新增、AI辅导、我的五个一级类型安全目的地。验证：导航目的地序列化单元测试。
- [x] `APP-007` 实现五项底部导航和中央 64 dp 新增按钮，一级页面显示、专注页面隐藏。验证：Compose 导航测试。
- [x] `APP-008` 实现 `NavigationIntent` 到具体路由的唯一映射，Feature 不直接互相导航。验证：架构测试扫描 Feature 依赖。
- [x] `APP-009` 接入全局 Snackbar、未处理错误展示和迁移进行中状态栏。验证：Fake 状态下的 Compose 测试。
- [x] `APP-010` 接入 App 图标、应用名称和浅色启动主题。验证：安装 Debug APK 后冷启动无白屏闪烁。
- [ ] `APP-011` 添加 Hilt 绑定，将 Repository、AI Provider 和 Transfer Runtime 注入 Domain 接口。验证：`:app:hiltJavaCompileDebug`。
- [ ] `APP-012` 添加 App 导航端到端测试：全新启动进入首页，五个一级入口顺序正确。验证：`:app:connectedDebugAndroidTest`。

## 模块完成条件

- [ ] `:app:assembleDebug`、导航测试和 Hilt 编译全部通过。
- [ ] App 只负责装配和导航，不包含业务数据库或 Provider 实现。
