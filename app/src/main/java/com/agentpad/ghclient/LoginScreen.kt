package com.agentpad.ghclient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONObject

/** 登录页 */
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
                val o = JSONObject(Gh.apiGet("/user"))
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
        Modifier.fillMaxSize().background(GhBg).verticalScroll(rememberScrollState())
    ) {
        // 顶部渐变标题区
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(GhGreen.copy(alpha = 0.16f), GhBg)
                    )
                )
                .padding(start = 22.dp, end = 22.dp, top = 46.dp, bottom = 26.dp)
        ) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(17.dp))
                    .background(GhGreen.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Dns, null, tint = GhGreen, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "GitHub 管理器",
                color = GhText, fontSize = 27.sp, fontWeight = FontWeight.Bold
            )
            Text(
                "图形化管理 · 本地 MCP 服务",
                color = GhGreen, fontSize = 12.5.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                "粘贴 Personal Access Token，即可管理仓库、代码、Issue、PR、\nActions 云构建，并把全部权限暴露成本地 MCP 工具。",
                color = GhDim, fontSize = 11.5.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Column(Modifier.padding(horizontal = 18.dp)) {
            GhCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VpnKey, null, tint = GhGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("① 粘贴访问令牌", color = GhText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "classic: ghp_… ／ fine-grained: github_pat_…",
                    color = GhDim, fontSize = 10.5.sp, modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(10.dp))
                Field(
                    token, { token = it }, "Personal Access Token",
                    mono = true, placeholder = "ghp_xxxxxxxxxxxx"
                )
                Spacer(Modifier.height(12.dp))

                if (info == null) {
                    PrimaryBtn(if (busy) "验证中…" else "验证令牌", Icons.Rounded.CheckCircle, enabled = !busy) {
                        if (!busy) verify()
                    }
                } else {
                    Row(Modifier.fillMaxWidth()) {
                        SmallBtn("重新验证", GhBlue, icon = Icons.Rounded.CheckCircle) { verify() }
                        Spacer(Modifier.width(8.dp))
                        SmallBtn("进入 →", GhGreen, filled = true) { onDone() }
                    }
                }
            }

            if (err.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GhRed.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GhRed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("!", color = GhRed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(err, color = GhRed, fontSize = 11.5.sp)
                    }
                }
            }

            val u = info
            if (u != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = GhGreen.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GhGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(u.str("avatar_url"), 48.dp, ring = GhGreen.copy(alpha = 0.5f))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        u.str("login"), color = GhText, fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Rounded.CheckCircle, null, tint = GhGreen, modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    u.str("name").ifEmpty { "（未设置昵称）" },
                                    color = GhDim, fontSize = 11.sp
                                )
                                Text(
                                    "公开仓库 " + u.num("public_repos") + " · 关注 " +
                                        u.num("followers") + " · 关注者 " + u.num("following"),
                                    color = GhDim, fontSize = 10.5.sp, modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(GhBorder))
                        Spacer(Modifier.height(10.dp))
                        Text("令牌权限（scopes）", color = GhDim, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (scopes.isBlank()) "（fine-grained 令牌不返回 scopes，按实际仓库授权）" else scopes,
                            color = GhPurple, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            "限流额度：" + Gh.lastRemaining + " / " + Gh.lastLimit,
                            color = GhDim, fontSize = 10.5.sp, modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = GhSurface2,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Explore, null, tint = GhBlue, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("怎么拿令牌喵？", color = GhText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "GitHub → 头像 → Settings → Developer settings →\n" +
                            "Personal access tokens → Tokens (classic) → Generate new token",
                        color = GhDim, fontSize = 10.5.sp, modifier = Modifier.padding(top = 6.dp)
                    )
                    Row(
                        Modifier.padding(top = 10.dp).clickable {
                            openUrl(
                                ctxHolder,
                                "https://github.com/settings/tokens/new?scopes=repo,workflow,admin:org,user,gist,notifications,delete_repo&description=GhManager"
                            )
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.OpenInNew, null, tint = GhBlue, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("点我直接打开令牌创建页", color = GhBlue, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            GhCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, null, tint = GhGreen, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("安全说明", color = GhText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "令牌只保存在本机 SharedPreferences，不会上传任何服务器。" +
                        "MCP 本地服务调用 GitHub 时也复用该令牌，权限与你的令牌完全一致。",
                    color = GhDim, fontSize = 10.5.sp, modifier = Modifier.padding(top = 6.dp)
                )
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

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
                val enc = java.net.URLEncoder.encode(q, "UTF-8")
                val path = when (kind) {
                    "repos" -> "/search/repositories?q=$enc&per_page=40"
                    "code" -> "/search/code?q=$enc&per_page=40"
                    "issues" -> "/search/issues?q=$enc&per_page=40"
                    else -> "/search/users?q=$enc&per_page=40"
                }
                rows = JSONObject(Gh.apiGet(path)).arr("items").toObjList()
            } catch (e: Exception) {
                err = errText(e)
                rows = emptyList()
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize().background(GhBg)) {
        AppBar("搜索 GitHub", "仓库 / 代码 / Issue / 用户", onBack = { Nav.pop() })
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    SearchInput(q, { q = it }, "输入关键词…")
                }
                Spacer(Modifier.width(8.dp))
                SmallBtn("搜索", GhGreen, filled = true, icon = Icons.Rounded.Search) { doSearch() }
            }
            Row(Modifier.padding(top = 10.dp)) {
                Pill("仓库", kind == "repos", GhGreen) { kind = "repos" }
                Pill("代码", kind == "code", GhBlue) { kind = "code" }
                Pill("Issue", kind == "issues", GhPurple) { kind = "issues" }
                Pill("用户", kind == "users", GhYellow) { kind = "users" }
            }
        }
        when {
            busy -> LoadingBox("搜索中…")
            err.isNotEmpty() -> ErrBox(err) { doSearch() }
            rows.isEmpty() -> EmptyBox("输入关键词开始搜索喵", "🔍")
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(rows.size) { i ->
                    val o = rows[i]
                    when (kind) {
                        "repos" -> RepoRow(o) { Nav.push("repo:" + o.str("full_name")) }
                        "code" -> ClickCard(onClick = {
                            val r = o.obj("repository").str("full_name")
                            if (r.isNotEmpty()) Nav.push("repo:$r")
                        }) {
                            Text(o.obj("repository").str("full_name"), color = GhBlue, fontSize = 11.5.sp)
                            Text(
                                o.str("name"), color = GhText, fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp)
                            )
                            Text(o.str("path"), color = GhDim, fontSize = 10.5.sp)
                        }
                        "issues" -> IssueRow(o, o.str("pull_request").isNotEmpty()) {
                            val r = o.str("repository_url").substringAfter("/repos/")
                            if (r.isNotEmpty()) {
                                Nav.push(
                                    "issue:$r|${o.num("number")}|" +
                                        (if (o.str("pull_request").isNotEmpty()) "pr" else "issue")
                                )
                            }
                        }
                        else -> ClickCard(onClick = { openUrl(ctxHolder, o.str("html_url")) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(o.str("avatar_url"), 36.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        o.str("login"), color = GhText, fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "github.com/" + o.str("login"), color = GhDim, fontSize = 10.5.sp
                                    )
                                }
                                Icon(Icons.Rounded.OpenInNew, null, tint = GhDim, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(14.dp)) }
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
        if (name.isBlank()) {
            err = "仓库名不能为空喵"
            return
        }
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
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize().background(GhBg).verticalScroll(rememberScrollState())) {
        AppBar("新建仓库", "POST /user/repos", onBack = { Nav.pop() })
        Column(Modifier.padding(14.dp)) {
            Field(name, { name = it }, "仓库名（必填）", placeholder = "my-project")
            Spacer(Modifier.height(10.dp))
            Field(desc, { desc = it }, "描述", single = false, minLines = 2, placeholder = "一句话简介")
            Spacer(Modifier.height(12.dp))
            GhCard {
                Row(
                    Modifier.fillMaxWidth().clickable { private = !private },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (private) "☑" else "☐",
                        color = if (private) GhYellow else GhDim, fontSize = 17.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("私有仓库", color = GhText, fontSize = 13.sp)
                        Text(
                            if (private) "只有你能看到" else "任何人都能看到",
                            color = GhDim, fontSize = 10.sp
                        )
                    }
                    Icon(
                        if (private) Icons.Rounded.Lock else Icons.Rounded.Explore,
                        null, tint = if (private) GhYellow else GhDim, modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(GhBorder))
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { initReadme = !initReadme },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (initReadme) "☑" else "☐",
                        color = if (initReadme) GhGreen else GhDim, fontSize = 17.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("自动初始化 README", color = GhText, fontSize = 13.sp)
                        Text("创建后直接有默认分支和 README.md", color = GhDim, fontSize = 10.sp)
                    }
                    Icon(Icons.Rounded.Description, null, tint = GhGreen, modifier = Modifier.size(15.dp))
                }
            }
            if (err.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(err, color = GhRed, fontSize = 11.5.sp)
            }
            Spacer(Modifier.height(16.dp))
            PrimaryBtn(if (busy) "创建中…" else "创建仓库", Icons.Rounded.Add, enabled = !busy) {
                if (!busy) create()
            }
            Spacer(Modifier.height(24.dp))
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
            } catch (e: Exception) {
                err = errText(e)
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().background(GhBg)) {
        AppBar(
            "我的 Gist", "共 ${rows.size} 个",
            onBack = { Nav.pop() },
            actions = { IconBtn(Icons.Rounded.CheckCircle, GhBlue) { load() } }
        )
        when {
            busy -> LoadingBox()
            err.isNotEmpty() -> ErrBox(err) { load() }
            rows.isEmpty() -> EmptyBox("还没有 Gist 喵", "📝")
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(rows.size) { i ->
                    val o = rows[i]
                    ClickCard(onClick = { openUrl(ctxHolder, o.str("html_url")) }) {
                        val files = o.obj("files")
                        val first = files.keys().asSequence().firstOrNull() ?: "（无文件）"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                                    .background(GhPurple.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Description, null, tint = GhPurple, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    first, color = GhText, fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    o.str("description").ifEmpty { "（无描述）" },
                                    color = GhDim, fontSize = 11.sp, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    (if (o.flag("public")) "公开" else "私密") + " · " +
                                        files.length() + " 个文件 · " + isoToRel(o.str("updated_at")),
                                    color = GhDim, fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(14.dp)) }
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
        if (t.isBlank()) {
            err = "标题不能为空喵"
            return
        }
        busy = true
        err = ""
        scope.launch {
            try {
                val b = JSONObject()
                b.put("title", t.trim())
                if (body.isNotBlank()) b.put("body", body)
                Gh.apiSend("POST", "/repos/$fullName/issues", b)
                Nav.pop()
            } catch (e: Exception) {
                err = errText(e)
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize().background(GhBg).verticalScroll(rememberScrollState())) {
        AppBar("新建 Issue", fullName, onBack = { Nav.pop() })
        Column(Modifier.padding(14.dp)) {
            Field(t, { t = it }, "标题", placeholder = "发生了什么？")
            Spacer(Modifier.height(10.dp))
            Field(body, { body = it }, "正文（Markdown）", single = false, minLines = 6, placeholder = "详细描述…")
            if (err.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(err, color = GhRed, fontSize = 11.5.sp)
            }
            Spacer(Modifier.height(14.dp))
            PrimaryBtn(if (busy) "提交中…" else "创建 Issue", Icons.Rounded.Add, enabled = !busy) {
                if (!busy) submit()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
