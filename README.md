# Maker World Clone (Next.js)

该项目已从 `Vite + React` 重构为 `Next.js App Router`。

## 开发命令

```bash
npm install
npm run dev
```

默认访问地址：`http://localhost:3000`

## 生产构建

```bash
npm run build
npm run start
```

## 路由说明

项目使用统一的 catch-all 路由来承接原有页面状态：

- `/zh`
- `/en`
- `/zh/laser-cut-models`
- `/zh/contests`
- `/zh/community`

对应英文路径同理（`/en/...`）。

## 用户搜索 API

- **接口**: `GET /api/users/search?name=...&email=...`（需认证，见 `docs/api-users-search.md`）
- **管理页**: `/admin/user-search`（输入 API token 后按名称/邮箱搜索用户）
