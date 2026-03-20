# next-bbs 项目说明

## 项目是做什么的

**next-bbs** 是一个基于 Next.js 的 **论坛 / BBS  Web 应用**，主要功能包括：

- **论坛首页**：帖子流 + 标签侧栏，根路径 `/` 展示帖子列表与标签导航。
- **帖子**：发帖、查看帖子详情、编辑帖子；路由为 `/posts`, `/posts/[postId]`, `/posts/new`, `/posts/[postId]/edit`。
- **评论**：在帖子下评论，评论线程展示。
- **用户与认证**：注册、登录、个人资料、登出；使用 better-auth，会话由服务端管理。
- **管理后台**：管理员可管理用户、帖子、标签；路由在 `/admin` 下（如 `/admin/users`, `/admin/posts`, `/admin/tags`）。
- **搜索**：按关键词搜索帖子，表单提交到 `/search`。

与 **maker-world-clone** 不同：next-bbs 是论坛产品（帖子/评论/标签/用户/管理），无侧边栏主导航；maker-world-clone 是 Makerworld 风格克隆（侧边栏、模型/竞赛/社区等视图）。二者业务定位不同，可借鉴的是**工程结构、目录约定与部分技术选型**，而非页面布局或功能照搬。

---

## 技术栈

- **框架**：Next.js 16，React 19；开发/构建通过 **Vinext**（Vite + Next 集成）运行（`vinext dev` / `vinext build` / `vinext start`）。
- **语言**：TypeScript，严格模式；路径别名 `@/*` 指向项目根。
- **样式**：Tailwind CSS v4；**shadcn** + **Base UI**、**class-variance-authority**、**tailwind-merge**、**clsx** 做组件样式与变体。
- **数据与认证**：**Drizzle ORM** + **Neon Serverless**（Postgres）；**better-auth** 做登录/注册/会话；密码哈希 **bcryptjs**；校验 **Zod**。
- **部署**：Cloudflare（`wrangler.jsonc`，`worker/`），Vite 插件 `@cloudflare/vite-plugin`。
- **测试**：Vitest，Testing Library；E2E 与单元测试在 `tests/` 下。

---

## 目录与模块结构

- **`app/`**：App Router 路由与布局。
  - **`(forum)/`**：论坛路由组；`layout.tsx` 提供全局论坛布局（顶部 header + 主内容 + footer，**无侧边栏**）；`page.tsx` 为首页；`search/`, `profile/`, `posts/`, `posts/[postId]/`, `posts/new/`, `posts/[postId]/edit/`；`_components/` 下为布局内组件（如 `SearchInput`, `SignOutButton`）。
  - **`(auth)/`**：登录/注册（`login/`, `register/`）。
  - **`admin/`**：管理后台布局与页面（`users`, `posts`, `tags`）。
  - **`api/auth/[...all]/`**：better-auth 的 API 路由。
- **`domains/`**：按领域组织的业务代码，每个领域下为 **`models/`、`service/`、`actions/`、`components/`、`hooks/`**（无下划线前缀）。
  - **`user`**：用户模型、认证服务、个人资料/登录/注册表单与操作。
  - **`post`**：帖子模型、服务、发帖/编辑操作、帖子流/卡片/详情组件。
  - **`comment`**：评论模型、服务、评论表单与线程组件。
  - **`tag`**：标签模型、服务、标签选择器与徽章。
  - **`admin`**：管理端服务、操作与表格等组件。
- **`lib/`**：共享工具与配置（如 `auth`、`utils`、`button-variants`）。
- **`components/`**：shadcn 等通用 UI 组件（`components.json` 中配置 `@/components`、`@/lib`）。
- **`openspec/`**：OpenSpec 规范与变更（specs、changes/archive）。
- **根配置**：`vite.config.ts`、`tsconfig.json`、`wrangler.jsonc`、`pnpm-workspace.yaml` 等。

---

## 关键约定（对重构的参考）

1. **领域目录**：`domains/<domain>/` 下固定子目录 `models/`、`service/`、`actions/`、`components/`、`hooks/`；文件名按概念命名（如 `post.ts`），不用角色后缀（如 `.service.ts`）。
2. **服务层**：`service/*.ts` 导出命名函数，不用命名空间对象；复合类型与业务类型与使用它们的函数放在同一文件或 `models/`（Drizzle + Zod 在 models）。
3. **路由与布局**：路由组 `(forum)`、`(auth)`、`admin` 分离；论坛为顶栏 + 主内容 + 底栏，无侧边栏。
4. **路径别名**：`@/*` 指向仓库根，便于 `@/lib`、`@/domains/...` 等引用。
5. **认证**：服务端通过 `auth.api.getSession({ headers })` 取会话；布局与服务中按需使用。
6. **OpenSpec**：用 OpenSpec 管理需求与变更（specs、design、tasks），与当前 maker-world-clone 的 openspec 用法一致。

---

## 与 maker-world-clone 的差异与可借鉴点

| 维度           | next-bbs                    | maker-world-clone（当前）     |
|----------------|-----------------------------|-------------------------------|
| 产品形态       | 论坛（帖子/评论/标签/用户） | Makerworld 风格（侧边栏/多视图） |
| 布局           | 顶栏 + 主内容 + 底栏         | 固定侧边栏 + 主内容            |
| 路由结构       | App Router + 路由组         | App Router，`[[...slug]]` 等   |
| 领域组织       | `domains/<domain>/` 五件套   | `src/components`, `src/views` 等 |
| 状态/数据      | Drizzle + better-auth       | 本地状态 + mock 数据           |
| 可借鉴         | 领域目录约定、路径别名、OpenSpec 流程 | 已在用 OpenSpec；可考虑逐步引入 domains 式分组或 `@/` 别名 |

重构时建议：**先保留当前产品形态与布局**，再按需在 maker-world-clone 中引入「领域式目录」或「路径别名」等约定，避免一次性大改；具体范围在「定义重构范围」阶段与产品方确认。
