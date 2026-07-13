# bandu_app Flutter 五层架构拆分方案

> 目标：将当前 Flutter 项目拆分为应用层、组件层、框架层、转换层、构建层。
> 适用范围：`bandu_app` Flutter Android 客户端。
> 当前阶段：五层拆分已完成；不实现 OTA，不扩展复杂 Web 功能，优先保证 Android 端稳定。

实施结果（2026-07-13）：

```text
应用层、组件层、框架层、转换层已落地
Repository 接口与实现已完成依赖反转
main.dart / bootstrap 为唯一依赖装配入口
旧 lib/app、lib/core、lib/features 已删除
架构边界测试、转换测试和 Android release 构建已通过
emulator-5556 安装与前台启动已验证
```

---

## 1. 拆分目标

当前项目已经完成：

```text
Web 与 Flutter 分离
旧 Kotlin / Compose Android App 删除
Flutter 提升为仓库根工程
Android 作为优先适配平台
```

下一阶段不再进行项目级迁移，而是进行 Flutter 内部架构拆分。

目标五层：

```text
应用层 Application
组件层 Components
框架层 Framework
转换层 Conversion
构建层 Build
```

核心原则：

```text
应用层负责业务流程
组件层负责复用 UI
框架层负责外部能力
转换层负责数据边界
构建层负责装配和编译
```

---

## 2. 总体依赖关系

```text
                 ┌────────────────┐
                 │     构建层      │
                 │ 装配、配置、编译 │
                 └───────┬────────┘
                         │
              ┌──────────▼──────────┐
              │       应用层         │
              │ 页面、状态、业务流程 │
              └──────┬────────┬─────┘
                     │        │
             ┌───────▼───┐ ┌──▼───────────┐
             │   组件层    │ │ 业务抽象接口  │
             │   通用 UI   │ │ Repository   │
             └────────────┘ └────▲─────────┘
                                  │ implements
                         ┌────────┴────────┐
                         │      框架层      │
                         │ 网络、存储、相机 │
                         └────────┬────────┘
                                  │
                         ┌────────▼────────┐
                         │      转换层      │
                         │ DTO、Mapper、编码 │
                         └─────────────────┘
```

禁止形成：

```text
应用层 → Dio
应用层 → Camera Plugin
应用层 → FlutterSecureStorage
组件层 → Repository
转换层 → 页面
框架层 → 页面状态
```

推荐依赖反转：

```text
应用层定义接口
框架层实现接口
构建层完成注入
```

---

## 3. 目标目录结构

```text
bandu_app/
├── lib/
│   ├── application/
│   │   ├── app/
│   │   │   ├── bandu_app.dart
│   │   │   ├── app_router.dart
│   │   │   ├── app_shell.dart
│   │   │   └── bootstrap.dart
│   │   │
│   │   └── features/
│   │       ├── auth/
│   │       ├── capture/
│   │       ├── home/
│   │       ├── library/
│   │       ├── practice/
│   │       ├── profile/
│   │       └── stats/
│   │
│   ├── components/
│   │   ├── design_system/
│   │   │   ├── theme/
│   │   │   ├── colors/
│   │   │   ├── typography/
│   │   │   └── spacing/
│   │   ├── buttons/
│   │   ├── cards/
│   │   ├── dialogs/
│   │   ├── feedback/
│   │   ├── forms/
│   │   └── navigation/
│   │
│   ├── framework/
│   │   ├── network/
│   │   │   ├── client/
│   │   │   ├── interceptors/
│   │   │   ├── services/
│   │   │   └── repositories/
│   │   ├── persistence/
│   │   │   ├── cache/
│   │   │   ├── secure_storage/
│   │   │   └── pending_tasks/
│   │   ├── camera/
│   │   ├── device/
│   │   ├── file_system/
│   │   ├── sync/
│   │   └── config/
│   │
│   ├── conversion/
│   │   ├── api/
│   │   │   ├── auth/
│   │   │   ├── analyze/
│   │   │   ├── error_items/
│   │   │   ├── practice/
│   │   │   └── stats/
│   │   ├── persistence/
│   │   ├── platform/
│   │   └── common/
│   │
│   └── main.dart
│
├── android/
├── scripts/
├── tool/
│   └── build/
├── test/
│   ├── application/
│   ├── components/
│   ├── framework/
│   └── conversion/
├── Makefile
├── pubspec.yaml
└── analysis_options.yaml
```

注意：

```text
不要在仓库根目录创建 build/ 源码目录
```

Flutter 自身会使用：

```text
build/
```

保存生成产物。

构建脚本建议放在：

```text
tool/build/
scripts/
```

---

## 4. 应用层 Application

### 4.1 职责

应用层负责：

```text
页面
页面状态
Controller / Notifier
UseCase
业务流程
业务实体
Repository 接口
路由
登录状态
```

应用层回答：

> 用户执行什么操作，业务流程如何推进？

---

### 4.2 Feature 结构

以拍题功能为例：

```text
application/features/capture/
├── domain/
│   ├── models/
│   │   ├── analyze_result.dart
│   │   └── captured_image.dart
│   └── contracts/
│       ├── analyze_repository.dart
│       └── camera_gateway.dart
│
├── use_cases/
│   ├── capture_question.dart
│   ├── analyze_question.dart
│   └── save_error_item.dart
│
└── presentation/
    ├── capture_page.dart
    ├── capture_controller.dart
    ├── capture_state.dart
    └── widgets/
```

---

### 4.3 应用层接口示例

```dart
abstract interface class AnalyzeRepository {
  Future<AnalyzeResult> analyze(CapturedImage image);
}
```

```dart
abstract interface class CameraGateway {
  Future<CapturedImage?> takePhoto();
}
```

应用层不能出现：

```text
Dio
MultipartFile
CameraController
FlutterSecureStorage
File
MethodChannel
Android API
```

---

### 4.4 应用层现有代码迁移

当前：

```text
lib/app/
lib/features/
```

迁移为：

```text
lib/application/app/
lib/application/features/
```

---

## 5. 组件层 Components

### 5.1 职责

组件层负责：

```text
主题
颜色
字体
间距
按钮
输入框
卡片
弹窗
加载状态
错误状态
空状态
底部导航
图片预览
```

组件层回答：

> 哪些 UI 可以跨多个页面复用？

---

### 5.2 推荐结构

```text
components/
├── design_system/
│   ├── theme/
│   ├── colors/
│   ├── typography/
│   └── spacing/
├── buttons/
│   ├── primary_button.dart
│   └── async_button.dart
├── cards/
│   ├── error_item_card.dart
│   └── statistic_card.dart
├── feedback/
│   ├── loading_view.dart
│   ├── empty_view.dart
│   └── error_view.dart
├── dialogs/
│   └── confirm_dialog.dart
└── navigation/
    └── app_navigation_bar.dart
```

---

### 5.3 组件分类

#### 纯通用组件

```text
PrimaryButton
AsyncButton
LoadingView
EmptyView
ErrorView
ConfirmDialog
```

不依赖任何业务 Feature。

#### 业务展示组件

```text
ErrorItemCard
AnalyzeResultCard
PracticeQuestionCard
StatisticCard
```

可以接收只读业务模型，但不能：

```text
调用 Repository
访问 Dio
修改全局状态
执行路由跳转
```

---

### 5.4 现有代码迁移

当前：

```text
lib/app/theme/
lib/core/widgets/
```

迁移为：

```text
lib/components/design_system/
lib/components/
```

---

## 6. 框架层 Framework

### 6.1 职责

框架层负责：

```text
Dio
HTTP API
Secure Storage
JSON 文件缓存
Camera Plugin
Image Picker
文件系统
MethodChannel
设备信息
同步任务
Repository 实现
```

框架层回答：

> 应用如何调用外部库、系统能力和服务端？

---

### 6.2 推荐结构

```text
framework/
├── network/
│   ├── client/
│   │   └── dio_client.dart
│   ├── interceptors/
│   │   ├── auth_interceptor.dart
│   │   └── refresh_token_interceptor.dart
│   ├── services/
│   │   ├── auth_api_service.dart
│   │   ├── analyze_api_service.dart
│   │   └── error_item_api_service.dart
│   └── repositories/
│       ├── auth_repository_impl.dart
│       ├── analyze_repository_impl.dart
│       └── error_item_repository_impl.dart
│
├── persistence/
│   ├── cache/
│   │   └── json_cache_store.dart
│   ├── secure_storage/
│   │   └── secure_token_store.dart
│   └── pending_tasks/
│       └── pending_task_store.dart
│
├── camera/
│   └── flutter_camera_gateway.dart
├── device/
│   └── android_device_info_service.dart
├── file_system/
├── sync/
│   └── persistent_sync_coordinator.dart
└── config/
```

---

### 6.3 框架层实现示例

应用层定义：

```dart
abstract interface class CameraGateway {
  Future<CapturedImage?> takePhoto();
}
```

框架层实现：

```dart
final class FlutterCameraGateway implements CameraGateway {
  @override
  Future<CapturedImage?> takePhoto() async {
    // 调用 camera 或 image_picker
    throw UnimplementedError();
  }
}
```

---

### 6.4 现有代码迁移

当前：

```text
lib/core/network/
lib/core/database/
lib/core/storage/
lib/core/camera/
lib/core/device/
lib/core/sync/
lib/core/config/
```

迁移为：

```text
lib/framework/network/
lib/framework/persistence/
lib/framework/camera/
lib/framework/device/
lib/framework/sync/
lib/framework/config/
```

---

## 7. 转换层 Conversion

### 7.1 职责

转换层负责：

```text
API JSON → DTO
DTO → Domain Model
Domain Model → API Request
缓存 JSON → Cache Record
平台返回值 → 应用模型
异常 → AppFailure
日期格式转换
枚举转换
路径转换
```

转换层回答：

> 外部数据如何进入应用内部，应用内部数据如何输出？

---

### 7.2 推荐结构

```text
conversion/
├── api/
│   ├── auth/
│   │   ├── auth_response_dto.dart
│   │   └── auth_mapper.dart
│   ├── analyze/
│   │   ├── analyze_request_dto.dart
│   │   ├── analyze_response_dto.dart
│   │   └── analyze_mapper.dart
│   ├── error_items/
│   │   ├── error_item_dto.dart
│   │   └── error_item_mapper.dart
│   ├── practice/
│   └── stats/
│
├── persistence/
│   ├── cached_error_item_record.dart
│   └── cached_error_item_mapper.dart
│
├── platform/
│   ├── captured_file_mapper.dart
│   └── device_info_mapper.dart
│
└── common/
    ├── date_time_converter.dart
    ├── enum_converter.dart
    └── failure_mapper.dart
```

---

### 7.3 Mapper 示例

```dart
final class ErrorItemMapper {
  const ErrorItemMapper();

  ErrorItem toDomain(ErrorItemDto dto) {
    return ErrorItem(
      id: dto.id,
      question: dto.questionText,
      answer: dto.answerText,
      masteryLevel: MasteryLevel.fromValue(dto.masteryLevel),
      createdAt: DateTime.parse(dto.createdAt),
    );
  }
}
```

---

### 7.4 禁止的写法

页面或 Controller 中直接解析 JSON：

```dart
final item = ErrorItem(
  id: json['id'],
  question: json['questionText'],
);
```

Domain Model 直接承担 API 协议：

```dart
class ErrorItem {
  factory ErrorItem.fromJson(Map<String, dynamic> json) {
    // 不建议
  }
}
```

推荐拆分：

```text
ErrorItem
=
应用模型

ErrorItemDto
=
API 模型

ErrorItemRecord
=
本地缓存模型

ErrorItemMapper
=
转换规则
```

---

## 8. 构建层 Build

### 8.1 职责

构建层负责：

```text
Flutter 入口
依赖装配
环境配置
Android Host
Gradle
Makefile
脚本
CI
版本号
签名配置
```

构建层回答：

> 应用如何被组装、配置、编译和安装？

---

### 8.2 包含目录

```text
lib/main.dart
android/
scripts/
tool/build/
Makefile
VERSION
pubspec.yaml
analysis_options.yaml
.github/workflows/
```

---

### 8.3 main.dart 规则

`main.dart` 只负责装配：

```dart
Future<void> main() async {
  final dependencies = await buildDependencies(
    environment: AppEnvironment.fromDefines(),
  );

  runApp(
    BanduApp(dependencies: dependencies),
  );
}
```

禁止在 `main.dart` 中写：

```text
登录逻辑
API 请求
文件读写
页面业务
错题同步
```

---

### 8.4 构建脚本结构

```text
tool/build/
├── check_version.sh
├── prepare_env.sh
└── verify_android.sh

scripts/
├── start-emulator.sh
├── sync-debug-apk.sh
├── install-apk.sh
└── relaunch-app.sh
```

---

### 8.5 Makefile 示例

```makefile
deps:
	flutter pub get

analyze:
	flutter analyze

test:
	flutter test

verify:
	$(MAKE) analyze
	$(MAKE) test
	flutter build apk --debug

apk:
	flutter build apk --release

install:
	adb install -r build/app/outputs/flutter-apk/app-release.apk
```

---

## 9. 各层依赖规则

| 来源层 | 可以依赖 | 禁止依赖 |
|---|---|---|
| 应用层 | 组件层、应用内部抽象 | Dio、存储插件、Camera、MethodChannel |
| 组件层 | Flutter SDK、只读展示模型 | Repository、Controller、框架层 |
| 框架层 | 应用层接口和模型、转换层 | 页面、路由、Widget 状态 |
| 转换层 | 应用层 Domain Model | 页面、Riverpod Controller、插件实例 |
| 构建层 | 所有层 | 被业务层反向依赖 |

推荐关系：

```text
application/domain
        ↑
conversion
        ↑
framework
        ↑
build
```

UI 关系：

```text
components
        ↑
application/presentation
```

---

## 10. 当前目录迁移映射

| 当前路径 | 目标路径 |
|---|---|
| `lib/app/app.dart` | `lib/application/app/bandu_app.dart` |
| `lib/app/app_router.dart` | `lib/application/app/app_router.dart` |
| `lib/app/app_shell.dart` | `lib/application/app/app_shell.dart` |
| `lib/app/bootstrap.dart` | `lib/application/app/bootstrap.dart` 或构建装配 |
| `lib/app/theme/` | `lib/components/design_system/` |
| `lib/features/*` | `lib/application/features/*` |
| `lib/core/widgets/` | `lib/components/` |
| `lib/core/network/` | `lib/framework/network/` |
| `lib/core/database/` | `lib/framework/persistence/` |
| `lib/core/storage/` | `lib/framework/persistence/secure_storage/` |
| `lib/core/camera/` | `lib/framework/camera/` |
| `lib/core/device/` | `lib/framework/device/` |
| `lib/core/sync/` | `lib/framework/sync/` |
| `lib/core/config/` | `lib/framework/config/` |
| Feature 内 DTO | `lib/conversion/api/<feature>/` |
| Feature 内缓存转换 | `lib/conversion/persistence/` |
| `android/`、`scripts/`、`Makefile` | 构建层 |

---

## 11. `lib/core/profile` 的拆分

`profile` 不能整体迁移到单一层，需要按职责拆分。

### 用户资料业务

```text
用户资料状态
资料编辑流程
头像更新流程
```

迁移到：

```text
application/features/profile/
```

### 文件和头像存储

```text
头像文件保存
头像路径读取
缓存清理
```

迁移到：

```text
framework/persistence/profile/
```

### API 数据转换

```text
ProfileDto
ProfileMapper
AvatarResponseDto
```

迁移到：

```text
conversion/api/profile/
```

---

## 12. 实施顺序

不要一次性移动全部目录。

每一步必须保持：

```text
flutter analyze 通过
flutter test 通过
Android 可运行
```

---

### 第一步：组件层

优先迁移：

```text
lib/app/theme/
lib/core/widgets/
重复按钮
重复卡片
Loading / Error / Empty UI
```

目标：

```text
lib/components/
```

风险最低。

---

### 第二步：转换层

提取：

```text
DTO
fromJson / toJson
Mapper
日期转换
枚举转换
错误转换
缓存转换
```

目标：

```text
lib/conversion/
```

这一步为框架层和应用层解耦建立边界。

---

### 第三步：框架层

迁移：

```text
network
storage
database
camera
device
sync
config
RepositoryImpl
```

同时在应用层建立：

```text
Repository Interface
Gateway Interface
Storage Interface
```

---

### 第四步：应用层

迁移：

```text
app
features
Controller
State
UseCase
Domain Model
Repository Interface
```

重点删除：

```text
Controller 对 Dio 的直接依赖
Controller 对 SecureStorage 的直接依赖
Controller 对 Camera Plugin 的直接依赖
Controller 对 File API 的直接依赖
```

---

### 第五步：构建层

整理：

```text
main.dart
依赖装配
环境变量
Makefile
scripts
Android 签名
CI
VERSION
```

最终删除旧目录：

```text
lib/app/
lib/core/
lib/features/
```

---

## 13. 推荐迁移提交顺序

建议拆成多个独立提交：

```text
refactor: add five-layer architecture directories

refactor: move theme and shared widgets to components

refactor: extract api dto and mapper layer

refactor: move network and persistence to framework

refactor: move camera and device adapters to framework

refactor: move features to application layer

refactor: add dependency composition in bootstrap

chore: remove legacy app core and features directories

test: reorganize tests by architecture layer
```

不要在一个提交中同时完成所有移动和业务修改。

---

## 14. 完成验收标准

完成后 `lib/` 只允许存在：

```text
lib/
├── application/
├── components/
├── framework/
├── conversion/
└── main.dart
```

必须满足：

```text
1. 页面和 Controller 不导入 Dio。

2. 页面和 Controller 不导入 Camera Plugin。

3. 页面和 Controller 不导入 FlutterSecureStorage。

4. Components 不导入 application/features。

5. 所有 API JSON 转换只存在于 conversion/api。

6. 所有缓存转换只存在于 conversion/persistence。

7. 所有插件调用只存在于 framework。

8. Repository 接口定义在 application。

9. Repository 实现在 framework。

10. main.dart 或 bootstrap 是唯一装配入口。

11. 不存在 lib/app、lib/core、lib/features 旧目录。

12. flutter analyze 通过。

13. flutter test 通过。

14. flutter build apk --release 通过。

15. Android 实机核心链路正常。
```

Android 核心链路：

```text
注册
  ↓
登录
  ↓
拍照
  ↓
AI 分析
  ↓
保存错题
  ↓
查看详情
  ↓
编辑 / 删除
  ↓
生成练习
```

---

## 15. 当前第一批建议任务

第一批只做低风险迁移：

```text
1. lib/app/theme
   → lib/components/design_system

2. lib/core/widgets
   → lib/components

3. Feature 内 DTO / fromJson / toJson
   → lib/conversion
```

完成并验证后，再开始：

```text
lib/core
→ lib/framework

lib/features
→ lib/application/features
```

---

## 16. 最终结论

五层架构完成后，项目职责如下：

```text
Application
=
业务流程和页面状态

Components
=
可复用 UI 和设计系统

Framework
=
网络、存储、相机、设备和插件实现

Conversion
=
API、缓存、平台数据转换

Build
=
入口、装配、Gradle、脚本和 CI
```

项目最终依赖必须保持单向：

```text
构建层
  ↓
应用层
  ↓
业务抽象

框架层
  ↑
实现业务抽象

转换层
  ↑
服务于框架和业务边界

组件层
  ↑
服务于应用展示
```

后续新增功能必须继续通过架构边界测试，避免应用层重新依赖插件或框架实现。
