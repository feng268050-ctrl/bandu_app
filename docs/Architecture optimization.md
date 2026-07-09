# bandu_app Flutter 重构设计方案

## 一、总体方案

### 1. 推荐架构

```text
┌─────────────────────────────────────────────┐
│                 Flutter App                 │
│                                             │
│  UI / Presentation                          │
│  Riverpod State                             │
│  Domain / UseCase                           │
│  Repository                                 │
│        ↓                    ↓               │
│  Drift Local DB         Dio HTTP Client     │
│  File Storage           Secure Storage      │
└──────────────┬──────────────────────────────┘
               │
               │ HTTP API
               ▼
┌─────────────────────────────────────────────┐
│             Next.js Backend                 │
│                                             │
│  Auth                                       │
│  Error Item                                 │
│  AI Analyze                                 │
│  Practice                                   │
│  Statistics                                 │
│  Tags                                       │
│  Export                                     │
│                                             │
│          Prisma → SQLite                    │
│          AI Provider Adapter                │
│     Gemini / OpenAI / Azure OpenAI          │
└─────────────────────────────────────────────┘

```

核心原则：

```text
Flutter
负责：
UI
相机
图片预览
本地缓存
离线任务
登录状态
接口调用

Next.js
负责：
用户认证
业务数据
AI API Key
AI 分析
数据统计
导出
数据库操作

```

当前项目已经使用 Next.js、Prisma、SQLite、NextAuth，并提供 AI、多错题本、标签、练习、统计、导出等功能，因此这部分继续保留比迁入 Flutter 风险小得多。

---

# 二、项目结构设计

## 1. 仓库结构

重构初期不建议立即调整整个仓库目录，直接增加：

```text
bandu_app/
├── flutter_app/            # 新 Flutter App
│
├── src/                    # 保留 Next.js
├── prisma/                 # 保留数据库
├── config/                 # AI 配置
│
├── android/                # 旧 Android 项目，迁移期间保留
│
├── public/
├── scripts/
└── docker-compose.yml

```

迁移完成以后：

```text
bandu_app/
├── flutter_app/
├── src/
├── prisma/
├── config/
└── docker-compose.yml

```

不建议一开始改造成复杂 Monorepo：

```text
apps/
packages/
services/
shared/

```

个人项目暂时没有必要增加这种管理成本。

---

# 三、Flutter 架构设计

Flutter 官方当前架构指导强调 UI 层和 Data 层分离，并建议通过 View、ViewModel、Repository、Service 明确职责；复杂业务可以增加 Domain/UseCase 层。对于 bandu_app，我建议采用相同思想，但目录组织使用 **Feature First**。

## 推荐目录

```text
flutter_app/
└── lib/
    ├── main.dart
    │
    ├── app/
    │   ├── app.dart
    │   ├── router.dart
    │   ├── bootstrap.dart
    │   └── theme/
    │
    ├── core/
    │   ├── network/
    │   │   ├── api_client.dart
    │   │   ├── auth_interceptor.dart
    │   │   └── api_exception.dart
    │   │
    │   ├── database/
    │   │   ├── app_database.dart
    │   │   └── tables/
    │   │
    │   ├── storage/
    │   │   ├── token_storage.dart
    │   │   └── image_storage.dart
    │   │
    │   ├── sync/
    │   │   ├── sync_service.dart
    │   │   └── pending_task.dart
    │   │
    │   └── widgets/
    │
    └── features/
        ├── auth/
        ├── home/
        ├── capture/
        ├── library/
        ├── error_item/
        ├── practice/
        ├── stats/
        ├── tags/
        ├── tutor/
        ├── profile/
        └── settings/

```

每个业务模块内部：

```text
capture/
├── data/
│   ├── capture_repository_impl.dart
│   ├── capture_remote_service.dart
│   └── capture_local_service.dart
│
├── domain/
│   ├── capture_repository.dart
│   ├── models/
│   └── usecases/
│
└── presentation/
    ├── capture_page.dart
    ├── capture_controller.dart
    ├── capture_state.dart
    └── widgets/

```

这和当前 Android 工程中的业务模块划分基本能够对应：

```text
Android              Flutter
---------------------------------
feature:capture   →  capture
feature:home      →  home
feature:library   →  library
feature:stats     →  stats
feature:tags      →  tags
feature:tutor     →  tutor
feature:profile   →  profile
feature:devices   →  devices，可选

```

现有 Android 工程已经明确存在这些 Feature 模块，因此 Flutter 重构时应迁移“业务能力”，而不是逐个 Compose 页面翻译成 Widget。

---

# 四、App 信息架构

## 推荐底部导航

```text
┌─────────────────────────────┐
│                             │
│          当前页面            │
│                             │
├─────────────────────────────┤
│ 首页  错题本   ＋   练习  我的 │
└─────────────────────────────┘
               ↑
             拍题

```

推荐 4 个主 Tab：

### 首页

```text
今日复习
待复习数量
最近错题
掌握情况
学科统计
快速拍题

```

### 错题本

```text
学科分类
错题列表
搜索
筛选
知识点
掌握状态
错题详情

```

### 练习

```text
智能练习
知识点练习
错题变式题
练习记录
正确率统计
AI Tutor

```

### 我的

```text
个人信息
学习阶段
标签管理
服务器地址
同步状态
AI 设置入口
退出登录

```

统计模块不一定需要占用一个主 Tab。

建议：

```text
首页
  └── 统计摘要

统计详情
  └── 独立二级页面

```

这样更符合手机使用场景。

---

# 五、最核心的拍题流程

这是 Flutter 重构中应该最先打通的完整业务链。

```text
打开拍题页
    ↓
Camera Preview
    ↓
拍照
    ↓
裁剪 / 旋转修正
    ↓
压缩
    ↓
保存本地原图
    ↓
创建 PendingAnalysisTask
    ↓
上传服务端
    ↓
AI 分析
    ↓
返回：
题目文本
答案
解析
错因分析
知识点
标签
GeoGebra Commands
    ↓
用户确认
    ↓
保存错题
    ↓
更新本地缓存

```

Flutter 官方维护的 `camera` 插件支持相机预览、拍照和图像流，适合作为专门拍题页面的基础；插件本身要求 App 正确处理相机生命周期，因此 `capture` 模块应独立管理 CameraController，而不是放进全局状态。

推荐状态机：

```text
idle
  ↓
capturing
  ↓
preview
  ↓
uploading
  ↓
analyzing
  ↓
success

任何阶段失败：

failed
  ↓
retry

```

不要设计成：

```text
bool isLoading

```

因为 AI 分析是明显的多阶段流程。

推荐：

```dart
sealed class AnalyzeStatus {
  const AnalyzeStatus();
}

class Idle extends AnalyzeStatus {}

class Uploading extends AnalyzeStatus {
  final double progress;
}

class Analyzing extends AnalyzeStatus {}

class Success extends AnalyzeStatus {
  final ErrorItem item;
}

class Failed extends AnalyzeStatus {
  final String message;
}

```

---

# 六、数据设计

当前 Prisma 核心数据关系已经比较完整，包括：

```text
User
Subject
ErrorItem
KnowledgeTag
ReviewSchedule
PracticeRecord

```

并且 `ErrorItem` 包含：

```text
originalImageUrl
ocrText
questionText
answerText
analysis
wrongAnswerText
mistakeAnalysis
mistakeStatus
geogebraCommands
source
errorType
userNotes
masteryLevel
gradeSemester
paperLevel

```

知识点采用树形层级，并与错题建立多对多关联。

因此 **Flutter 不应重新设计另一套业务模型**。

---

## Flutter 本地数据库

推荐 Drift。

Drift 是 Flutter/Dart 的响应式关系型持久化方案，基于 SQLite 体系，并支持类型安全查询和迁移机制，适合当前已经具有明显关系型数据结构的项目。

第一阶段只建立：

```text
CachedSubject
CachedErrorItem
CachedKnowledgeTag
PendingTask
SyncMetadata

```

不要第一天就完整复制服务端所有 Prisma 表。

建议职责：

```text
服务端 SQLite
=
最终数据源

Flutter Drift
=
缓存
离线读取
待上传任务
同步状态

```

图片不要存 SQLite BLOB。

使用：

```text
Application Documents/
└── bandu/
    ├── capture/
    │   └── {uuid}/original.jpg
    │
    ├── cache/
    └── temp/

```

Drift 中只保存：

```text
localPath
remoteUrl
uploadStatus

```

---

# 七、离线策略

对于个人使用，我不建议第一版做完整双向离线同步。

## 第一阶段

```text
在线优先
+
本地缓存
+
拍题失败重试

```

具体：

```text
GET 错题列表
    ↓
写 Drift
    ↓
UI 始终读取 Drift

```

拍题：

```text
保存图片到本地
    ↓
写 PendingTask
    ↓
尝试上传
    ↓
失败保留
    ↓
下次启动 / 回到前台时重试

```

网络状态插件只能表示当前网络接口情况，不能保证真正能够访问服务端，因此同步逻辑仍然必须以真实 HTTP 请求成功或失败作为判断依据。

因此不要写：

```text
Wi-Fi = 可以上传

```

而应该写：

```text
检测网络
    ↓
尝试请求
    ↓
请求成功 → 完成任务
请求失败 → 保留 Pending

```

---

# 八、登录注册方案

## 当前问题

现有项目使用：

```text
NextAuth
+
Credentials
+
浏览器 Session/Cookie

```

现有 API 中也有 NextAuth 路由和注册接口。

浏览器继续使用 NextAuth。

Flutter 不建议直接模拟完整网页 Cookie + CSRF 登录流程。

推荐新增：

```text
/api/mobile/v1/auth/

```

接口：

```text
POST /auth/login
POST /auth/register
POST /auth/refresh
POST /auth/logout

GET  /users/me

```

登录：

```http
POST /api/mobile/v1/auth/login

```

```json
{
  "email": "user@example.com",
  "password": "123456"
}

```

返回：

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "user": {
    "id": "...",
    "name": "feng",
    "email": "..."
  }
}

```

建议：

```text
Access Token
短生命周期
Flutter 内存保存

Refresh Token
长生命周期
Secure Storage 保存

```

移动端 Token 可通过安全存储插件写入平台安全存储；该插件在 iOS 侧使用 Keychain 相关机制。

建议给 Prisma 增加：

```prisma
model RefreshToken {
  id         String   @id @default(cuid())
  userId     String
  tokenHash  String
  expiresAt  DateTime
  revokedAt  DateTime?
  createdAt  DateTime @default(now())
}

```

保存：

```text
Hash(refreshToken)

```

不要直接保存明文 Token。

---

# 九、HTTP 层设计

推荐：

```text
Dio

```

结构：

```text
Dio
 ├── AuthInterceptor
 ├── RefreshTokenInterceptor
 ├── ErrorInterceptor
 └── LogInterceptor

```

Dio 原生支持拦截器、取消请求、上传下载和 FormData，适合图片上传以及统一 Token 注入；它也提供队列式拦截机制，可用于避免多个并发请求同时触发 Token Refresh。

流程：

```text
Request
  ↓
读取 AccessToken
  ↓
Authorization: Bearer xxx
  ↓
API

如果 401：
  ↓
Refresh Token
  ↓
刷新成功
  ↓
重放原请求

```

---

# 十、API 重构建议

当前 API 已经覆盖：

```text
admin
ai
analytics
analyze
auth
error-items
export
geogebra-analyze
import
notebooks
practice
register
settings
stats
tags
user
version

```

因此不应该重写全部 API，只需要增加适合 App 的稳定接口层。

推荐：

```text
/api/mobile/v1/
├── auth/
├── users/
├── subjects/
├── error-items/
├── analyze/
├── practice/
├── stats/
├── tags/
└── export/

```

例如：

```text
GET    /error-items
GET    /error-items/{id}
POST   /error-items
PATCH  /error-items/{id}
DELETE /error-items/{id}

POST   /analyze

GET    /tags
POST   /tags

POST   /practice/generate
POST   /practice/records

GET    /stats/overview

```

推荐统一响应：

```json
{
  "data": {},
  "error": null,
  "meta": {}
}

```

失败：

```json
{
  "data": null,
  "error": {
    "code": "ANALYZE_FAILED",
    "message": "AI 分析失败"
  }
}

```

Flutter 只处理：

```text
ErrorCode

```

避免分析：

```text
"AI 分析失败"
"分析出现异常"
"Gemini request failed"

```

这类不断变化的字符串。

---

# 十一、AI 模块处理原则

当前项目支持 Gemini、OpenAI 和 Azure OpenAI，并允许服务端动态配置 AI Provider。

推荐继续保持：

```text
Flutter
    ↓
POST /analyze
    ↓
Next.js AI Service
    ↓
AI Provider Adapter
    ├── Gemini
    ├── OpenAI
    └── Azure

```

不要：

```text
Flutter
    ↓
直接调用 OpenAI/Gemini

```

原因是：

```text
API Key 不应该进入客户端
统一 Prompt 更容易维护
返回格式可以统一校验
更换模型不需要更新 App

```

Flutter 中只认识：

```dart
class AnalyzeResult {
  String question;
  String answer;
  String analysis;
  List<KnowledgeTag> tags;
  MistakeAnalysis? mistake;
  List<String>? geogebraCommands;
}

```

不关心模型是：

```text
Gemini
GPT
Qwen
GLM

```

---

# 十二、GeoGebra 和公式显示

当前数据模型保留了：

```text
geogebraCommands

```

同时后端已经存在：

```text
/api/geogebra-analyze

```

因此建议不要尝试在 Dart 中重新实现几何绘图解释器。

建议封装：

```text
GeoGebraView

```

内部：

```text
WebView
+
本地 HTML
+
GeoGebra JS
+
commands

```

Flutter 官方维护的 `webview_flutter` 提供基于系统 WebView 的组件，可以将此能力限制在一个独立 Widget 内，而不是让 WebView 侵入其他页面。

整体：

```text
ErrorItemDetailPage
├── QuestionCard
├── AnswerCard
├── AnalysisCard
├── KnowledgeTagList
└── GeoGebraView

```

---

# 十三、导出与打印

现有项目已经有：

```text
/api/export

```

并且 Web 功能包含错题导出和打印能力。

Flutter 中不要重新实现复杂 PDF 排版逻辑。

推荐：

```text
Flutter 选择：
学科
标签
掌握状态
日期
是否带答案
是否带解析

        ↓

POST /export

        ↓

Server 生成 PDF

        ↓

Flutter 下载

        ↓

预览 / 系统分享 / 打印

```

跨平台分享插件可以调用 Android 和 iOS 的系统分享界面并支持文件分享。

---

# 十四、推荐技术栈


| 功能     | 推荐                     |
| ------ | ---------------------- |
| UI     | Flutter Material 3     |
| 状态管理   | Riverpod               |
| 路由     | go_router              |
| HTTP   | Dio                    |
| 本地数据库  | Drift                  |
| Token  | flutter_secure_storage |
| Camera | camera                 |
| JSON   | json_serializable      |
| 不可变模型  | Freezed，可选             |
| Web 内容 | webview_flutter        |
| 文件分享   | share_plus             |


状态管理建议使用 Riverpod，但仅用于状态、依赖注入和异步状态组织，不要把所有业务逻辑堆入一个全局 Provider。Riverpod 官方文档提供 Provider、自动销毁、重试、测试覆盖以及 Provider Override 等机制，适合将 Repository 和 Controller 作为清晰依赖注入边界。

路由使用 `go_router`，用于：

```text
登录重定向
嵌套路由
底部 Navigation Shell
错题详情路径
Deep Link

```

`go_router` 是 Flutter 发布方维护的声明式路由包，并支持 URL 路由和 Deep Link 场景。

---

# 十五、Riverpod 使用规则

建议：

```text
Provider
    ↓
Repository

AsyncNotifier
    ↓
页面状态与用户操作

Widget
    ↓
只负责展示和发送 Intent

```

例如：

```text
CapturePage
     ↓
CaptureController
     ↓
AnalyzeErrorItemUseCase
     ↓
ErrorItemRepository
     ↓
RemoteService

```

不要：

```text
CapturePage
  ↓
Dio.post()
  ↓
Drift insert()
  ↓
File.write()

```

Widget 不直接操作数据库和 HTTP。

Flutter 官方架构建议 Views 不包含数据业务逻辑，ViewModel 负责从 Repository 读取数据并转换为 UI 状态；Repository 和 Service 则构成 Data Layer。

---

# 十六、哪些功能第一版不迁移

因为这是个人使用项目，我建议第一版 Flutter **不迁移**：

```text
管理员用户管理
复杂 AI Provider 配置
Docker 管理
系统日志查看
网页打印预览编辑器
复杂数据导入管理后台

```

这些功能继续放在 Web。

Flutter 第一版只做：

```text
登录
注册
首页
拍题
AI 分析
错题列表
错题详情
标签
练习
统计
个人信息

```

这样职责非常明确：

```text
Flutter App
=
日常使用

Web
=
配置和管理

```

---

# 十七、迁移实施顺序

## 阶段 1：固定服务端边界

先完成：

```text
/api/mobile/v1/auth/login
/api/mobile/v1/auth/refresh
/api/mobile/v1/users/me

/api/mobile/v1/error-items
/api/mobile/v1/analyze

```

同时定义：

```text
DTO
ErrorCode
分页格式
日期格式
Token 规则

```

这是 Flutter 开发前最重要的基础。

---

## 阶段 2：Flutter App 骨架

完成：

```text
AppTheme
go_router
Riverpod
Dio
SecureStorage
Drift
登录状态恢复
全局异常处理

```

目标：

```text
启动
 ↓
检查 Token
 ↓
登录页 or 首页

```

---

## 阶段 3：第一条完整闭环

只做：

```text
登录
 ↓
拍照
 ↓
上传
 ↓
AI 分析
 ↓
结果展示
 ↓
保存错题
 ↓
错题列表
 ↓
错题详情

```

先把这一条做到稳定。

不要同时开发：

```text
统计
练习
标签
设备
AI Tutor

```

---

## 阶段 4：知识体系

加入：

```text
Subject
KnowledgeTag
Tag Tree
筛选
搜索
掌握状态
复习计划

```

---

## 阶段 5：练习体系

加入：

```text
智能练习
生成相似题
答题
判定
PracticeRecord
正确率
薄弱知识点

```

---

## 阶段 6：离线能力

加入：

```text
Drift Cache
PendingTask
失败重试
App Resume Sync
增量刷新

```

第一版不需要复杂后台常驻同步。

---

# 十八、最终推荐架构

最终建议项目形态：

```text
                ┌───────────────┐
                │  Flutter App  │
                │ Android / iOS │
                └───────┬───────┘
                        │
                  REST API / JSON
                        │
                ┌───────▼───────┐
                │    Next.js    │
                │ API + Web 管理 │
                └───────┬───────┘
                        │
            ┌───────────┼───────────┐
            │           │           │
            ▼           ▼           ▼
         Prisma       AI Adapter   Export
            │           │
            ▼       Gemini/OpenAI
         SQLite

```

## 我的最终判断

对于当前 `bandu_app`，最合适的不是：

```text
Next.js
    ↓
全部改 Flutter
    ↓
SQLite 也放手机
    ↓
AI 直接从手机调用

```

而是：

```text
现有 Next.js
=
后端 + Web 管理端

现有 Prisma SQLite
=
主数据库

现有 AI 模块
=
继续复用

Flutter
=
新的主力手机客户端

旧 Android 项目
=
Flutter 功能完整后删除

```

当前 Android 工程已经进行了明显的 Feature、Data、Domain 分层，而 Flutter 官方的架构建议也强调 UI、Data 和可选 Domain 层分离，因此最值得继承的是这些业务边界，而不是 Kotlin 或 Compose 的具体代码。