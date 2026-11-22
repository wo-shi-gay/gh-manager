# GitHub 管理器（GhManager）

一个用 **Kotlin + Jetpack Compose** 写的 Android 原生 GitHub 图形化管理客户端。
粘贴你的 Personal Access Token 即可图形化管理仓库、代码、Issue、PR、Commit、Actions 云构建、通知、Gist、搜索。

## 功能

- **令牌登录**：粘贴 PAT → 自动调 `/user` 验证 → 显示该令牌的 scopes 与 API 限流余额（classic / fine-grained 都支持）
- **仓库**：全部 / 我拥有 / 已 Star / 组织 分类列表，新建仓库（私有、auto_init）
- **仓库详情**
  - 概览：Star / Watch / Fork 一键操作、统计、权限、默认分支、License
  - 代码：目录逐级浏览、文件内容查看（base64 解码）
  - Issues：开启/关闭/全部筛选、详情、评论、新建、开关
  - PR：列表、详情、可合并状态、合并、关闭
  - 提交：历史列表、单次提交的文件 diff（patch）
  - **Actions 云构建**：工作流列表、`workflow_dispatch` 触发（main/master/dev）、运行记录、运行详情、Job/Step 状态、取消、重跑、Job 日志
  - 设置：改描述、切换私有/公开、归档、删除仓库（二次确认）
- **通知**：未读/全部、全部标为已读
- **搜索**：仓库 / 代码 / Issue / 用户
- **Gist**：列表、跳转
- **我的**：账号信息、令牌权限、API 限流

## 构建

本仓库自带 GitHub Actions 工作流 `.github/workflows/build.yml`：
push 到 `main`/`master` 或手动 `workflow_dispatch` 即自动编译，产物在仓库 **Actions → 最近一次运行 → Artifacts** 里下载：

- `GhManager-debug` — debug 版（可直接安装）
- `GhManager-release` — release 版（用 debug keystore 签名，也能直接装）

## 本地构建

```bash
./gradlew :app:assembleDebug
```

需要 JDK 17、Android SDK Platform 35。

## 安全说明

- 令牌仅保存在本机 `SharedPreferences`，不写入源码、不上传任何服务器。
- 建议使用 fine-grained PAT，只授予你真正需要的仓库权限。
- 不要在公开场合分享令牌；一旦泄露请立刻在 GitHub 设置中撤销。
