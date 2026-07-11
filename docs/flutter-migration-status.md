# Flutter 迁移收尾状态

## 当前边界

Flutter 客户端作为日常移动端入口，负责登录注册、拍题、错题本、练习、首页统计、本地缓存和发布安装。

Next.js 后端继续负责账号、AI Key、AI 分析、错题数据、标签、练习记录、统计聚合、导出和数据库写入。

## 已补齐的移动端 API

- `POST /api/mobile/v1/auth/register`
- `POST /api/mobile/v1/auth/login`
- `POST /api/mobile/v1/auth/refresh`
- `POST /api/mobile/v1/auth/logout`
- `GET /api/mobile/v1/users/me`
- `GET /api/mobile/v1/subjects`
- `POST /api/mobile/v1/subjects`
- `GET /api/mobile/v1/tags`
- `POST /api/mobile/v1/tags`
- `GET /api/mobile/v1/error-items`
- `POST /api/mobile/v1/error-items`
- `GET /api/mobile/v1/error-items/{id}`
- `PATCH /api/mobile/v1/error-items/{id}`
- `DELETE /api/mobile/v1/error-items/{id}`
- `POST /api/mobile/v1/analyze`
- `POST /api/mobile/v1/practice/generate`
- `POST /api/mobile/v1/practice/records`
- `GET /api/mobile/v1/stats/overview`

## 功能闭环

拍题页现在按以下链路工作：

```text
拍照/相册
  -> 本地保存原图
  -> POST /analyze
  -> 展示 AI 分析结果
  -> 用户确认保存
  -> POST /error-items
  -> 写入服务端错题
  -> 写入 Flutter 本地缓存
  -> 错题本刷新
```

练习页现在按以下链路工作：

```text
读取错题本最新错题
  -> POST /practice/generate
  -> 展示变式题
  -> 查看答案/解析
  -> POST /practice/records
  -> 首页统计刷新
```

## 本地缓存

Flutter 缓存位置：

```text
ApplicationSupportDirectory/bandu/cache/app_cache_v1.json
```

当前缓存内容：

- 错题列表摘要
- 错题详情

策略：

- 列表和详情远程优先。
- 远程失败时读取本地缓存。
- 拍题保存成功后立即写入本地缓存。

## 发布安装

调试运行：

```bash
API_BASE_URL=http://<server-ip>:3000/api/mobile/v1 make install-app
```

只构建 release APK：

```bash
API_BASE_URL=http://<server-ip>:3000/api/mobile/v1 make flutter-build-release
```

构建并安装 release APK：

```bash
ADB_SERIAL=<serial> API_BASE_URL=http://<server-ip>:3000/api/mobile/v1 make install-app-release
```

## 剩余迁移项

- Drift 表结构和迁移脚本替换当前 JSON 文件缓存。
- 拍题失败后的 pending task 自动重试。
- 标签管理页和统计详情页入口完善。
- PDF/导出/打印能力从后端 API 接入 Flutter。
