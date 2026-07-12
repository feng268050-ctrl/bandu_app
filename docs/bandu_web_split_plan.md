# bandu_app Web 端拆分与 Flutter 主客户端迁移方案

## 1. 背景

当前 `bandu_app` 仓库在迁移阶段同时包含：

```text
bandu_app/
├── src/            # Next.js Web 页面 + API Route
├── prisma/         # Prisma Schema / SQLite 数据库
├── config/         # 服务端配置、AI 配置
├── public/         # Web 静态资源
├── scripts/        # Web/服务端脚本
├── flutter_app/    # Flutter 手机 App
└── android/        # 旧 Kotlin/Compose Android App
```

下一阶段目标：

```text
1. Web 端代码独立迁移到：
   https://github.com/feng268050-ctrl/bandu_web

2. 当前仓库只保留 Flutter App 主客户端。

3. 旧 android/ 原生 Android App 彻底遗弃。

4. 暂时不做 OTA。

5. Web 端暂时只承担：
   - 服务端 API
   - 数据库
   - AI 分析能力
   - 分析结果查看页面
```

---

## 2. 最终项目边界

### 2.1 bandu_app

后续定位：

```text
bandu_app
=
Flutter 手机 App
=
主客户端
=
优先适配 Android 手机
```

保留内容：

```text
bandu_app/
├── lib/
├── android/          # Flutter Android 平台工程，不是旧原生 Android App
├── ios/              # 可保留，但暂不作为 P0
├── test/
├── integration_test/
├── assets/
├── pubspec.yaml
├── analysis_options.yaml
├── README.md
└── Makefile
```

职责：

```text
登录 / 注册
首页
拍题
AI 分析结果展示
保存错题
错题列表
错题详情
练习
统计
个人中心
本地缓存
失败重试
Android 实机适配
```

不负责：

```text
数据库主存储
AI API Key 管理
AI Provider 调度
服务端业务聚合
Web 页面
管理员后台
PDF 导出
OTA
```

---

### 2.2 bandu_web

仓库地址：

```text
https://github.com/feng268050-ctrl/bandu_web
```

后续定位：

```text
bandu_web
=
Web 查看端
+
Backend API
+
AI / 数据库 / 服务端能力
```

迁入内容：

```text
bandu_web/
├── src/
│   ├── app/
│   │   ├── api/
│   │   │   └── mobile/
│   │   │       └── v1/
│   │   └── viewer/
│   ├── components/
│   ├── contexts/
│   ├── lib/
│   └── types/
│
├── prisma/
├── config/
├── public/
├── scripts/
├── e2e/
├── Dockerfile
├── docker-compose.yml
├── package.json
├── package-lock.json
├── next.config.ts
├── tsconfig.json
├── vitest.config.ts
├── playwright.config.ts
└── README.md
```

职责：

```text
Mobile API
认证核心逻辑
用户数据
错题数据
AI 分析
练习生成
统计聚合
标签数据
分析结果查看 Web 页面
数据库迁移
Docker 部署
```

暂时不做：

```text
复杂 Web 管理后台
完整 Web 错题编辑
Web 端拍题
Web 端练习
Web 端 OTA 管理
```

---

## 3. 旧 Android App 处理原则

需要彻底遗弃的是当前旧目录：

```text
android/
```

该目录代表：

```text
Kotlin
Jetpack Compose
原生 Android App
```

它不再继续开发，不再维护新功能。

但 Flutter 项目里的：

```text
android/
```

必须保留。

它代表：

```text
Flutter Android Host
Gradle 配置
AndroidManifest
MainActivity
权限配置
签名配置
APK 构建配置
```

两者不能混淆。

---

## 4. 拆分后的依赖关系

```text
┌──────────────────────────────┐
│          bandu_app            │
│        Flutter Android        │
│                              │
│  UI / Camera / Cache / Auth   │
└───────────────┬──────────────┘
                │
                │ HTTP
                │ API_BASE_URL
                ▼
┌──────────────────────────────┐
│          bandu_web            │
│                              │
│  Mobile API / AI / Prisma     │
│  SQLite / Viewer / Docker     │
└──────────────────────────────┘
```

Flutter 只通过接口访问 Web 服务端：

```text
API_BASE_URL=http://<server-ip>:3000/api/mobile/v1
```

禁止 Flutter 直接依赖：

```text
../src
../prisma
../config
../scripts
```

---

## 5. 迁移步骤

## 阶段 1：冻结旧 Android

执行：

```bash
git tag legacy-android-final
git push origin legacy-android-final
```

处理原则：

```text
android/ 不再开发新功能
android/ 不再修非阻塞问题
android/ 只作为历史参考
```

后续完成 Flutter 验收后，删除旧 `android/`。

---

## 阶段 2：创建 bandu_web 仓库内容

目标仓库：

```text
https://github.com/feng268050-ctrl/bandu_web
```

迁移内容：

```text
src/
prisma/
config/
public/
scripts/
e2e/
permissions/
openspec/
Dockerfile
docker-compose.yml
docker-compose.https.yml
docker-entrypoint.sh
https-server.js
package.json
package-lock.json
next.config.ts
tsconfig.json
vitest.config.ts
playwright.config.ts
postcss.config.mjs
eslint.config.mjs
components.json
.env.example
```

不迁移：

```text
flutter_app/
android/
Flutter 相关 Make target
旧 Android Gradle / adb 脚本
```

---

## 阶段 3：整理 bandu_web README

`bandu_web` README 应只说明：

```text
项目定位
本地启动
Docker 启动
数据库初始化
AI 配置
Mobile API
Web 查看页面
```

不再出现：

```text
Android 原生安装
Flutter 安装
ADB 调试
手机 APK 安装
OTA
```

---

## 阶段 4：整理 bandu_app 为 Flutter 项目

当前 `flutter_app/` 应提升为仓库根目录。

目标结构：

```text
bandu_app/
├── lib/
├── android/
├── ios/
├── test/
├── integration_test/
├── assets/
├── pubspec.yaml
├── analysis_options.yaml
├── README.md
└── Makefile
```

如果当前还没有 Flutter 平台工程，需要执行：

```bash
flutter create --platforms=android,ios .
```

并提交：

```text
android/
ios/
```

其中 Android 是 P0，iOS 只是保留后续兼容空间。

---

## 阶段 5：清理 bandu_app 中的 Web 内容

从 `bandu_app` 删除：

```text
src/
prisma/
config/
public/
scripts/
e2e/
permissions/
openspec/
Dockerfile
docker-compose.yml
docker-compose.https.yml
docker-entrypoint.sh
https-server.js
package.json
package-lock.json
next.config.ts
tsconfig.json
vitest.config.ts
playwright.config.ts
postcss.config.mjs
eslint.config.mjs
components.json
.env.example
```

保留 Flutter 必需文件。

---

## 阶段 6：补齐 Flutter 核心闭环

删除旧 Android 前，Flutter 至少需要完成：

```text
1. 登录
2. 注册
3. 退出登录
4. Token 恢复
5. 拍照
6. AI 分析
7. 分析结果确认
8. 保存错题
9. 错题列表
10. 错题详情
11. 编辑错题
12. 删除错题
13. 练习生成
14. 练习答题记录
15. Android 真机可运行
```

暂时不阻塞删除旧 Android 的功能：

```text
OTA
Web 完整管理端
PDF 导出
复杂标签管理
高级统计图表
AI Tutor
iOS 真机适配
```

---

## 6. Mobile API 边界

`bandu_web` 需要稳定提供：

```text
/api/mobile/v1
```

建议接口：

```text
auth/
├── login
├── register
├── refresh
└── logout

users/
└── me

analyze/

error-items/
├── GET list
├── GET detail
├── POST create
├── PATCH update
└── DELETE delete

practice/
├── generate
├── submit
└── history

stats/
└── overview

tags/
subjects/
```

Flutter 只消费 Mobile API，不访问 Web 内部实现。

---

## 7. Makefile 拆分

### bandu_app Makefile

只保留 Flutter 命令：

```makefile
run:
	flutter run --dart-define=API_BASE_URL=$(API_BASE_URL)

apk:
	flutter build apk --release --dart-define=API_BASE_URL=$(API_BASE_URL)

test:
	flutter test

analyze:
	flutter analyze

clean:
	flutter clean
```

删除：

```text
install-android
build-debug
adb relaunch
旧 Android Gradle 命令
Next.js 命令
Docker 命令
```

---

### bandu_web Makefile

只保留 Web / Server 命令：

```makefile
dev:
	npm run dev

build:
	npm run build

test:
	npm test

db:
	npx prisma migrate dev

docker-up:
	docker compose up -d
```

删除：

```text
flutter-run
install-app
adb
Android APK build
```

---

## 8. 环境变量拆分

### bandu_app

只需要：

```text
API_BASE_URL
```

示例：

```bash
flutter run --dart-define=API_BASE_URL=http://192.168.1.10:3000/api/mobile/v1
```

---

### bandu_web

继续管理：

```text
DATABASE_URL
NEXTAUTH_SECRET
NEXTAUTH_URL
AUTH_TRUST_HOST
AI_PROVIDER
GOOGLE_API_KEY
GEMINI_MODEL
OPENAI_API_KEY
OPENAI_BASE_URL
OPENAI_MODEL
AZURE_OPENAI_API_KEY
```

AI Key、数据库地址、Auth Secret 不进入 Flutter 项目。

---

## 9. 验收标准

## bandu_web 验收

独立 clone 后：

```bash
npm install
cp .env.example .env
npx prisma migrate dev
npm run dev
```

必须可用：

```text
Web 查看页面
/api/mobile/v1/auth/login
/api/mobile/v1/auth/register
/api/mobile/v1/users/me
/api/mobile/v1/analyze
/api/mobile/v1/error-items
```

且仓库中不存在：

```text
flutter_app/
android/ 旧原生 App
adb 安装说明
Flutter 构建说明
```

---

## bandu_app 验收

独立 clone 后：

```bash
flutter pub get
flutter analyze
flutter test
flutter run --dart-define=API_BASE_URL=http://192.168.1.10:3000/api/mobile/v1
```

Android 真机必须跑通：

```text
注册
登录
拍照
AI 分析
保存错题
查看错题列表
查看错题详情
编辑错题
删除错题
生成练习
记录答题
退出登录
重新登录
```

且仓库中不存在：

```text
src/
prisma/
config/
Dockerfile
docker-compose.yml
Next.js package.json
旧 Kotlin/Compose android/
```

---

## 10. 删除旧 Android 的条件

满足以下条件后，可以删除旧 Android App：

```text
1. Flutter Android 平台工程已提交
2. Flutter App 可独立 build APK
3. Flutter 核心链路通过 Android 真机测试
4. bandu_web 已承接服务端和 Web 查看能力
5. 旧 Android 已打 tag 归档
6. README 不再推荐旧 Android 作为主客户端
```

删除方式：

```bash
git tag legacy-android-final
git push origin legacy-android-final

git rm -r android
git commit -m "chore: remove legacy native android app"
```

---

## 11. 推荐最终状态

```text
bandu_app
=
Flutter 手机端
Android First
只保留移动端代码

bandu_web
=
Server + Web Viewer
负责 API / AI / DB / 分析查看

legacy Android
=
只保留 Git tag
不再维护
```

最终目标不是三端并行，而是：

```text
Flutter App
    +
bandu_web Server / Viewer
```

