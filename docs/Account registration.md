这个项目**已经支持“注册账号 + 密码登录”**，不需要重新做一套登录系统。仓库里已经有：

- `/register` 注册页
- `/login` 登录页
- `/api/register` 注册接口
- `/api/auth/[...nextauth]` NextAuth 登录接口
- Prisma `User` 表，字段里有 `email`、`password`、`role`、`isActive` 等
- 登录方式使用 NextAuth 的 `CredentialsProvider`，也就是邮箱/密码登录

## 1. 本地启动

```
git clone git@github.com:feng268050-ctrl/bandu_app.git
cd bandu_app
npm install
cp .env.example .env
```

`.env` 至少保证有这些：

```
DATABASE_URL="file:./dev.db"
NEXTAUTH_SECRET="supersecret-dev-secret"
AUTH_TRUST_HOST=true
```

生产环境建议重新生成 `NEXTAUTH_SECRET`：

```
openssl rand -base64 32
```

README 里本地源码运行也是按 `npm install`、复制 `.env.example`、初始化数据库、启动服务这个流程来做的 。

## 2. 初始化数据库

```
npx prisma migrate dev
npx prisma db seed
```

项目 README 里也说明了这两个命令，并且 seed 后会有默认管理员账号：

```
邮箱：admin@localhost
密码：123456
```



## 3. 启动项目

```
npm run dev
```

然后访问：

```
http://localhost:3000
```

未登录时，`middleware.ts` 会把用户重定向到 `/login`；登录页和注册页是允许未登录访问的 。

## 4. 注册账号

打开：

```
http://localhost:3000/register
```

填写：

```
姓名
邮箱
密码
确认密码
学段
入学年份
```

注册页会调用：

```
apiClient.post("/api/register", {
  name,
  email,
  password,
  educationStage,
  enrollmentYear
})
```

后端 `/api/register` 会校验邮箱、密码长度，检查邮箱是否已存在，然后用 `bcryptjs.hash(password, 10)` 加密密码后保存到数据库 。

## 5. 使用注册账号密码登录

打开：

```
http://localhost:3000/login
```

输入刚注册的：

```
邮箱
密码
```

登录页会调用：

```
await signIn("credentials", {
  redirect: false,
  email,
  password,
})
```

登录成功后跳转到首页 `/` 。

后端登录逻辑在 `src/lib/auth.ts`，流程是：

```
根据 email 查找用户
↓
检查用户是否存在
↓
检查 isActive 是否为 true
↓
使用 bcrypt compare 校验密码
↓
成功后写入 JWT session
```



## 6. 如果注册入口不能用

项目有注册开关。`/api/register/status` 会读取配置里的 `allowRegistration`，如果为 `false`，注册页会显示“注册已禁用” 。

检查这个文件：

```
config/app-config.json
```

确认里面不要写：

```
{
  "allowRegistration": false
}
```

可以改成：

```
{
  "allowRegistration": true
}
```

或者先用默认管理员登录：

```
admin@localhost
123456
```

然后在后台管理用户。

## 7. 核心结论

你这个项目现在的登录方式就是：

```
注册页 /register 创建用户
↓
密码加密保存到 SQLite
↓
登录页 /login 使用 NextAuth Credentials 登录
↓
NextAuth 生成 session token
↓
middleware 保护未登录页面
```

所以实际使用只需要：

```
cp .env.example .env
npx prisma migrate dev
npx prisma db seed
npm run dev
```

然后访问：

```
/register 注册
/login 登录
```

