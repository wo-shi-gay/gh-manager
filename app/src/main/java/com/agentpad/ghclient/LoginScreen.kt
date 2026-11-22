package com.agentpad.ghclient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONObject

/** 登录页：粘贴 PAT → 调 /user 验证 → 显示权限 */
@Composable
fun LoginScreen(onDone: () -> Unit) {
    var token by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    var info by remember { mutableStateOf<JSONObject?>(null) }
    var scopes by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun verify() {
        err = ""
        info = null
        if (token.isBlank()) {
            err = "先把令牌粘贴进来喵"
            return
        }
        busy = true
        scope.launch {
            try {
                Session.token = token.trim()
                val t = Gh.apiGet("/user")
                val o = JSONObject(t)
                info = o
                scopes = Gh.lastScopes
                Session.login = o.str("login")
                Session.avatar = o.str("avatar_url")
            } catch (e: Exception) {
                Session.token = ""
                err = errText(e)
            } finally {
                busy = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(GhBg).verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        androidx.compose.foundation.layout.Spacer(Modifier.size(30.dp))
        Text("GitHub 管理器", color = GhText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            "图形化管理你的仓库 · Issues · PR · Actions 云构建",
            color = GhDim,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp)
        )

        androidx.compose.foundation.layout.Spacer(Modifier.size(26.dp))

        GhCard {
            Text("① 粘贴 Personal Access Token", color = GhGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                "classic: ghp_… / fine-grained: github_pat_…",
                color = GhDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
            androidx.compose.material3.OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                placeholder = { Text("ghp_xxxxxxxxxxxx", fontSize = 12.sp, color = GhDim.copy(alpha = .5f)) },
                singleLine = true,
                visualTransformation = if (info == null) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.5.sp, color = GhText, fontFamily = FontFamily.Monospace),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallBtn(if (busy) "验证中…" else "验证令牌", GhGreen, filled = true) { if (!busy) verify() }
                if (info != null) {
                    androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
                    SmallBtn("进入 →", GhBlue, filled = true) { onDone() }
                }
            }
        }

        if (err.isNotEmpty()) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
            Surface(shape = RoundedCornerShape(10.dp), color = GhRed.copy(alpha = .12f), modifier = Modifier.fillMaxWidth()) {
                Text(err, color = GhRed, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
            }
        }

        val u = info
        if (u != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
            GhCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(u.str("avatar_url"), 44.dp)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(u.str("login"), color = GhText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            (u.str("name").ifEmpty { "（未设置昵称）" }) + " · ${u.num("public_repos")} 公开仓库",
                            color = GhDim, fontSize = 11.sp
                        )
                        Text(
                            u.str("plan").let { "" } + "关注 ${u.num("followers")} · 关注者 ${u.num("following")}",
                            color = GhDim, fontSize = 11.sp
                        )
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
                RowDivider()
                androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                Text("令牌权限（scopes）", color = GhDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (scopes.isBlank()) "无（fine-grained 令牌不返回 scopes）" else scopes,
                    color = GhPurple, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "限流：${Gh.lastRemaining}/${Gh.lastLimit} 次剩余",
                    color = GhDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        androidx.compose.foundation.layout.Spacer(Modifier.size(20.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = GhSurface2,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("怎么拿令牌喵？", color = GhText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "GitHub → 右上头像 → Settings → Developer settings →\n" +
                        "Personal access tokens → Tokens (classic) → Generate new token",
                    color = GhDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "↗ 点我直接打开",
                    color = GhBlue, fontSize = 11.5.sp,
                    modifier = Modifier.padding(top = 8.dp).clickable {
                        openUrl(ctxHolder, "https://github.com/settings/tokens/new?scopes=repo,workflow,admin:org,user,gist,notifications,delete_repo,write:packages&description=GhManager")
                    }
                )
            }
        }
    }
}

/** 简易全局 Context 持有（仅用于登录页开链接） */
lateinit var ctxHolder: android.content.Context

/** 搜索页 */
@Composable
fun SearchScreen() {
    var q by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("repos") }
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun doSearch() {
        if (q.isBlank()) return
        busy = true
        err = ""
        scope.launch {
            try {
                val path = when (kind) {
                    "repos" -> "/search/repositories?q=${java.net.URLEncoder.encode(q, "UTF-8")}&per_page=40"
                    "code" -> "/search/code?q=${java.net.URLEncoder.encode(q, "UTF-8")}&per_page=40"
                    "issues" -> "/search/issues?q=${java.net.URLEncoder.encode(q, "UTF-8")}&per_page=40"
                    else -> "/search/users?q=${java.net.URLEncoder.encode(q, "UTF-8")}&per_page=40"
                }
                val o = JSONObject(Gh.apiGet(path))
                rows = o.arr("items").toObjList()
            } catch (e: Exception) {
                err = errText(e)
                rows = emptyList()
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize().background(GhBg)) {
        HeadBar("搜索 GitHub", "仓库 / 代码 / Issue / 用户", onBack = { Nav.pop() })
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.OutlinedTextField(
                    value = q,
                    onValueChange = { q = it },
                    placeholder = { Text("搜索关键词…", fontSize = 12.sp, color = GhDim.copy(alpha = .5f)) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = GhText),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                SmallBtn("搜索", GhGreen, filled = true) { doSearch() }
            }
            Row(Modifier.padding(top = 10.dp)) {
                Pill("仓库", kind == "repos") { kind = "repos" }
                Pill("代码", kind == "code") { kind = "code" }
                Pill("Issue", kind == "issues") { kind = "issues" }
                Pill("用户", kind == "users") { kind = "users" }
            }
        }
        when {
            busy -> LoadingBox("搜索中…")
            err.isNotEmpty() -> ErrBox(err) { doSearch() }
            rows.isEmpty() -> EmptyBox("输入关键词开始搜索喵")
            else -> androidx.compose.foundation.lazy.LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rows.size) { i ->
                    val o = rows[i]
                    when (kind) {
                        "repos" -> RepoRow(o) { Nav.push("repo:" + o.str("full_name")) }
                        "code" -> ClickCard(onClick = {
                            val r = o.obj("repository").str("full_name")
                            if (r.isNotEmpty()) Nav.push("repo:$r")
                        }) {
                            Text(o.obj("repository").str("full_name"), color = GhBlue, fontSize = 12.sp)
                            Text(o.str("name"), color = GhText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(o.str("path"), color = GhDim, fontSize = 10.5.sp)
                        }
                        "issues" -> IssueRow(o, false) {
                            val r = o.str("repository_url").substringAfter("/repos/")
                            if (r.isNotEmpty()) Nav.push("issue:$r|${o.num("number")}|${if (o.str("pull_request").isNotEmpty()) "pr" else "issue"}")
                        }
                        else -> ClickCard(onClick = {}) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(o.str("avatar_url"), 34.dp)
                                Column(Modifier.padding(start = 10.dp)) {
                                    Text(o.str("login"), color = GhText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("↗ github.com/" + o.str("login"), color = GhDim, fontSize = 10.5.sp,
                                        modifier = Modifier.clickable { openUrl(ctxHolder, o.str("html_url")) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 新建仓库 */
@Composable
fun CreateRepoScreen() {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var private by remember { mutableStateOf(false) }
    var initReadme by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun create() {
        if (name.isBlank()) { err = "仓库名不能为空喵"; return }
        busy = true
        err = ""
        scope.launch {
            try {
                val body = JSONObject()
                body.put("name", name.trim())
                if (desc.isNotBlank()) body.put("description", desc.trim())
                body.put("private", private)
                body.put("auto_init", initReadme)
                Gh.apiSend("POST", "/user/repos", body)
                Nav.pop()
            } catch (e: Exception) {
                err = errText(e)
            } finally { busy = false }
        }
    }

    Column(
        Modifier.fillMaxSize().background(GhBg).verticalScroll(rememberScrollState())
    ) {
        HeadBar("新建仓库", "POST /user/repos", onBack = { Nav.pop() })
        Column(Modifier.padding(14.dp)) {
            Field(name, { name = it }, "仓库名（必填）", placeholder = "my-project")
            Gap(10)
            Field(desc, { desc = it }, "描述", single = false, minLines = 2, placeholder = "一句话简介")
            Gap(10)
            GhCard {
                Row(
                    Modifier.fillMaxWidth().clickable { private = !private },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (private) "☑" else "☐",
                        color = if (private) GhGreen else GhDim, fontSize = 16.sp
                    )
                    Text("  私有仓库", color = GhText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                }
                Gap(6)
                Row(
                    Modifier.fillMaxWidth().clickable { initReadme = !initReadme },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (initReadme) "☑" else "☐",
                        color = if (initReadme) GhGreen else GhDim, fontSize = 16.sp
                    )
                    Text("  自动初始化 README", color = GhText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                }
            }
            if (err.isNotEmpty()) {
                Gap(10)
                Text(err, color = GhRed, fontSize = 12.sp)
            }
            Gap(14)
            SmallBtn(if (busy) "创建中…" else "创建仓库", GhGreen, filled = true) { if (!busy) create() }
        }
    }
}

/** Gist 列表 */
@Composable
fun GistsScreen() {
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var busy by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun load() {
        busy = true
        err = ""
        scope.launch {
            try {
                rows = org.json.JSONArray(Gh.apiGet("/gists?per_page=100")).toObjList()
            } catch (e: Exception) { err = errText(e) } finally { busy = false }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().background(GhBg)) {
        HeadBar("我的 Gist", "共 ${rows.size} 个", onBack = { Nav.pop() },
            right = { SmallBtn("刷新", GhBlue) { load() } })
        when {
            busy -> LoadingBox()
            err.isNotEmpty() -> ErrBox(err) { load() }
            rows.isEmpty() -> EmptyBox("还没有 Gist 喵")
            else -> androidx.compose.foundation.lazy.LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rows.size) { i ->
                    val o = rows[i]
                    ClickCard(onClick = { openUrl(ctxHolder, o.str("html_url")) }) {
                        val files = o.obj("files")
                        val first = files.keys().asSequence().firstOrNull() ?: "（无文件）"
                        Text(first, color = GhBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            o.str("description").ifEmpty { "（无描述）" },
                            color = GhDim, fontSize = 11.5.sp, maxLines = 2,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                        Text(
                            "${if (o.flag("public")) "公开" else "私密"} · ${files.length()} 个文件 · ${isoToRel(o.str("updated_at"))}",
                            color = GhDim, fontSize = 10.5.sp, modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/** 新建 Issue */
@Composable
fun NewIssueScreen(fullName: String, title: String) {
    var t by remember { mutableStateOf(title) }
    var body by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (t.isBlank()) { err = "标题不能为空喵"; return }
        busy = true; err = ""
        scope.launch {
            try {
                val b = JSONObject()
                b.put("title", t.trim())
                if (body.isNotBlank()) b.put("body", body)
                Gh.apiSend("POST", "/repos/$fullName/issues", b)
                Nav.pop()
            } catch (e: Exception) { err = errText(e) } finally { busy = false }
        }
    }

    Column(Modifier.fillMaxSize().background(GhBg).verticalScroll(rememberScrollState())) {
        HeadBar("新建 Issue", fullName, onBack = { Nav.pop() })
        Column(Modifier.padding(14.dp)) {
            Field(t, { t = it }, "标题", placeholder = "发生了什么？")
            Gap(10)
            Field(body, { body = it }, "正文（Markdown）", single = false, minLines = 6, placeholder = "详细描述…")
            if (err.isNotEmpty()) { Gap(8); Text(err, color = GhRed, fontSize = 12.sp) }
            Gap(12)
            SmallBtn(if (busy) "提交中…" else "创建 Issue", GhGreen, filled = true) { if (!busy) submit() }
        }
    }
}
