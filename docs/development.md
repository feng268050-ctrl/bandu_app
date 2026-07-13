# Flutter 开发指南

本文档说明 `bandu_app` Flutter 客户端的本地开发与部署命令。

## 前置条件

- Flutter SDK（`flutter doctor` 通过）
- Android SDK（`adb`、`emulator`、`avdmanager` 可用）
- 后端 API 已启动（默认 `http://<host>:3000/api/mobile/v1`）

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
| `make apk` | 构建 release APK |
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
| `API_BASE_URL` | `http://10.0.2.2:3000/api/mobile/v1` | 后端 API 根路径 |
| `EMULATOR_GPU` | （空） | 如 `host`、`swiftshader_indirect` |
| `EMULATOR_NO_AUDIO` | `0` | 设为 `1` 关闭模拟器音频 |
| `EMULATOR_HEADLESS` | `0` | 设为 `1` 无窗口模式（仅 CI/脚本） |

示例：

```bash
# 真机
ADB_SERIAL=9DLVS8DE7DQCCU9P make sync

# 指定 API
API_BASE_URL=http://192.168.1.10:3000/api/mobile/v1 make run
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

查看已连接设备：

```bash
adb devices -l
```

## 脚本

| 脚本 | 用途 |
|------|------|
| `scripts/emulator-launch.sh` | 启动或复用模拟器，等待开机完成 |
| `scripts/flutter-sync.sh` | debug 构建、安装、重启 |

跳过构建仅重装（需已有 debug APK）：

```bash
SYNC_SKIP_BUILD=1 make sync
```
