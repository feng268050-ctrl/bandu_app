# bandu_app Win11 / WSL / Tailscale 部署与验收方案

> 目标：利用闲置 Win11 作为 `bandu_web` 常驻服务端，Mac 继续负责 Flutter 开发和 Android Emulator 调试。  
> 网络：Mac 已通过 Tailscale 连接 Win11 的 WSL。  
> 适用项目：
>
> - Flutter 客户端：`bandu_app`
> - Web / Server：`bandu_web`

---

## 0. 2026-07-14 验收结论

当前验收目标：

```text
WSL Tailscale IPv4：100.69.41.14
MagicDNS：desktop-wsl.tail5143ee.ts.net
Mobile API：http://100.69.41.14:3000/api/mobile/v1
HTTPS 目标：https://desktop-wsl.tail5143ee.ts.net/api/mobile/v1
```

实测结果：

```text
Tailscale 节点在线；Mac 到 WSL 可达，最新复验走 DERP(lax) 中继，约 330-750 ms，未建立直连
Mac 到 Web 根路径返回 307 /login，并带 CSP、COOP、Permissions-Policy、HSTS、nosniff、X-Frame-Options 等安全头
Mac 到 Mobile API：ai-configs 未认证返回 401，并带安全头；100.69.41.14 实测 TTFB 约 677 ms
MagicDNS HTTP :3000 可用，ai-configs 未认证返回 401；实测 TTFB 约 488 ms
MagicDNS HTTPS 443 当前不可连接，Tailscale Serve 尚未启用
Emulator 到 WSL：ICMP 0% 丢包，3000/TCP 可连接
Flutter 静态分析无问题，37 项测试通过，Debug APK 已安装到 emulator-5556
应用启动后显示真实登录页，BYPASS_AUTH=false 已生效
Debug 合并清单允许当前 HTTP；Release 合并清单未启用明文流量
22/TCP 和 3000/TCP 开放；80/TCP、443/TCP 关闭
未认证访问 users/me、ai-configs、tutor/models 均返回 401
错误密码返回统一 INVALID_CREDENTIALS，不泄露账号是否存在
Access Token、Refresh Token 登录正常；users/me 登录后返回 200
注销后当前 Access Token 立即返回 401，Refresh Token 立即返回 401
AI 配置新增、掩码读取、空密钥编辑保留、Tutor 模型联动和删除清理均通过
API 响应未返回明文 API Key 或 apiKeyCiphertext
Mobile 登录接口已添加失败限流，5 次失败后第 6 次返回 429，Retry-After=900
bandu_web 已从 tmux + next dev 切换为生产构建 + systemd 用户服务
```

当前判定：

```text
开发联调可用性：通过
Mobile API 功能闭环：通过
长期无人值守运行：WSL 本机通过，仍需确认 Windows 登录后自动拉起 WSL
安全验收：联调环境通过；真实 AI Key 建议在完成 ACL 后保存
```

已完成的 P0 安全项：

```text
1. WSL .env 中的 NEXTAUTH_SECRET 和 MOBILE_AUTH_SECRET 已更换为两个不同的随机值
2. admin@localhost / 123456 已失效，新密码仅保存在 WSL /tmp/bandu_admin_password.txt，权限 600
3. DATABASE_URL 已改为绝对 SQLite 路径，避免 standalone 工作目录读错数据库
4. Mobile 登录限流已启用
5. logout 后当前 Access Token 已立即失效
6. 服务端安全响应头已启用
7. MOBILE_AUTH_SECRET 已固定；保存真实 AI Key 后不要直接更换
```

仍需人工确认：

```text
1. 在 Tailscale 管理后台确认 ACL/Grant 只允许 Mac peer 访问 WSL 节点的 3000，以及启用 Serve 后的 443
2. Tailscale Serve 当前未启用；启用前 Release 不能使用 HTTPS 目标地址
3. 当前 443 未监听，发布构建不能使用现有 HTTP 地址
4. 全量 npm run lint 仍有项目既有无关问题；本次改动文件 targeted lint 通过
5. Mac 到 WSL 当前经 DERP 中继，联调可用但延迟高；需要排查双方 NAT/防火墙/UDP 直连能力
```

当前 HTTP 地址仅用于“受控 Tailnet + Flutter Debug”联调；Release 必须切换到 HTTPS。

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

`make dev` 启动的是 Next 开发服务器，只适合调试；长期常驻必须先 `npm run build`，再用 systemd 启动 `.next/standalone/server.js` 或等价的生产启动脚本。

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

### 8.2 创建 systemd 用户服务

先在服务目录完成生产构建：

```bash
cd ~/projects/bandu_web
npm ci
npx prisma generate
npx prisma migrate deploy
npm run build
```

当前落地方式为 systemd 用户服务：

```text
/home/gin/.config/systemd/user/bandu-web.service
```

当前服务应启动 Next standalone 产物：

```text
.next/standalone/server.js
```

确认信息：

```bash
whoami
realpath ~/projects/bandu_web
command -v make
command -v npm
command -v node
```

创建服务：

```bash
mkdir -p ~/.config/systemd/user
nano ~/.config/systemd/user/bandu-web.service
```

示例：

```ini
[Unit]
Description=Bandu Web Service
After=network-online.target

[Service]
Type=simple
WorkingDirectory=/home/gin/projects/bandu_web
Environment=NODE_ENV=production
Environment=PORT=3000
ExecStart=/usr/bin/node /home/gin/projects/bandu_web/.next/standalone/server.js
Restart=always
RestartSec=5

[Install]
WantedBy=default.target
```

替换：

```text
WorkingDirectory
ExecStart
```

为实际值。

不要在 systemd 中运行 `make dev`。开发服务器包含热更新和额外诊断开销，不适合作为长期无人值守服务。

启用服务：

```bash
systemctl --user daemon-reload
systemctl --user enable --now bandu-web
```

查看状态：

```bash
systemctl --user status bandu-web
```

查看日志：

```bash
journalctl --user -u bandu-web -f
```

如果需要用户未登录时也保持用户服务可拉起，启用 linger：

```bash
sudo loginctl enable-linger gin
```

本次 WSL 本机落地状态：

```text
service: /home/gin/.config/systemd/user/bandu-web.service
status: active (running)
listen: 0.0.0.0:3000
startup: .next/standalone/server.js
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

当前 Mobile 登录接口已增加失败限流：连续 5 次失败后第 6 次返回 `429`，并带 `Retry-After: 900`。logout 后当前 Refresh Token 和当前 Access Token 都应立即失效。

扩大到多人使用前，仍建议把限流、封禁、审计日志和异常登录告警纳入统一运维策略。

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

发布构建应先通过 Tailscale Serve 或反向代理提供 HTTPS，再将 `API_BASE_URL` 改为 `https://.../api/mobile/v1`。服务端当前已补充 CSP、COOP、Permissions-Policy、Referrer-Policy、HSTS、`X-Content-Type-Options: nosniff` 和 `X-Frame-Options: DENY` 等响应头。

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
systemctl --user restart bandu-web
```

查看日志：

```bash
journalctl --user -u bandu-web -f
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

9. 完成生产构建并配置 systemd 用户服务

10. 配置 Windows 登录时启动 WSL

11. 增加 SQLite 在线备份和完整性校验

12. 发布前配置 Tailscale Serve/HTTPS 和 Release API 地址
```

---

## 16. 验收标准

本次 Mac 侧已通过：

```text
1. Mac Tailscale 可访问 100.69.41.14；最新复验走 DERP 中继，约 330-750 ms，未建立直连。

2. Mac 可访问 WSL 3000/TCP，Web 根路径返回 307 /login 且带安全响应头。

3. emulator-5556 已安装使用 WSL API 地址的 Debug APK，并显示真实登录页。

4. 未认证请求返回 401，登录、用户信息读取、logout 后 access/refresh 失效均正常。

5. AI 配置的掩码、保留密钥、模型联动和删除闭环正常。

6. Flutter Debug 构建使用 WSL API 地址且 BYPASS_AUTH=false。

7. WSL 本机 production systemd 用户服务 active，监听 0.0.0.0:3000。

8. MagicDNS HTTP :3000 可访问；HTTPS 443 当前不可连接。
```

完整部署仍需满足：

```text
1. NEXTAUTH_SECRET 和 MOBILE_AUTH_SECRET 已更换为独立随机值。

2. 默认管理员密码已修改。

3. Tailscale ACL/Grant 已在管理后台按最小权限核对。

4. Win11 登录后 WSL 自动启动，bandu_web 由 systemd 运行生产构建。

5. Flutter 不再请求 10.0.2.2 访问远程 WSL。

6. AI API Key 只保存在 bandu_web，备份后可恢复。

7. Mac 关闭后，Win11 上的服务端仍可运行。

8. SQLite 在线备份定时执行且 `PRAGMA integrity_check` 返回 `ok`。

9. Release 使用 HTTPS，不依赖 Debug 明文流量配置。

10. Tailscale Serve 或反向代理启用后，`https://desktop-wsl.tail5143ee.ts.net/api/mobile/v1` 可访问。
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
