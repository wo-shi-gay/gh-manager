# GitHub 管理器（GhManager）v2.0

一个用 **Kotlin + Jetpack Compose** 写的 Android 原生 GitHub 客户端，**内置本地 MCP Server**。

粘贴 Personal Access Token 即可图形化管理仓库、代码、Issue、PR、Commit、Actions 云构建、通知、Gist，并把**全部 GitHub 权限**暴露成本地 MCP 工具，供任意 MCP 客户端调用。

---

## ✨ v2.0 新特性

### 🎨 全新 UI
- 深色 GitHub 风格配色体系（6 组强调色 + 渐变）
- Material Icons 图标化导航（底部 Tab + 胶囊式页面切换）
- 卡片化信息层级、渐变主按钮、状态色 Chip
- 统计块（StatTile）、空状态/错误状态/加载状态统一设计
- 头像环、仓库隐私标识、Actions 运行状态着色

### 📡 本地 MCP 服务
App 内可一键开启 HTTP 服务，把 GitHub 能力暴露成标准 MCP（Model Context Protocol）工具：

- **协议**：JSON-RPC 2.0 over HTTP，`POST /mcp`
- **端点**
  - `POST /mcp` — `initialize` / `tools/list` / `tools/call` / `ping`
  - `GET /mcp` — SSE 握手（兼容老式 SSE 客户端）
  - `GET /health` — 健康检查（含工具数、请求数、运行时长）
  - `GET /` — 浏览器可读的说明页
  - `OPTIONS *` — CORS 预检
- **能力**：43 个工具，覆盖令牌的全部权限
- **安全**：可选访问令牌校验（`x-mcp-token` / `Authorization: Bearer`）
- **常驻**：前台服务 + 通知栏常驻，不被系统回收
- **可视化**：界面实时显示局域网 IP、客户端 JSON 配置（一键复制）、请求计数、运行日志

### MCP 客户端接入

```json
{
  "mcpServers": {
    "gh-manager": {
      "url": "http://<手机局域网IP>:8765/mcp"
    }
  }
}
```

App 的「MCP 本地服务」页面会直接生成这段配置并支持一键复制。

---

## 📋 MCP 工具清单（43 个）

### 账号
| 工具 | 说明 |
|---|---|
| `get_me` | 当前用户信息 |
| `get_rate_limit` | API 限流额度 |

### 仓库
| 工具 | 说明 |
|---|---|
| `list_repos` | 仓库列表（affiliation / visibility / sort 可调） |
| `search_repos` | 搜索仓库（支持 GitHub 搜索语法） |
| `get_repo` | 仓库详情 |
| `create_repo` | 创建仓库 |
| `update_repo` | 修改描述 / 私有 / 归档 / 主页 |
| `delete_repo` | 删除仓库 |
| `star_repo` | Star / 取消 Star |
| `fork_repo` | Fork 仓库 |
| `list_branches` | 分支列表 |

### 代码与文件
| 工具 | 说明 |
|---|---|
| `list_contents` | 浏览目录 |
| `read_file` | 读取文件（自动 base64 解码） |
| `write_file` | 创建 / 更新文件并自动提交 |
| `delete_file` | 删除文件 |

### Issue / PR
| 工具 | 说明 |
|---|---|
| `list_issues` / `get_issue` | 列表 / 详情 |
| `create_issue` | 新建（支持 labels / assignees） |
| `comment_issue` | 评论 |
| `set_issue_state` | 打开 / 关闭 |
| `list_pulls` / `get_pull` | PR 列表 / 详情 |
| `merge_pull` | 合并（merge / squash / rebase） |

### 提交
| 工具 | 说明 |
|---|---|
| `list_commits` | 提交历史 |
| `get_commit` | 单次提交（含 diff / patch） |

### Actions 云构建
| 工具 | 说明 |
|---|---|
| `list_workflows` | 工作流列表 |
| `list_workflow_runs` | 运行记录 |
| `get_workflow_run` | 运行详情 |
| `get_run_jobs` | Job / Step 状态 |
| `get_job_log` | Job 日志（可 tail） |
| `dispatch_workflow` | 触发工作流 |
| `cancel_run` | 取消运行 |
| `rerun_workflow` | 重新运行 |

### 通知 / Gist / 搜索 / 兜底
| 工具 | 说明 |
|---|---|
| `list_notifications` / `mark_notifications_read` | 通知 |
| `list_gists` / `create_gist` | Gist |
| `search_code` / `search_issues` / `search_users` | 全站搜索 |
| `raw_api` | 直接调用任意 REST 路径（兜底，覆盖所有未列出的接口） |

---

## 📱 App 内功能

- **令牌登录**：粘贴 PAT → 调 `/user` 验证 → 展示 scopes 与限流余额
- **仓库**：全部 / 我拥有 / 已 Star / 组织 分类，本地筛选，新建仓库
- **仓库详情** 7 个 Tab
  - 概览：Star / Watch / Fork 操作、统计块、权限、License、克隆地址
  - 代码：目录逐级浏览、文件内容查看（base64 解码）
  - Issues：状态筛选、详情、评论、开关、新建
  - PR：列表、详情、可合并状态、合并、关闭
  - 提交：历史、单次提交文件 diff
  - Actions：工作流触发、运行记录、运行详情、Job/Step、取消/重跑、日志
  - 设置：改描述、私有/归档切换、删除仓库（二次确认）
- **通知**：未读/全部、全部标已读
- **发现**：MCP 大卡、搜索、Gist、API 状态、能力清单
- **我**：账号卡、统计、MCP 入口、令牌信息

---

## 🔨 构建

本仓库自带 GitHub Actions（`.github/workflows/build.yml`）：
push 到 `main` 即自动编译，**并自动创建 Release 附带 APK**。

产物位置：
- **Release**：`https://github.com/<user>/gh-manager/releases/latest`（直接下载，无需登录）
- **Artifacts**：Actions 运行页底部（需登录 GitHub）

本地构建：
```bash
./gradlew :app:assembleDebug
```
需要 JDK 17 + Android SDK Platform 35。

---

## 🔐 安全

- 令牌只保存在本机 `SharedPreferences`，不写入源码、不上传任何服务器
- MCP 服务默认仅本机/局域网可访问，建议开启访问令牌校验
- App 不会自动泄露令牌；`raw_api` 工具也只使用你已授权的令牌
- 建议使用 fine-grained PAT，只授予真正需要的仓库权限
