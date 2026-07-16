# bandu_app 公网 API 接入任务清单（单域名方案）

> 项目：`bandu_app`  
> 客户端：Flutter，Android First  
> 公网域名：`aibandu.dpdns.org`  
> 生产 API：`https://aibandu.dpdns.org/api/mobile/v1`  
> 目标：App 在任意 Wi-Fi、手机流量和异地网络下完整使用。
> 实现状态更新：2026-07-16。`[x]` 表示仓库实现或自动化验证已完成；真实网络、账号业务和发布密钥等外部验收仍保留为 `[ ]`。

---

## 1. 最终 API 地址

生产环境固定使用：

```text
https://aibandu.dpdns.org/api/mobile/v1
```

健康检查：

```text
https://aibandu.dpdns.org/api/health
```

不使用：

```text
https://api.aibandu.dpdns.org
10.0.2.2
localhost
127.0.0.1
192.168.x.x
100.x.x.x
```

本地地址只允许用于 Debug 环境。

---

## 2. 目标网络模型

```text
Flutter Android App
        ↓ HTTPS
https://aibandu.dpdns.org/api/mobile/v1
        ↓
Cloudflare Tunnel
        ↓
bandu_web :3000
```

App 不依赖：

```text
Mac
Tailscale
SSH Tunnel
家庭 Wi-Fi
固定公网 IP
```

---

## 3. 环境配置分离

建议：

| 环境 | API 地址 |
|---|---|
| Emulator 本地开发 | `http://10.0.2.2:3000/api/mobile/v1` |
| Tailscale 调试 | `http://<WSL_TAILSCALE_IP>:3000/api/mobile/v1` |
| Production | `https://aibandu.dpdns.org/api/mobile/v1` |

生产配置：

```env
API_BASE_URL=https://aibandu.dpdns.org/api/mobile/v1
BYPASS_AUTH=false
```

任务：

- [x] `AppConfig` 区分 dev、staging、production。
- [x] Production 默认使用根域名 API 路径。
- [x] Release 禁止使用 `api.aibandu.dpdns.org`。
- [x] Release 禁止使用 `10.0.2.2`。
- [x] Release 禁止使用 `localhost`。
- [x] Release 禁止使用 HTTP。
- [x] Release 强制 `BYPASS_AUTH=false`。

---

## 4. 构建命令

Makefile 建议：

```makefile
PROD_API_BASE_URL := https://aibandu.dpdns.org/api/mobile/v1

run-prod:
	flutter run \
		--dart-define=API_BASE_URL=$(PROD_API_BASE_URL) \
		--dart-define=BYPASS_AUTH=false

apk-prod:
	flutter build apk --release \
		--dart-define=API_BASE_URL=$(PROD_API_BASE_URL) \
		--dart-define=BYPASS_AUTH=false
```

任务：

- [x] 增加 `make run-prod`。
- [x] 增加 `make apk-prod`。
- [x] 增加 `make verify-prod-config`。
- [x] README 写明单域名地址。
- [x] 删除所有 API 子域名示例。
- [x] CI 校验生产 API Host。

---

## 5. Release 配置保护

Release 构建必须满足：

```text
scheme = https
host = aibandu.dpdns.org
pathPrefix = /api/mobile/v1
BYPASS_AUTH = false
```

示例：

```dart
void validateProductionConfig() {
  if (!kReleaseMode) return;

  final uri = Uri.parse(apiBaseUrl);

  if (uri.scheme != 'https') {
    throw StateError('Release API must use HTTPS');
  }

  if (uri.host != 'aibandu.dpdns.org') {
    throw StateError('Unexpected production API host');
  }

  if (!uri.path.startsWith('/api/mobile/v1')) {
    throw StateError('Unexpected production API path');
  }

  if (bypassAuth) {
    throw StateError('BYPASS_AUTH must be false in release');
  }
}
```

任务：

- [x] App 启动时执行 Release 校验。
- [x] 构建脚本执行静态校验。
- [x] 错误配置时停止构建或启动。
- [x] 禁止 Release 回退到默认本地地址。

---

## 6. Android HTTPS 配置

任务：

- [x] 主 Manifest 不允许明文 HTTP。
- [x] Release 删除 `usesCleartextTraffic=true`。
- [x] Debug 需要本地 HTTP 时单独允许。
- [x] 不关闭 TLS 验证。
- [x] 不使用接受所有证书的 HttpClient。
- [x] 不硬编码 Cloudflare 证书。

Debug Manifest 可允许：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:usesCleartextTraffic="true" />
</manifest>
```

Production 只能使用：

```text
https://aibandu.dpdns.org
```

---

## 7. 健康检查

服务端地址：

```text
GET https://aibandu.dpdns.org/api/health
```

任务：

- [x] 新增 `HealthApiService`。
- [x] 健康检查使用独立 Base URL 或正确移除 `/api/mobile/v1`。
- [x] 不拼成错误地址：

```text
https://aibandu.dpdns.org/api/mobile/v1/api/health
```

正确：

```text
https://aibandu.dpdns.org/api/health
```

- [x] 健康检查超时设置为 5 秒。
- [x] 登录注册失败时提供“检查服务”。
- [x] 健康检查失败不阻塞离线启动。

---

## 8. URL 拼接规范

统一要求：

```text
Base URL:
https://aibandu.dpdns.org/api/mobile/v1/

相对路径:
auth/login
auth/register
auth/refresh
error-items
analyze
stats
```

最终请求：

```text
https://aibandu.dpdns.org/api/mobile/v1/auth/login
https://aibandu.dpdns.org/api/mobile/v1/auth/register
https://aibandu.dpdns.org/api/mobile/v1/analyze
```

禁止：

```text
重复 /api/mobile/v1
缺少 /api/mobile/v1
使用 API 子域名
```

建议增加 URL 单元测试。

---

## 9. 网络错误统一转换

禁止直接显示：

```text
DioException
SocketException
connection timeout
完整英文堆栈
```

统一用户提示：

| 技术错误 | 用户提示 |
|---|---|
| 无网络 | 当前网络不可用，请检查 Wi-Fi 或移动数据。 |
| DNS 失败 | 无法解析服务地址，请稍后重试。 |
| 连接超时 | 连接服务器超时，请检查网络后重试。 |
| 接收超时 | 服务器处理时间较长，请稍后重试。 |
| 401 | 登录状态已失效，请重新登录。 |
| 403 | 当前账号无权执行此操作。 |
| 404 | 请求的服务不存在，请检查 App 版本。 |
| 429 | 操作过于频繁，请稍后重试。 |
| 500/502/503 | 服务器暂时不可用，请稍后重试。 |
| 未知错误 | 操作失败，请稍后重试。 |

任务：

- [x] Conversion 层实现 `DioException → AppFailure`。
- [x] UI 只显示中文 `userMessage`。
- [x] Debug 日志保留技术原因。
- [x] Release 日志不记录 Token、密码和敏感响应。

---

## 10. 超时策略

| 请求类型 | 连接超时 | 接收超时 |
|---|---:|---:|
| 健康检查 | 5 秒 | 5 秒 |
| 登录/注册 | 10 秒 | 20 秒 |
| 普通查询 | 10 秒 | 20 秒 |
| 图片上传 | 15 秒 | 60 秒 |
| AI 分析 | 15 秒 | 90～180 秒 |

任务：

- [x] 不使用一个固定超时覆盖所有接口。
- [x] AI 分析使用独立长接收超时。
- [x] 上传和分析显示状态。
- [x] 用户可取消长任务。
- [x] 不用增加连接超时掩盖错误地址。

---

## 11. 重试策略

可有限自动重试：

```text
GET 错题列表
GET 错题详情
GET 统计
GET 健康检查
```

不盲目自动重试：

```text
注册
登录
保存错题
删除错题
提交练习
AI 分析
```

任务：

- [x] 查询失败最多重试 1～2 次。
- [x] 采用指数退避。
- [x] 写请求由用户主动重试。
- [x] AI 分析重试由用户确认。
- [x] Pending Task 使用 UUID。
- [x] 保存错题请求支持 Idempotency Key。

---

## 12. 认证流程

- [x] 注册使用 `https://aibandu.dpdns.org/api/mobile/v1/auth/register`。
- [x] 登录使用 `https://aibandu.dpdns.org/api/mobile/v1/auth/login`。
- [x] Refresh 使用同一根域名。
- [x] Refresh Token 存入 Secure Storage。
- [x] 401 自动 Refresh 一次。
- [x] 并发 401 只触发一次 Refresh。
- [x] Refresh 失败后清理会话。
- [x] 退出登录撤销服务端 Token。
- [x] Release 禁止绕过认证。

---

## 13. 注册和登录错误体验

任务：

- [x] Loading 时禁止重复点击。
- [x] 超时后恢复按钮。
- [x] 显示中文错误。
- [x] 提供重试。
- [x] 提供检查服务。
- [x] 保留已输入昵称和邮箱。
- [x] 密码不写入日志。
- [x] 账号已存在显示业务错误。
- [x] 不显示 Cloudflare HTML 错误页原文。

---

## 14. 公网实机测试

网络矩阵：

- [ ] 家庭 Wi-Fi。
- [ ] 手机 4G/5G。
- [ ] 外部 Wi-Fi。
- [ ] Mac 关闭。
- [ ] 手机 Tailscale 关闭。
- [ ] Win11 和 WSL 正常运行。
- [x] Cloudflare Tunnel 正常运行（2026-07-16 从当前开发机验证健康接口为 HTTP 200）。

核心链路：

```text
注册
→ 登录
→ 读取 AI 配置
→ 拍照
→ 上传
→ AI 分析
→ 保存错题
→ 查看详情
→ 编辑
→ 删除
→ 生成练习
→ 提交练习记录
```

---

## 15. 离线缓存

无网络时允许：

```text
打开 App
查看缓存错题
查看缓存详情
查看本地设置
查看待上传任务
```

任务：

- [x] 缓存错题列表。
- [x] 缓存错题详情。
- [x] 缓存统计摘要。
- [x] 标记离线数据。
- [x] 网络恢复后刷新。
- [x] 缓存损坏时安全重建。
- [x] 设置缓存清理策略。

---

## 16. Pending Task 持久化

```text
拍照
→ 保存本地图片
→ 创建 Pending Task
→ 尝试上传
├─ 成功：完成并清理
└─ 失败：保留
```

任务：

- [x] App 退出后任务不丢失。
- [x] 启动时恢复任务。
- [x] 网络恢复时自动重试。
- [x] 支持手动重试。
- [x] 支持删除失败任务。
- [x] 限制最大重试次数。
- [x] 使用请求 UUID。
- [x] 成功后清理临时图片。

---

## 17. 网络诊断页面

入口：

```text
我的
→ 设置
→ 网络诊断
```

展示：

```text
API Host：aibandu.dpdns.org
API Base Path：/api/mobile/v1
HTTPS 状态
健康检查结果
最近一次请求时间
App 版本
构建模式
```

禁止展示：

```text
Access Token
Refresh Token
AI Key
密码
服务端路径
```

实现状态：

- [x] “我的”页面提供网络诊断入口。
- [x] 展示 Host、Base Path、HTTPS、健康结果、最近请求、版本和构建环境。
- [x] 页面不读取或展示 Token、AI Key、密码及敏感响应。

---

## 18. Release 签名

即使暂不做 OTA，也应完成：

- [ ] 创建固定 Release Keystore。
- [ ] Keystore 至少备份两份。
- [x] `key.properties` 不提交 Git。
- [x] Release 不再使用 Debug 签名。
- [x] 固定 applicationId。
- [ ] versionCode 持续递增。
- [ ] 验证正式 APK 可覆盖上一正式版本。

---

## 19. CI 校验

CI 增加：

```text
flutter pub get
flutter analyze
flutter test
flutter build apk --debug
生产配置静态检查
```

校验：

- [x] Host 必须为 `aibandu.dpdns.org`。
- [x] Path 必须为 `/api/mobile/v1`。
- [x] Scheme 必须为 HTTPS。
- [x] `BYPASS_AUTH=false`。
- [x] Release Manifest 禁止明文 HTTP。
- [x] 不存在 `api.aibandu.dpdns.org`。
- [x] 不向用户展示 DioException。

---

## 20. 验收清单

### 配置

- [x] Production API 是 `https://aibandu.dpdns.org/api/mobile/v1`。
- [x] 健康检查是 `https://aibandu.dpdns.org/api/health`。
- [x] 不使用子域名。
- [ ] Release 不包含本地、局域网和 Tailscale 地址。

### 网络

- [ ] Wi-Fi 可用。
- [ ] 4G/5G 可用。
- [ ] 异地网络可用。
- [ ] Tailscale 关闭后可用。
- [ ] Mac 关闭后可用。

### 错误体验

- [x] 不显示 DioException。
- [x] 超时提示中文。
- [x] 401 自动 Refresh。
- [x] 服务不可用可重试。
- [x] 离线可查看缓存。

### 业务

- [ ] 注册成功。
- [ ] 登录成功。
- [ ] AI 配置读取成功。
- [ ] 图片上传成功。
- [ ] AI 分析成功。
- [ ] 错题保存成功。
- [ ] 练习提交成功。

---

## 21. 完成定义

```text
https://aibandu.dpdns.org
=
唯一公网域名

https://aibandu.dpdns.org/api/mobile/v1
=
唯一生产 Mobile API

https://aibandu.dpdns.org/api/health
=
唯一健康检查地址

App
=
不依赖子域名、Tailscale、Mac 和局域网
```
