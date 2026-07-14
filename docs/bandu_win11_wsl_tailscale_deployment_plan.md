# bandu_app 开发环境分工方案

> 目标：利用闲置 Win11 作为 `bandu_web` 常驻服务端，Mac 继续负责 Flutter 开发和 Android Emulator 调试。  
> 网络：Mac 已通过 Tailscale 连接 Win11 的 WSL。  
> 适用项目：
>
> - Flutter 客户端：`bandu_app`
> - Web / Server：`bandu_web`

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
npm install
npx prisma generate
```

需要初始化数据时：

```bash
npx prisma db seed
```

或者按照项目 Makefile：

```bash
make dev
```

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

示例：

```text
100.88.12.34
```

后续 Flutter 使用：

```text
http://100.88.12.34:3000/api/mobile/v1
```

也可以使用 Tailscale MagicDNS 名称，例如：

```text
http://bandu-wsl:3000/api/mobile/v1
```

初期建议优先使用 `100.x.x.x` 地址，先排除 DNS 问题。

---

## 5. 从 Mac 验证 WSL 服务

Mac 终端执行：

```bash
curl -v http://100.88.12.34:3000
```

再测试 Mobile API：

```bash
curl -v http://100.88.12.34:3000/api/mobile/v1/ai-configs
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
API_BASE_URL=http://100.88.12.34:3000/api/mobile/v1
```

如果当前开发阶段需要绕过认证：

```env
BYPASS_AUTH=true
```

将 `100.88.12.34` 替换为 WSL 实际 Tailscale IP。

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
  feng@100.88.12.34
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
ExecStart=/usr/bin/make dev
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

不建议将 `3000` 端口直接暴露到公网。

### 11.2 API Key 只放服务端

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

### 11.3 数据库备份

如果使用 SQLite，建议定期备份：

```bash
mkdir -p ~/backups/bandu

cp prisma/dev.db \
  ~/backups/bandu/dev-$(date +%F-%H%M%S).db
```

可以后续增加 cron 或 systemd timer。

---

## 12. 推荐开发流程

### Win11 / WSL

```bash
cd ~/projects/bandu_web
git pull
npm install
npx prisma generate
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

同时确认 Android 允许 HTTP 明文请求。

开发阶段可在：

```text
android/app/src/main/AndroidManifest.xml
```

配置：

```xml
<application
    android:usesCleartextTraffic="true">
```

生产环境建议改成 HTTPS。

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

3. 初始化 Node、Prisma 和环境变量

4. 启动 bandu_web

5. 获取 WSL Tailscale IP

6. Mac curl 验证

7. 修改 bandu_app/.env

8. make sync 重新构建安装

9. 配置 systemd 常驻服务

10. 配置 Windows 登录时启动 WSL

11. 增加 SQLite 备份
```

---

## 16. 验收标准

完成后应满足：

```text
1. Win11 开机并登录后，WSL 自动启动。

2. bandu_web 自动运行。

3. Mac 可以通过 Tailscale IP 访问 bandu_web。

4. Android Emulator 可以加载 AI 配置。

5. Flutter 不再请求 10.0.2.2 访问远程 WSL。

6. AI API Key 只保存在 bandu_web。

7. Mac 关闭后，Win11 上的服务端仍可运行。

8. SQLite 数据具备备份方案。
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
API_BASE_URL=http://<WSL_TAILSCALE_IP>:3000/api/mobile/v1
```

截图中的：

```text
10.0.2.2:36508
```

只适用于访问 Mac 本机端口，不应继续作为远程 WSL 服务端地址。
