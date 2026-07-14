# bandu_app Win11 / WSL / Tailscale 部署与验收方案

> 目标：利用闲置 Win11 作为 `bandu_web` 常驻服务端，Mac 继续负责 Flutter 开发和 Android Emulator 调试。  
> 网络：Mac 已通过 Tailscale 连接 Win11 的 WSL。  
> 适用项目：
>
> - Flutter 客户端：`bandu_app`
> - Web / Server：`bandu_web`

---

## 0. 2026-07-14 Mac 侧验收结论

当前验收目标：

```text
WSL Tailscale IPv4：100.69.41.14
MagicDNS：desktop-wsl.tail5143ee.ts.net
Mobile API：http://100.69.41.14:3000/api/mobile/v1
```

实测结果：

```text
Tailscale 节点在线，Mac 到 WSL 为直连，延迟约 6 ms
Mac 到 Mobile API：连接约 15 ms，首字节约 33 ms
Emulator 到 WSL：ICMP 0% 丢包，3000/TCP 可连接
Flutter 静态分析无问题，37 项测试通过，Debug APK 已安装到 emulator-5556
应用启动后显示真实登录页，BYPASS_AUTH=false 已生效
Debug 合并清单允许当前 HTTP；Release 合并清单未启用明文流量
22/TCP 和 3000/TCP 开放；80/TCP、443/TCP 关闭
未认证访问 users/me、ai-configs、tutor/models 均返回 401
错误密码返回统一 INVALID_CREDENTIALS，不泄露账号是否存在
Access Token、Refresh Token 登录正常，Refresh Token 会轮换且旧令牌不可重放
注销后 Refresh Token 立即失效
AI 配置新增、掩码读取、空密钥编辑保留、Tutor 模型联动和删除清理均通过
API 响应未返回明文 API Key 或 apiKeyCiphertext
```

当前判定：

```text
开发联调可用性：通过
Mobile API 功能闭环：通过
长期无人值守运行：未通过，当前仍是 tmux + Next dev
安全验收：有条件通过，完成下列 P0 项后才能保存真实 AI Key
```

P0 安全门槛：

```text
1. 将 WSL .env 中的 NEXTAUTH_SECRET 和 MOBILE_AUTH_SECRET 更换为独立随机值
2. 立即修改 admin@localhost 的默认密码
3. 在 Tailscale 管理后台确认 ACL 只允许指定用户/设备访问 WSL
4. 保存首个真实 AI Key 前固定 MOBILE_AUTH_SECRET；之后修改会导致已加密 Key 无法解密
```

已知残余风险：

```text
Mobile 登录接口当前没有失败次数限流
注销只撤销 Refresh Token，已签发 Access Token 最长仍可使用 15 分钟
Next 响应暂未配置 CSP、X-Frame-Options、X-Content-Type-Options 等安全头
当前 443 未监听，发布构建不能使用现有 HTTP 地址
```

这些风险在“仅限受控 Tailnet 的个人开发环境”中可以暂时接受；扩大用户或网络范围前必须修复。

---

## 1. 当前问题

Flutter App 当前请求：

```text
http://10.0.2.2:36508
```

Android Emulator 中：

```text
10.0.2.2
=
运行模拟器的宿主机
```

当前模拟器运行在 Mac 上，因此：

```text
10.0.2.2
=
Mac 本机
```

它不是 Win11，也不是 WSL。

当前报错：

```text
Connection refused
```

说明 Mac 本地 `36508` 端口没有运行 `bandu_web` 服务。

---

## 2. 推荐架构

```text
Win11
└── WSL
    ├── bandu_web
    ├── Mobile API
    ├── Prisma / SQLite
    ├── AI 配置
    ├── AI API Key
    └── Tailscale

Mac
├── bandu_app
├── Flutter SDK
├── Android Emulator
├── Android Studio
└── Tailscale
```

网络路径：

```text
Android Emulator
        ↓
Mac 网络栈
        ↓
Mac Tailscale
        ↓
WSL Tailscale IP
        ↓
bandu_web :3000
```

职责分工：

```text
Win11 / WSL
=
服务端、数据库、AI 配置、长期运行

Mac
=
Flutter 开发、模拟器、界面调试、APK 构建
```

---

## 3. Win11 / WSL 部署 bandu_web

### 3.1 克隆项目

进入 WSL：

```bash
mkdir -p ~/projects
cd ~/projects

git clone https://github.com/feng268050-ctrl/bandu_web.git
cd bandu_web
```

### 3.2 初始化环境

```bash
cp .env.example .env
npm ci
npx prisma generate
```

先生成两个不同的随机密钥，并分别写入 `.env`：

```bash
openssl rand -hex 32
openssl rand -hex 32
```

```env
NEXTAUTH_SECRET=<第一个随机值>
MOBILE_AUTH_SECRET=<第二个随机值>
```

`MOBILE_AUTH_SECRET` 用于加密用户保存的 AI API Key。保存第一个真实 Key 前必须固定该值；后续直接更换会导致已有 Key 无法解密。

首次部署且数据库为空时：

```bash
npx prisma db push
npm exec prisma -- db seed
```

已有数据库的日常升级应执行已提交的迁移，不能继续使用 `db push`：

```bash
npx prisma migrate deploy
```

Seed 创建的开发管理员为 `admin@localhost / 123456`。首次登录后必须立即修改密码，不能把该口令保留在常驻环境。

本地临时调试可运行：

```bash
make dev
```

`make dev` 启动的是 Next 开发服务器，只适合调试；长期常驻必须先 `npm run build`，再使用 `make start`。

### 3.3 确认服务监听

服务应监听：

```text
0.0.0.0:3000
```

检查：

```bash
ss -lntp | grep ':3000'
```

WSL 本机验证：

```bash
curl -I http://127.0.0.1:3000
```

如果 Mobile API 有健康检查接口，也应单独验证：

```bash
curl -v http://127.0.0.1:3000/api/mobile/v1
```

---

## 4. 获取 WSL Tailscale 地址

在 WSL 执行：

```bash
tailscale ip -4
```

本次验收地址：

```text
100.69.41.14
```

后续 Flutter 使用：

```text
http://100.69.41.14:3000/api/mobile/v1
```

也可以使用 Tailscale MagicDNS 名称，例如：

```text
http://desktop-wsl.tail5143ee.ts.net:3000/api/mobile/v1
```

初期建议优先使用 `100.x.x.x` 地址，先排除 DNS 问题。

---

## 5. 从 Mac 验证 WSL 服务

Mac 终端执行：

```bash
tailscale ping 100.69.41.14
curl --connect-timeout 5 --max-time 15 -I http://100.69.41.14:3000
```

再测试 Mobile API：

```bash
curl --connect-timeout 5 --max-time 15 \
  http://100.69.41.14:3000/api/mobile/v1/ai-configs
```

可能返回：

```text
200
401
403
```

以上都表示网络已经连通。

以下表示仍有网络或监听问题：

```text
Connection refused
Timeout
No route to host
```

---

## 6. Flutter 配置

在 Mac 的 `bandu_app/.env` 中配置：

```env
API_BASE_URL=http://100.69.41.14:3000/api/mobile/v1
BYPASS_AUTH=false
```

`BYPASS_AUTH=true` 只允许用于不访问服务端的静态 UI 调试。它只跳过 Flutter 客户端登录流程，服务端 Mobile API 仍要求 Bearer Token，因此不能用于 WSL 联调。

如需临时切换到纯 UI 模式：

```env
BYPASS_AUTH=true
```

其他环境应将 `100.69.41.14` 替换为对应 WSL Tailscale IP。

然后重新构建并安装：

```bash
make sync
```

如果模拟器未启动：

```bash
make emulator
```

注意：

```text
API_BASE_URL
```

通常通过 `--dart-define` 在构建时注入，修改 `.env` 后不能只依赖 Hot Reload，必须重新构建 APK。

---

## 7. 临时兼容现有 10.0.2.2:36508

如果暂时不想重新构建 App，可以在 Mac 建立 SSH 隧道：

```bash
ssh -N \
  -L 36508:127.0.0.1:3000 \
  <WSL用户名>@<WSL的Tailscale-IP>
```

示例：

```bash
ssh -N \
  -L 36508:127.0.0.1:3000 \
  feng@100.69.41.14
```

效果：

```text
Flutter App
请求 10.0.2.2:36508
        ↓
Mac 127.0.0.1:36508
        ↓
SSH Tunnel
        ↓
WSL 127.0.0.1:3000
        ↓
bandu_web
```

该方案适合临时调试。

长期仍建议直接配置：

```env
API_BASE_URL=http://<WSL_TAILSCALE_IP>:3000/api/mobile/v1
```

---

## 8. 让 bandu_web 常驻运行

### 8.1 启用 WSL systemd

检查：

```bash
systemctl status
```

未启用时编辑：

```bash
sudo nano /etc/wsl.conf
```

写入：

```ini
[boot]
systemd=true
```

然后在 Windows PowerShell 执行：

```powershell
wsl --shutdown
```

重新进入 WSL。

---

### 8.2 创建 systemd 服务

先在服务目录完成生产构建：

```bash
cd ~/projects/bandu_web
npm ci
npx prisma generate
npx prisma migrate deploy
npm run build
```

确认信息：

```bash
whoami
realpath ~/projects/bandu_web
command -v make
command -v npm
```

创建服务：

```bash
sudo nano /etc/systemd/system/bandu-web.service
```

示例：

```ini
[Unit]
Description=Bandu Web Service
After=network-online.target tailscaled.service
Wants=network-online.target

[Service]
Type=simple
User=feng
WorkingDirectory=/home/feng/projects/bandu_web
Environment=NODE_ENV=production
ExecStart=/usr/bin/make start
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

替换：

```text
User
WorkingDirectory
ExecStart
```

为实际值。

不要在 systemd 中运行 `make dev`。开发服务器包含热更新和额外诊断开销，不适合作为长期无人值守服务。

启用服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now bandu-web
```

查看状态：

```bash
systemctl status bandu-web
```

查看日志：

```bash
journalctl -u bandu-web -f
```

---

### 8.3 NVM 环境注意事项

如果 Node.js 使用 NVM 安装，systemd 可能找不到 `node` 或 `npm`。

查看路径：

```bash
which node
which npm
```

在 Service 中增加：

```ini
Environment="PATH=/home/feng/.nvm/versions/node/vXX/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
```

将 `vXX` 替换为实际 Node 版本目录。

---

## 9. Win11 开机后的 WSL 启动问题

即使 systemd 服务设置为自动启动，WSL 实例本身也需要被启动。

可在 Windows 任务计划程序中增加：

```powershell
wsl.exe -d <发行版名称> --exec /bin/true
```

或者：

```powershell
wsl.exe -d Ubuntu
```

建议设置：

```text
触发器：Windows 登录时
操作：启动 WSL
```

这样：

```text
Win11 登录
    ↓
WSL 启动
    ↓
systemd 启动
    ↓
bandu-web.service 自动启动
```

---

## 10. Windows 防火墙

如果 Tailscale 运行在 WSL 内，通常通过 WSL 的 Tailscale IP 直接访问。

如果 Tailscale 只运行在 Windows，而不是 WSL，需要额外处理：

```text
Windows → WSL 端口转发
Windows 防火墙开放端口
```

当前优先推荐：

```text
Mac Tailscale
        ↔
WSL Tailscale
```

避免通过 Windows NAT 转发。

检查 WSL 是否直接运行 Tailscale：

```bash
tailscale status
tailscale ip -4
```

---

## 11. 安全建议

### 11.1 不向公网开放

开发阶段只允许：

```text
Tailscale 网络访问
```

不建议将 `3000` 端口直接暴露到公网。还必须在 Tailscale 管理后台核对 ACL/Grant，只允许指定用户和设备访问 WSL；节点在线不等于访问策略已经最小化。

### 11.2 账号和服务端密钥

必须满足：

```text
NEXTAUTH_SECRET 与 MOBILE_AUTH_SECRET 使用两个不同的随机值
admin@localhost 不再使用默认密码 123456
MOBILE_AUTH_SECRET 在保存首个真实 AI Key 前固定并纳入安全备份
WSL 的 .env 不提交 Git，不通过聊天或日志传输
```

当前 Mobile 登录接口没有失败次数限流，注销也只会立即撤销 Refresh Token；已签发的 Access Token 最长仍可使用 15 分钟。扩大到多人使用前，应增加账号/IP 限流和 Access Token 撤销机制。

### 11.3 API Key 只放服务端

AI Key 应保存在：

```text
bandu_web/.env
服务端数据库
服务端配置
```

不能放入：

```text
bandu_app
Flutter 源码
APK
dart-define
```

Mobile API 返回配置时只能提供固定掩码和 `hasApiKey` 状态，不能返回明文 Key 或数据库密文。本次黑盒验收已覆盖新增、读取、更新保留和删除流程。

### 11.4 HTTP 与响应头边界

Tailscale 节点间流量由 Tailnet 加密，因此当前 HTTP 地址可用于受控设备上的开发联调。Flutter 主清单和 Release 构建不应全局允许明文 HTTP；本项目仅在 `android/app/src/debug/AndroidManifest.xml` 开启该能力。

发布构建应先通过 Tailscale Serve 或反向代理提供 HTTPS，再将 `API_BASE_URL` 改为 `https://.../api/mobile/v1`。服务端还应补充 CSP、`X-Frame-Options`、`X-Content-Type-Options` 和 `Referrer-Policy` 等响应头。

### 11.5 数据库备份

SQLite 运行中不要直接 `cp` 主数据库。使用 SQLite 在线备份接口并校验备份文件：

```bash
mkdir -p ~/backups/bandu
backup="$HOME/backups/bandu/dev-$(date +%F-%H%M%S).db"
sqlite3 prisma/dev.db ".backup '$backup'"
sqlite3 "$backup" "PRAGMA integrity_check;"
```

输出必须为 `ok`。后续应使用 systemd timer 定期执行，并将备份复制到另一台设备或独立磁盘。

---

## 12. 推荐开发流程

### Win11 / WSL

```bash
cd ~/projects/bandu_web
git pull
npm ci
npx prisma generate
npx prisma migrate deploy
npm run build
sudo systemctl restart bandu-web
```

查看日志：

```bash
journalctl -u bandu-web -f
```

### Mac

```bash
cd ~/projects/bandu_app
git pull
flutter pub get
make emulator
make sync
```

Mac 的 `.env` 应保持：

```env
API_BASE_URL=http://100.69.41.14:3000/api/mobile/v1
BYPASS_AUTH=false
```

网络验证：

```bash
curl -v http://<WSL_TAILSCALE_IP>:3000
```

---

## 13. 故障排查顺序

### 13.1 Flutter 显示 Connection refused

检查：

```text
1. WSL 的 bandu_web 是否启动
2. 是否监听 0.0.0.0:3000
3. Mac 能否 curl WSL Tailscale IP
4. Flutter API_BASE_URL 是否正确
5. 是否重新构建 APK
```

命令：

```bash
# WSL
ss -lntp | grep ':3000'
curl -v http://127.0.0.1:3000

# Mac
curl -v http://<WSL_TAILSCALE_IP>:3000
```

---

### 13.2 Mac 能访问，但 Emulator 不能访问

优先检查 Flutter 配置：

```env
API_BASE_URL=http://<WSL_TAILSCALE_IP>:3000/api/mobile/v1
```

不要使用：

```text
10.0.2.2
```

访问远程 WSL。

当前 WSL 只提供 HTTP，因此 Debug 构建需要允许明文请求。

本项目只在：

```text
android/app/src/debug/AndroidManifest.xml
```

配置：

```xml
<application
    android:usesCleartextTraffic="true">
```

不要把该配置放回 `src/main`，否则 Release APK 也会全局允许明文流量。生产环境必须提供 HTTPS 并使用 `https://` API 地址。

---

### 13.3 API 返回 401

说明网络已经连通，但认证无效。

检查：

```text
Access Token
Refresh Token
BYPASS_AUTH
服务端认证配置
```

---

### 13.4 API 返回 404

说明地址可能重复或缺少：

```text
/api/mobile/v1
```

检查最终请求是否类似：

```text
http://100.x.x.x:3000/api/mobile/v1/ai-configs
```

不要形成：

```text
/api/mobile/v1/api/mobile/v1
```

---

## 14. 后续可扩展用途

闲置 Win11 还可以承担：

### P0

```text
bandu_web 常驻服务
SQLite 数据存储
AI 配置
AI API Key 管理
```

### P1

```text
数据库自动备份
接口自动测试
GitHub Actions Self-hosted Runner
定时更新代码
```

### P2

有合适 GPU 时：

```text
Ollama
本地视觉模型
本地 Embedding
本地 OCR
AI 推理服务
```

---

## 15. 实施顺序

```text
1. WSL 安装并登录 Tailscale

2. WSL 克隆 bandu_web

3. 初始化 Node、Prisma、独立随机密钥和管理员密码

4. 启动 bandu_web

5. 获取 WSL Tailscale IP

6. Mac curl 验证

7. 修改 bandu_app/.env

8. make sync 重新构建安装

9. 完成生产构建并配置 systemd 常驻服务

10. 配置 Windows 登录时启动 WSL

11. 增加 SQLite 在线备份和完整性校验

12. 发布前配置 HTTPS、Release API 地址和服务端安全响应头
```

---

## 16. 验收标准

本次 Mac 侧已通过：

```text
1. Mac Tailscale 可直连 100.69.41.14，延迟约 6 ms。

2. Mac 和 emulator-5556 均可连接 WSL 3000/TCP。

3. 未认证请求返回 401，登录、刷新轮换、旧令牌拒绝和注销均正常。

4. AI 配置的掩码、保留密钥、模型联动和删除闭环正常。

5. Flutter Debug 构建使用 WSL API 地址且 BYPASS_AUTH=false。
```

完整部署仍需满足：

```text
1. NEXTAUTH_SECRET 和 MOBILE_AUTH_SECRET 已更换为独立随机值。

2. 默认管理员密码已修改，Tailscale ACL 已按最小权限核对。

3. Win11 登录后 WSL 自动启动，bandu_web 由 systemd 运行生产构建。

4. Flutter 不再请求 10.0.2.2 访问远程 WSL。

5. AI API Key 只保存在 bandu_web，备份后可恢复。

6. Mac 关闭后，Win11 上的服务端仍可运行。

7. SQLite 在线备份定时执行且 `PRAGMA integrity_check` 返回 `ok`。

8. Release 使用 HTTPS，不依赖 Debug 明文流量配置。
```

---

## 17. 最终结构

```text
Win11 / WSL
├── bandu_web
├── Prisma / SQLite
├── AI 配置
├── Tailscale
└── systemd 常驻服务

Mac
├── bandu_app
├── Flutter
├── Android Emulator
└── Tailscale
```

最终 Flutter 配置：

```env
API_BASE_URL=http://100.69.41.14:3000/api/mobile/v1
BYPASS_AUTH=false
```

上面的 HTTP 地址只用于当前 Debug 联调。Release 应替换为 Tailnet 内可验证的 HTTPS 地址。

截图中的：

```text
10.0.2.2:36508
```

只适用于访问 Mac 本机端口，不应继续作为远程 WSL 服务端地址。
