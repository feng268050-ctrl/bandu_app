# 版本管理

当前版本从 `v0.1.1` 开始，根目录 `VERSION` 是应用版本号的单一来源。Android 构建会读取该文件生成 `versionName` 和 `versionCode`，关于页显示同一个版本号。

## 规则

版本格式为 `v<主版本>.<次版本>.<补丁>`。

| 字段 | 范围 | 用途 |
| --- | --- | --- |
| 主版本 | `0` 到 `9` | 稳定阶段或重大方向变化 |
| 次版本 | `0` 到 `99` | 功能阶段 |
| 补丁 | `0` 到 `999` | 小修复和微调 |

Android `versionCode` 由版本号自动计算：

```text
versionCode = 主版本 * 100000 + 次版本 * 1000 + 补丁
```

例如 `v0.1.1` 对应 `versionCode = 1001`。

## 阶段

| 版本 | 说明 |
| --- | --- |
| `v0.1.0` | 初始可用版 |
| `v0.1.1` | 当前开发验收版 |
| `v0.2.0` | 注册登录版 |
| `v0.3.0` | 学习计划版 |
| `v1.0.0` | 自己稳定使用版 |

## 使用

查看当前版本：

```bash
make version
```

升级版本时只改根目录 `VERSION`，然后运行：

```bash
cd android
./gradlew :app:testDebugUnitTest :app:assembleDebug
```
