# Flutter 开发指南

本文档说明 `bandu_app` Flutter 客户端的本地开发与部署命令。

## 前置条件

- Flutter SDK（`flutter doctor` 通过）
- Android SDK（`adb`、`emulator`、`avdmanager` 可用）
- 本地开发后端已启动（默认 `http://<host>:3000/api/mobile/v1`）

生产环境固定使用：

```text
https://aibandu.dpdns.org/api/mobile/v1
```

生产健康检查固定使用：

```text
https://aibandu.dpdns.org/api/health
```

模拟器访问本机服务使用：

```text
http://10.0.2.2:3000/api/mobile/v1
```

## 快速开始

```bash
cp .env.example .env   # 可选，按需修改 AVD / 端口 / API 地址
make deps
make emulator          # 启动模拟器并 flutter run
```

改代码后热同步到设备：

```bash
make sync
```

## Make 命令

| 命令 | 说明 |
|------|------|
| `make deps` | `flutter pub get` |
| `make run` | 在当前设备上 `flutter run` |
| `make emulator` | 启动模拟器（默认 `emulator-5556`）并 `flutter run` |
| `make sync` | debug 构建 → `adb install -r` → 重启应用 |
| `make run-prod` | 使用唯一公网 API 运行生产配置 |
| `make apk` / `make apk-prod` | 校验生产配置并构建 Release APK |
| `make verify-prod-config` | 静态校验生产域名、HTTPS、认证和 Manifest |
| `make install` | 安装 release APK 并重启 |
| `make install-debug` | 安装已有 debug APK 并重启 |
| `make relaunch` | 仅重启应用 |
| `make analyze` | 静态分析 |
| `make test` | 运行测试 |

## 环境变量

通过 `.env` 或命令行覆盖（`make` 会自动加载 `.env`）：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `ADB_SERIAL` | `emulator-5556` | 目标设备序列号 |
| `AVD` | `Bandu_Pixel_7_Pro_API_36` | 模拟器 AVD 名称 |
| `EMULATOR_PORT` | `5556` | 模拟器 ADB 端口 |
| `API_BASE_URL` | （空） | Debug 单次覆盖地址；Production 必须等于固定公网地址 |
| `EMULATOR_API_BASE_URL` | `http://10.0.2.2:3000/api/mobile/v1` | Android 模拟器 API 地址 |
| `DEVICE_API_BASE_URL` | `http://100.69.41.14:3000/api/mobile/v1` | 真机 debug 使用的 WSL/Tailscale 地址 |
| `RELEASE_API_BASE_URL` | `https://aibandu.dpdns.org/api/mobile/v1` | Release 兼容变量，不允许改为其他地址 |
| `APP_ENV` | Debug 为 `development`，Release 为 `production` | App 环境；Release 强制为 `production` |
| `BYPASS_AUTH` | `false` | 仅静态 Debug 可启用；Production 强制为 `false` |
| `EMULATOR_GPU` | （空） | 如 `host`、`swiftshader_indirect` |
| `EMULATOR_NO_AUDIO` | `0` | 设为 `1` 关闭模拟器音频 |
| `EMULATOR_HEADLESS` | `0` | 设为 `1` 无窗口模式（仅 CI/脚本） |

## 版本管理

发布或合并一批可见功能后，同步更新下列位置（保持一致）：

| 位置 | 含义 |
|------|------|
| `VERSION` | 展示用版本，形如 `v0.2.0`；`make` / 构建脚本读取此文件注入 `APP_VERSION` |
| `pubspec.yaml` 的 `version` | Flutter / Android 正式版本，形如 `0.2.0+1002`（`+` 后为 `versionCode`，每次发布必须递增） |

约定：

- **主版本 / 次版本 / 修订**：`0.x.y` 阶段，功能增量升次版本（如 `0.1.1` → `0.2.0`），仅修 bug 升修订号。
- **versionCode（`+N`）**：只增不减；商店/覆盖安装依赖它。
- 硬编码兜底（`app_config.dart`、构建脚本默认值等）与 `VERSION` 保持同号，避免未注入 `APP_VERSION` 时显示旧号。
- 改完后可用 `make version` 核对当前号。

## AI 模型设置

“设置 > AI 模型”使用 `/ai/models` 和 `/ai/preferences` 管理模型目录与按用途偏好。
API Key 仅在提交时发送给服务端；客户端不会存储或展示完整 Key。服务端接口尚未就绪时，
可在测试或开发 Provider override 中使用 `FakeAiModelRepository` 和
`FakeAiPreferenceRepository` 驱动目录与偏好 UI。

示例：

```bash
# 真机
ADB_SERIAL=9DLVS8DE7DQCCU9P make sync

# 临时指定模拟器 API
EMULATOR_API_BASE_URL=http://10.0.2.2:3001/api/mobile/v1 make run

# 临时指定真机 debug API
ADB_SERIAL=9DLVS8DE7DQCCU9P API_BASE_URL=http://192.168.1.10:3000/api/mobile/v1 make sync
```

## 模拟器说明

### 默认配置

- AVD：`Bandu_Pixel_7_Pro_API_36`（Pixel 7 Pro, API 36）
- 端口：`5556`（`emulator-5556`）
- `emulator-5554` 预留给其他项目，请勿占用

### 无窗口（headless）问题

若 `make emulator` 成功但看不到模拟器窗口，通常是之前以 **无头模式**（`-no-window`）启动过，参数被缓存在：

```text
~/.android/avd/<AVD名>.avd/emu-launch-params.txt
```

处理方式：

```bash
adb -s emulator-5556 emu kill
rm ~/.android/avd/Bandu_Pixel_7_Pro_API_36.avd/emu-launch-params.txt
make emulator
```

`scripts/emulator-launch.sh` 会自动检测无头实例并尝试以 GUI 模式重启。

### 重建 AVD

```bash
EMULATOR_RECREATE=1 make emulator
```

## 真机安装

Release 包（发布验证）：

```bash
ADB_SERIAL=<设备序列号> make install
```

日常开发同步（debug，更快）：

```bash
ADB_SERIAL=<设备序列号> make sync
```

`make sync` 会通过 ADB 自动识别目标。模拟器会优先使用显式 `API_BASE_URL`，未配置时才读取
`EMULATOR_API_BASE_URL`，默认使用 `10.0.2.2`；真机若发现
`API_BASE_URL` 仍为 `10.0.2.2`、`localhost` 或 `127.0.0.1`，会在终端提示并自动切换到
`DEVICE_API_BASE_URL`。

`make apk` 和 `make apk-prod` 只接受
`https://aibandu.dpdns.org/api/mobile/v1`，不会静默回退到局域网、Tailscale 或其他
域名。直接执行 `flutter build apk --release` 时，应用配置层同样强制
`APP_ENV=production`、HTTPS、固定 Host/Path 和 `BYPASS_AUTH=false`。

Release 构建前会请求 `https://aibandu.dpdns.org/api/health`。若域名、TLS 或健康接口
不可用，构建会停止，避免产生无法联网的 APK。只有明确需要离线产物时才可使用
`SKIP_RELEASE_API_PREFLIGHT=1 make apk` 跳过可达性检查；该变量不会跳过生产配置和
签名校验。

### Release 签名

Release 不允许使用 Debug 签名。首次配置：

```bash
cp android/key.properties.example android/key.properties
# 修改 android/key.properties，使其指向团队固定的 Release keystore
make apk-prod
```

`android/key.properties`、`*.jks` 和 `*.keystore` 已从 Git 排除。正式 keystore 及密码
必须由发布负责人保管，并至少在两个独立安全位置备份。未配置签名时，Debug APK 仍可
构建，但 Release 构建会明确失败。

## 离线与恢复

- 错题列表、错题详情和统计摘要会保存在本机；离线读取时界面显示“离线缓存”。
- 缓存损坏时自动删除并重建；错题最多保留 500 条，统计摘要保留 30 天。
- 已分析题目保存时先创建带 UUID 的持久化任务，再尝试上传。
- App 启动、回到前台或检测到健康状态恢复后自动重试；离线健康检查失败不会消耗任务重试次数。
- 单个任务最多尝试 5 次，也可在“我的 > 待上传任务”中手动重试或删除。
- 上传成功后自动清理对应临时图片。

查看已连接设备：

```bash
adb devices -l
```

## 脚本

| 脚本 | 用途 |
|------|------|
| `scripts/emulator-launch.sh` | 启动或复用模拟器，等待开机完成 |
| `scripts/flutter-sync.sh` | debug 构建、安装、重启 |
| `scripts/flutter-build-apk.sh` | 按 Debug/Production 策略构建 APK |
| `scripts/api-base-url.sh` | 按目标解析并验证 API 地址 |
| `scripts/verify-production-config.sh` | CI/本地生产配置静态检查 |

跳过构建仅重装（需已有 debug APK）：

```bash
SYNC_SKIP_BUILD=1 make sync
```
