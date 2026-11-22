package com.agentpad.ghclient

import org.json.JSONArray
import org.json.JSONObject

/**
 * MCP 工具集：把所有 GitHub 权限暴露成标准 MCP tool。
 * 每个工具都通过 Gh.callSync 直接走 GitHub REST API，用的是 App 里已登录的令牌，
 * 所以「工具能做的事 = 令牌能做的事」。
 */
object McpTools {

    // ---------------- 工具定义 ----------------

    private fun tool(name: String, desc: String, props: JSONObject, required: List<String>): JSONObject {
        val schema = JSONObject()
            .put("type", "object")
            .put("properties", props)
        if (required.isNotEmpty()) schema.put("required", JSONArray(required))
        return JSONObject()
            .put("name", name)
            .put("description", desc)
            .put("inputSchema", schema)
    }

    private fun s(desc: String): JSONObject = JSONObject().put("type", "string").put("description", desc)

    private fun i(desc: String): JSONObject = JSONObject().put("type", "integer").put("description", desc)

    private fun b(desc: String): JSONObject = JSONObject().put("type", "boolean").put("description", desc)

    private val tools: List<JSONObject> by lazy {
        listOf(
            // --- 账号 ---
            tool(
                "get_me", "获取当前登录的 GitHub 用户信息（身份、仓库数、关注数、套餐）",
                JSONObject(), emptyList()
            ),
            tool(
                "get_rate_limit", "查看当前令牌的 API 限流额度", JSONObject(), emptyList()
            ),

            // --- 仓库 ---
            tool(
                "list_repos",
                "列出仓库。affiliation: all(默认) / owner / organization_member；visibility: all/public/private",
                JSONObject()
                    .put("affiliation", s("all / owner / organization_member，默认 all"))
                    .put("visibility", s("all / public / private，默认 all"))
                    .put("sort", s("updated / pushed / full_name / created，默认 updated"))
                    .put("per_page", i("每页数量，默认 50，最大 100"))
                    .put("page", i("页码，默认 1")),
                emptyList()
            ),
            tool(
                "search_repos", "按关键字搜索 GitHub 仓库",
                JSONObject()
                    .put("q", s("搜索关键词，支持 GitHub 搜索语法如 language:kotlin stars:>100"))
                    .put("sort", s("stars / forks / updated，可选"))
                    .put("per_page", i("每页数量，默认 20")),
                listOf("q")
            ),
            tool(
                "get_repo", "获取单个仓库的详细信息",
                JSONObject().put("repo", s("owner/name 形式，如 octocat/Hello-World")),
                listOf("repo")
            ),
            tool(
                "create_repo", "创建一个新仓库",
                JSONObject()
                    .put("name", s("仓库名"))
                    .put("description", s("描述"))
                    .put("private", b("是否私有，默认 false"))
                    .put("auto_init", b("是否自动初始化 README，默认 true")),
                listOf("name")
            ),
            tool(
                "update_repo", "修改仓库属性（描述 / 私有 / 归档 / 主页）",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("description", s("新描述"))
                    .put("private", b("是否私有"))
                    .put("archived", b("是否归档"))
                    .put("homepage", s("主页 URL")),
                listOf("repo")
            ),
            tool(
                "delete_repo", "删除仓库（危险！不可撤销，需要 delete_repo 权限）",
                JSONObject().put("repo", s("owner/name")),
                listOf("repo")
            ),
            tool(
                "star_repo", "Star 或取消 Star 仓库",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("starred", b("true = Star，false = 取消，默认 true")),
                listOf("repo")
            ),
            tool(
                "fork_repo", "Fork 仓库到自己的账号",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("organization", s("可选，Fork 到指定组织")),
                listOf("repo")
            ),
            tool(
                "list_branches", "列出仓库的分支",
                JSONObject().put("repo", s("owner/name")),
                listOf("repo")
            ),
            tool(
                "list_issues", "列出仓库的 Issue（不含 PR）",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("state", s("open / closed / all，默认 open"))
                    .put("per_page", i("每页数量，默认 30")),
                listOf("repo")
            ),
            tool(
                "get_issue", "获取单个 Issue 详情",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("number", i("Issue 编号")),
                listOf("repo", "number")
            ),
            tool(
                "create_issue", "创建 Issue",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("title", s("标题"))
                    .put("body", s("正文，支持 Markdown"))
                    .put("labels", s("逗号分隔的标签名，可选"))
                    .put("assignees", s("逗号分隔的负责人用户名，可选")),
                listOf("repo", "title")
            ),
            tool(
                "comment_issue", "在 Issue 或 PR 下发表评论",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("number", i("Issue / PR 编号"))
                    .put("body", s("评论内容，支持 Markdown")),
                listOf("repo", "number", "body")
            ),
            tool(
                "set_issue_state", "打开或关闭 Issue / PR",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("number", i("Issue / PR 编号"))
                    .put("state", s("open 或 closed")),
                listOf("repo", "number", "state")
            ),
            tool(
                "list_pulls", "列出仓库的 Pull Request",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("state", s("open / closed / all，默认 open"))
                    .put("per_page", i("每页数量，默认 30")),
                listOf("repo")
            ),
            tool(
                "get_pull", "获取单个 PR 详情（含可合并状态、改动统计）",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("number", i("PR 编号")),
                listOf("repo", "number")
            ),
            tool(
                "merge_pull", "合并 Pull Request",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("number", i("PR 编号"))
                    .put("merge_method", s("merge / squash / rebase，默认 merge"))
                    .put("commit_title", s("合并提交标题，可选")),
                listOf("repo", "number")
            ),
            tool(
                "list_commits", "列出仓库的提交历史",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("sha", s("分支名或 commit sha，可选"))
                    .put("path", s("只看某个路径的提交，可选"))
                    .put("per_page", i("每页数量，默认 30")),
                listOf("repo")
            ),
            tool(
                "get_commit", "获取单次提交的详情（含文件 diff / patch）",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("sha", s("commit sha")),
                listOf("repo", "sha")
            ),

            // --- 文件 / 内容 ---
            tool(
                "list_contents", "列出仓库某个目录下的文件（不传 path 即根目录）",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("path", s("目录路径，默认空（根目录）"))
                    .put("ref", s("分支或 tag，可选")),
                listOf("repo")
            ),
            tool(
                "read_file", "读取仓库里的文本文件内容（自动 base64 解码）",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("path", s("文件路径"))
                    .put("ref", s("分支或 tag，可选")),
                listOf("repo", "path")
            ),
            tool(
                "write_file",
                "创建或更新仓库里的文件（自动提交）。更新已存在的文件时必须提供该文件的 sha",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("path", s("文件路径"))
                    .put("content", s("文件内容（纯文本）"))
                    .put("message", s("提交信息"))
                    .put("branch", s("分支，默认仓库默认分支"))
                    .put("sha", s("更新时必填：文件当前 blob sha")),
                listOf("repo", "path", "content")
            ),
            tool(
                "delete_file", "删除仓库里的文件（需要文件 sha）",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("path", s("文件路径"))
                    .put("sha", s("文件 blob sha"))
                    .put("message", s("提交信息"))
                    .put("branch", s("分支，可选")),
                listOf("repo", "path", "sha")
            ),

            // --- Actions 云构建 ---
            tool(
                "list_workflows", "列出仓库的 GitHub Actions 工作流",
                JSONObject().put("repo", s("owner/name")),
                listOf("repo")
            ),
            tool(
                "list_workflow_runs", "列出 Actions 运行记录",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("workflow_id", s("可选，只看某个工作流的运行"))
                    .put("branch", s("可选，只看某个分支"))
                    .put("status", s("可选：queued / in_progress / completed"))
                    .put("per_page", i("数量，默认 20")),
                listOf("repo")
            ),
            tool(
                "get_workflow_run", "获取单次 Actions 运行的详情",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("run_id", i("运行 ID")),
                listOf("repo", "run_id")
            ),
            tool(
                "get_run_jobs", "获取某次运行的 Job 与步骤状态",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("run_id", i("运行 ID")),
                listOf("repo", "run_id")
            ),
            tool(
                "get_job_log",
                "获取某个 Job 的完整日志文本（可能较大，建议只看失败 Job）",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("job_id", i("Job ID"))
                    .put("tail_lines", i("只返回最后 N 行，默认 200")),
                listOf("repo", "job_id")
            ),
            tool(
                "dispatch_workflow", "触发工作流（要求 workflow 文件声明了 on: workflow_dispatch）",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("workflow_id", s("工作流 ID 或文件名如 build.yml"))
                    .put("ref", s("分支名，默认 main"))
                    .put("inputs", s("可选，JSON 字符串形式的 inputs")),
                listOf("repo", "workflow_id")
            ),
            tool(
                "cancel_run", "取消一次正在运行的 Actions",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("run_id", i("运行 ID")),
                listOf("repo", "run_id")
            ),
            tool(
                "rerun_workflow", "重新运行一次 Actions",
                JSONObject()
                    .put("repo", s("owner/name"))
                    .put("run_id", i("运行 ID")),
                listOf("repo", "run_id")
            ),

            // --- 通知 / Gist / 用户 ---
            tool(
                "list_notifications", "列出通知",
                JSONObject()
                    .put("all", b("true = 含已读，false = 只看未读，默认 false"))
                    .put("per_page", i("数量，默认 30")),
                emptyList()
            ),
            tool(
                "mark_notifications_read", "把所有通知标记为已读",
                JSONObject().put("last_read_at", s("可选，ISO 时间")), emptyList()
            ),
            tool(
                "list_gists", "列出自己的 Gist",
                JSONObject().put("per_page", i("数量，默认 30")), emptyList()
            ),
            tool(
                "create_gist", "创建一个 Gist",
                JSONObject()
                    .put("description", s("描述"))
                    .put("filename", s("文件名，如 hello.md"))
                    .put("content", s("文件内容"))
                    .put("public", b("是否公开，默认 false")),
                listOf("filename", "content")
            ),
            tool(
                "search_code", "在 GitHub 上搜索代码",
                JSONObject()
                    .put("q", s("搜索关键词"))
                    .put("per_page", i("数量，默认 20")),
                listOf("q")
            ),
            tool(
                "search_issues", "搜索 Issue / PR",
                JSONObject()
                    .put("q", s("搜索关键词，支持 is:issue is:open repo:owner/name 等"))
                    .put("per_page", i("数量，默认 20")),
                listOf("q")
            ),
            tool(
                "search_users", "搜索 GitHub 用户",
                JSONObject()
                    .put("q", s("用户名关键词"))
                    .put("per_page", i("数量，默认 20")),
                listOf("q")
            ),
            tool(
                "raw_api",
                "直接调用任意 GitHub REST API 路径（用于覆盖上面没写到的接口）。path 形如 /repos/owner/name/stats",
                JSONObject()
                    .put("method", s("GET / POST / PATCH / PUT / DELETE，默认 GET"))
                    .put("path", s("以 / 开头的 API 路径"))
                    .put("body", s("可选，JSON 字符串形式的请求体")),
                listOf("path")
            )
        )
    }

    fun list(): JSONArray {
        val arr = JSONArray()
        tools.forEach { arr.put(it) }
        return arr
    }

    fun count(): Int = tools.size

    // ---------------- 调用分发 ----------------

    fun call(name: String, a: JSONObject): String {
        fun req(key: String): String {
            val v = a.optString(key, "")
            if (v.isBlank()) throw IllegalArgumentException("缺少参数 $key")
            return v
        }

        fun enc(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")

        return when (name) {
            "get_me" -> Gh.callSync("GET", "/user")

            "get_rate_limit" -> Gh.callSync("GET", "/rate_limit")

            "list_repos" -> {
                val aff = a.optString("affiliation", "all").ifBlank { "all" }
                val vis = a.optString("visibility", "all").ifBlank { "all" }
                val sort = a.optString("sort", "updated").ifBlank { "updated" }
                val per = a.optInt("per_page", 50).coerceIn(1, 100)
                val page = a.optInt("page", 1).coerceAtLeast(1)
                val affParam = if (aff == "all") "owner,collaborator,organization_member" else aff
                Gh.callSync(
                    "GET",
                    "/user/repos?per_page=$per&page=$page&sort=$sort&visibility=$vis&affiliation=$affParam"
                )
            }

            "search_repos" -> {
                val q = req("q")
                val sort = a.optString("sort", "")
                val per = a.optInt("per_page", 20).coerceIn(1, 100)
                var path = "/search/repositories?q=${enc(q)}&per_page=$per"
                if (sort.isNotBlank()) path += "&sort=$sort"
                Gh.callSync("GET", path)
            }

            "get_repo" -> Gh.callSync("GET", "/repos/" + req("repo"))

            "create_repo" -> {
                val body = JSONObject()
                body.put("name", req("name"))
                if (a.has("description")) body.put("description", a.optString("description"))
                body.put("private", a.optBoolean("private", false))
                body.put("auto_init", a.optBoolean("auto_init", true))
                Gh.callSync("POST", "/user/repos", body)
            }

            "update_repo" -> {
                val repo = req("repo")
                val body = JSONObject()
                if (a.has("description")) body.put("description", a.optString("description"))
                if (a.has("private")) body.put("private", a.optBoolean("private", false))
                if (a.has("archived")) body.put("archived", a.optBoolean("archived", false))
                if (a.has("homepage")) body.put("homepage", a.optString("homepage"))
                Gh.callSync("PATCH", "/repos/$repo", body)
            }

            "delete_repo" -> {
                Gh.callSync("DELETE", "/repos/" + req("repo"))
                "{\"ok\":true,\"deleted\":\"" + req("repo") + "\"}"
            }

            "star_repo" -> {
                val repo = req("repo")
                if (a.optBoolean("starred", true)) {
                    Gh.callSync("PUT", "/user/starred/$repo", JSONObject())
                    "{\"ok\":true,\"starred\":true}"
                } else {
                    Gh.callSync("DELETE", "/user/starred/$repo")
                    "{\"ok\":true,\"starred\":false}"
                }
            }

            "fork_repo" -> {
                val repo = req("repo")
                val body = JSONObject()
                val org = a.optString("organization", "")
                if (org.isNotBlank()) body.put("organization", org)
                Gh.callSync("POST", "/repos/$repo/forks", body)
            }

            "list_branches" -> Gh.callSync("GET", "/repos/" + req("repo") + "/branches?per_page=100")

            "list_issues" -> {
                val repo = req("repo")
                val st = a.optString("state", "open").ifBlank { "open" }
                val per = a.optInt("per_page", 30).coerceIn(1, 100)
                Gh.callSync("GET", "/repos/$repo/issues?state=$st&per_page=$per")
            }

            "get_issue" -> {
                val repo = req("repo")
                val n = a.optInt("number", 0)
                if (n <= 0) throw IllegalArgumentException("缺少参数 number")
                Gh.callSync("GET", "/repos/$repo/issues/$n")
            }

            "create_issue" -> {
                val repo = req("repo")
                val body = JSONObject()
                body.put("title", req("title"))
                if (a.has("body")) body.put("body", a.optString("body"))
                val labels = a.optString("labels", "")
                if (labels.isNotBlank()) {
                    val arr = JSONArray()
                    labels.split(",").forEach { if (it.isNotBlank()) arr.put(it.trim()) }
                    body.put("labels", arr)
                }
                val assignees = a.optString("assignees", "")
                if (assignees.isNotBlank()) {
                    val arr = JSONArray()
                    assignees.split(",").forEach { if (it.isNotBlank()) arr.put(it.trim()) }
                    body.put("assignees", arr)
                }
                Gh.callSync("POST", "/repos/$repo/issues", body)
            }

            "comment_issue" -> {
                val repo = req("repo")
                val n = a.optInt("number", 0)
                if (n <= 0) throw IllegalArgumentException("缺少参数 number")
                Gh.callSync(
                    "POST", "/repos/$repo/issues/$n/comments",
                    JSONObject().put("body", req("body"))
                )
            }

            "set_issue_state" -> {
                val repo = req("repo")
                val n = a.optInt("number", 0)
                if (n <= 0) throw IllegalArgumentException("缺少参数 number")
                val st = req("state")
                Gh.callSync("PATCH", "/repos/$repo/issues/$n", JSONObject().put("state", st))
            }

            "list_pulls" -> {
                val repo = req("repo")
                val st = a.optString("state", "open").ifBlank { "open" }
                val per = a.optInt("per_page", 30).coerceIn(1, 100)
                Gh.callSync("GET", "/repos/$repo/pulls?state=$st&per_page=$per")
            }

            "get_pull" -> {
                val repo = req("repo")
                val n = a.optInt("number", 0)
                if (n <= 0) throw IllegalArgumentException("缺少参数 number")
                Gh.callSync("GET", "/repos/$repo/pulls/$n")
            }

            "merge_pull" -> {
                val repo = req("repo")
                val n = a.optInt("number", 0)
                if (n <= 0) throw IllegalArgumentException("缺少参数 number")
                val body = JSONObject()
                body.put("merge_method", a.optString("merge_method", "merge").ifBlank { "merge" })
                val title = a.optString("commit_title", "")
                if (title.isNotBlank()) body.put("commit_title", title)
                Gh.callSync("PUT", "/repos/$repo/pulls/$n/merge", body)
            }

            "list_commits" -> {
                val repo = req("repo")
                val per = a.optInt("per_page", 30).coerceIn(1, 100)
                var path = "/repos/$repo/commits?per_page=$per"
                val sha = a.optString("sha", "")
                if (sha.isNotBlank()) path += "&sha=${enc(sha)}"
                val p = a.optString("path", "")
                if (p.isNotBlank()) path += "&path=${enc(p)}"
                Gh.callSync("GET", path)
            }

            "get_commit" -> {
                val repo = req("repo")
                val sha = req("sha")
                Gh.callSync("GET", "/repos/$repo/commits/$sha")
            }

            "list_contents" -> {
                val repo = req("repo")
                val p = a.optString("path", "")
                val ref = a.optString("ref", "")
                var path = "/repos/$repo/contents"
                if (p.isNotBlank()) path += "/" + p.trimStart('/')
                if (ref.isNotBlank()) path += "?ref=" + enc(ref)
                Gh.callSync("GET", path)
            }

            "read_file" -> {
                val repo = req("repo")
                val p = req("path").trimStart('/')
                val ref = a.optString("ref", "")
                var path = "/repos/$repo/contents/$p"
                if (ref.isNotBlank()) path += "?ref=" + enc(ref)
                val raw = Gh.callSync("GET", path)
                val o = JSONObject(raw)
                val sb = StringBuilder()
                sb.append("path: ").append(o.optString("path")).append("\n")
                sb.append("size: ").append(o.optInt("size")).append("\n")
                sb.append("sha: ").append(o.optString("sha")).append("\n")
                sb.append("---\n")
                sb.append(base64Decode(o.optString("content", "")))
                sb.toString()
            }

            "write_file" -> {
                val repo = req("repo")
                val p = req("path").trimStart('/')
                val body = JSONObject()
                body.put("message", a.optString("message", "update " + p).ifBlank { "update " + p })
                body.put("content", android.util.Base64.encodeToString(
                    req("content").toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP
                ))
                val branch = a.optString("branch", "")
                if (branch.isNotBlank()) body.put("branch", branch)
                val sha = a.optString("sha", "")
                if (sha.isNotBlank()) body.put("sha", sha)
                Gh.callSync("PUT", "/repos/$repo/contents/$p", body)
            }

            "delete_file" -> {
                val repo = req("repo")
                val p = req("path").trimStart('/')
                val body = JSONObject()
                body.put("message", a.optString("message", "delete " + p).ifBlank { "delete " + p })
                body.put("sha", req("sha"))
                val branch = a.optString("branch", "")
                if (branch.isNotBlank()) body.put("branch", branch)
                Gh.callSync("DELETE", "/repos/$repo/contents/$p", body)
            }

            "list_workflows" -> Gh.callSync("GET", "/repos/" + req("repo") + "/actions/workflows")

            "list_workflow_runs" -> {
                val repo = req("repo")
                val per = a.optInt("per_page", 20).coerceIn(1, 100)
                val wf = a.optString("workflow_id", "")
                var path = if (wf.isNotBlank()) "/repos/$repo/actions/workflows/$wf/runs?per_page=$per"
                else "/repos/$repo/actions/runs?per_page=$per"
                val br = a.optString("branch", "")
                if (br.isNotBlank()) path += "&branch=" + enc(br)
                val st = a.optString("status", "")
                if (st.isNotBlank()) path += "&status=" + enc(st)
                Gh.callSync("GET", path)
            }

            "get_workflow_run" -> {
                val repo = req("repo")
                val id = a.optLong("run_id", 0L)
                if (id <= 0L) throw IllegalArgumentException("缺少参数 run_id")
                Gh.callSync("GET", "/repos/$repo/actions/runs/$id")
            }

            "get_run_jobs" -> {
                val repo = req("repo")
                val id = a.optLong("run_id", 0L)
                if (id <= 0L) throw IllegalArgumentException("缺少参数 run_id")
                Gh.callSync("GET", "/repos/$repo/actions/runs/$id/jobs?per_page=100")
            }

            "get_job_log" -> {
                val repo = req("repo")
                val jid = a.optLong("job_id", 0L)
                if (jid <= 0L) throw IllegalArgumentException("缺少参数 job_id")
                // 日志接口会 302 到临时地址；这里用不带鉴权的原始 GET 跟随重定向
                val text = Gh.callSync("GET", "/repos/$repo/actions/jobs/$jid/logs")
                val tail = a.optInt("tail_lines", 200).coerceIn(1, 5000)
                val lines = text.lines()
                if (lines.size <= tail) text else lines.takeLast(tail).joinToString("\n")
            }

            "dispatch_workflow" -> {
                val repo = req("repo")
                val wf = req("workflow_id")
                val body = JSONObject()
                body.put("ref", a.optString("ref", "main").ifBlank { "main" })
                val inputs = a.optString("inputs", "")
                if (inputs.isNotBlank()) {
                    body.put("inputs", JSONObject(inputs))
                }
                Gh.callSync("POST", "/repos/$repo/actions/workflows/$wf/dispatches", body)
                "{\"ok\":true,\"dispatched\":true}"
            }

            "cancel_run" -> {
                val repo = req("repo")
                val id = a.optLong("run_id", 0L)
                if (id <= 0L) throw IllegalArgumentException("缺少参数 run_id")
                Gh.callSync("POST", "/repos/$repo/actions/runs/$id/cancel", JSONObject())
                "{\"ok\":true,\"cancelled\":true}"
            }

            "rerun_workflow" -> {
                val repo = req("repo")
                val id = a.optLong("run_id", 0L)
                if (id <= 0L) throw IllegalArgumentException("缺少参数 run_id")
                Gh.callSync("POST", "/repos/$repo/actions/runs/$id/rerun", JSONObject())
                "{\"ok\":true,\"rerun\":true}"
            }

            "list_notifications" -> {
                val all = a.optBoolean("all", false)
                val per = a.optInt("per_page", 30).coerceIn(1, 100)
                Gh.callSync("GET", "/notifications?all=$all&per_page=$per")
            }

            "mark_notifications_read" -> {
                val body = JSONObject().put("read", true)
                val t = a.optString("last_read_at", "")
                if (t.isNotBlank()) body.put("last_read_at", t)
                Gh.callSync("PUT", "/notifications", body)
                "{\"ok\":true}"
            }

            "list_gists" -> {
                val per = a.optInt("per_page", 30).coerceIn(1, 100)
                Gh.callSync("GET", "/gists?per_page=$per")
            }

            "create_gist" -> {
                val fn = req("filename")
                val files = JSONObject().put(fn, JSONObject().put("content", req("content")))
                val body = JSONObject().put("files", files).put("public", a.optBoolean("public", false))
                val d = a.optString("description", "")
                if (d.isNotBlank()) body.put("description", d)
                Gh.callSync("POST", "/gists", body)
            }

            "search_code" -> {
                val per = a.optInt("per_page", 20).coerceIn(1, 100)
                Gh.callSync("GET", "/search/code?q=${enc(req("q"))}&per_page=$per")
            }

            "search_issues" -> {
                val per = a.optInt("per_page", 20).coerceIn(1, 100)
                Gh.callSync("GET", "/search/issues?q=${enc(req("q"))}&per_page=$per")
            }

            "search_users" -> {
                val per = a.optInt("per_page", 20).coerceIn(1, 100)
                Gh.callSync("GET", "/search/users?q=${enc(req("q"))}&per_page=$per")
            }

            "raw_api" -> {
                val m = a.optString("method", "GET").ifBlank { "GET" }.uppercase()
                val p = req("path")
                val b = a.optString("body", "")
                val bodyObj = if (b.isBlank()) null else JSONObject(b)
                val raw = Gh.callSync(m, p, bodyObj)
                if (raw.isBlank()) "{\"ok\":true}" else raw
            }

            else -> throw IllegalArgumentException("未知工具：$name")
        }
    }
}
