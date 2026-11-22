package com.agentpad.ghclient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Terminal
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    val ctx = LocalContext.current
    ctxHolder = ctx
    var tab by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().background(GhBg)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> ReposTab()
                1 -> NotificationsTab()
                2 -> ExploreTab()
                else -> ProfileTab(onLogout)
            }
        }
        BottomBar(tab) { tab = it }
    }
}

@Composable
private fun BottomBar(tab: Int, onPick: (Int) -> Unit) {
    Surface(color = GhSurface) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, GhBorder, Color.Transparent)))
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                val items = listOf(
                    Pair("仓库", Icons.Rounded.Folder),
                    Pair("通知", Icons.Rounded.Notifications),
                    Pair("发现", Icons.Rounded.Explore),
                    Pair("我", Icons.Rounded.Person)
                )
                items.forEachIndexed { i, pair ->
                    val it = pair
                    val active = tab == i
                    Column(
                        Modifier.weight(1f).clickable { onPick(i) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) GhGreen.copy(alpha = 0.15f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                it.second, null,
                                tint = if (active) GhGreen else GhDim,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Text(
                            it.first,
                            color = if (active) GhGreen else GhDim,
                            fontSize = 9.5.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/** 仓库列表页 */
@Composable
fun ReposTab() {
    var filter by remember { mutableStateOf("all") }
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var me by remember { mutableStateOf<JSONObject?>(null) }
    var busy by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf("") }
    var q by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun load() {
        busy = true
        err = ""
        scope.launch {
            try {
                val path = when (filter) {
                    "mine" -> "/user/repos?per_page=100&sort=updated&affiliation=owner"
                    "starred" -> "/user/starred?per_page=100"
                    "orgs" -> "/user/repos?per_page=100&sort=updated&affiliation=organization_member"
                    else -> "/user/repos?per_page=100&sort=updated&affiliation=owner,collaborator,organization_member"
                }
                rows = org.json.JSONArray(Gh.apiGet(path)).toObjList()
                if (me == null) {
                    me = try { JSONObject(Gh.apiGet("/user")) } catch (_: Exception) { null }
                }
            } catch (e: Exception) {
                err = errText(e)
                rows = emptyList()
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(filter) { load() }

    val shown = if (q.isBlank()) rows else rows.filter { it.str("full_name").contains(q, true) }

    Column(Modifier.fillMaxSize()) {
        // ---- 顶部个人信息条 ----
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(GhSurface, GhBg)))
                .padding(start = 18.dp, end = 14.dp, top = 16.dp, bottom = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(me?.str("avatar_url") ?: Session.avatar, 42.dp, ring = GhGreen.copy(alpha = 0.5f))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "你好，" + (Session.login.ifEmpty { "喵" }),
                        color = GhText, fontSize = 17.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        (me?.num("public_repos")?.toString() ?: "0") + " 个公开仓库 · " +
                            (rows.size.toString()) + " 个可访问",
                        color = GhDim, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp)
                    )
                }
                IconBtn(Icons.Rounded.Dns, GhCyan, 34.dp) { Nav.push("mcp:0") }
                IconBtn(Icons.Rounded.Add, GhGreen, 34.dp) { Nav.push("create:new") }
            }
            Spacer(Modifier.height(14.dp))
            SearchInput(q, { q = it }, "在仓库里筛选…")
        }

        // ---- 筛选 ----
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 8.dp, top = 4.dp, bottom = 6.dp)
        ) {
            Pill("全部", filter == "all", GhGreen, Icons.Rounded.Folder) { filter = "all" }
            Pill("我拥有", filter == "mine", GhBlue, Icons.Rounded.Person) { filter = "mine" }
            Pill("已 Star", filter == "starred", GhYellow, Icons.Rounded.Star) { filter = "starred" }
            Pill("组织", filter == "orgs", GhPurple, Icons.Rounded.Explore) { filter = "orgs" }
        }

        when {
            busy -> LoadingBox()
            err.isNotEmpty() -> ErrBox(err) { load() }
            shown.isEmpty() -> EmptyBox(if (q.isBlank()) "还没有仓库喵，点右上角 + 新建一个吧" else "没找到匹配的仓库")
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(shown.size) { i ->
                    val o = shown[i]
                    RepoRow(o) { Nav.push("repo:" + o.str("full_name")) }
                }
                item {
                    Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                        Text("共 ${shown.size} 个仓库", color = GhDim, fontSize = 10.5.sp)
                    }
                }
            }
        }
    }
}

/** 通知页 */
@Composable
fun NotificationsTab() {
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var busy by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf("") }
    var onlyUnread by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun load() {
        busy = true
        err = ""
        scope.launch {
            try {
                rows = org.json.JSONArray(Gh.apiGet("/notifications?all=${!onlyUnread}&per_page=100")).toObjList()
            } catch (e: Exception) {
                err = errText(e)
                rows = emptyList()
            } finally {
                busy = false
            }
        }
    }

    fun markAllRead() {
        scope.launch {
            try {
                Gh.apiSend("PUT", "/notifications", JSONObject().put("read", true))
                load()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(onlyUnread) { load() }

    Column(Modifier.fillMaxSize()) {
        AppBar(
            "通知", "共 ${rows.size} 条",
            actions = {
                IconBtn(Icons.Rounded.Refresh, GhBlue) { load() }
                IconBtn(Icons.Rounded.CheckCircle, GhGreen) { markAllRead() }
            }
        )
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Pill("未读", onlyUnread, GhGreen) { onlyUnread = true }
            Pill("全部", !onlyUnread, GhBlue) { onlyUnread = false }
        }
        when {
            busy -> LoadingBox()
            err.isNotEmpty() -> ErrBox(err) { load() }
            rows.isEmpty() -> EmptyBox("没有通知喵，很清静～", "🌙")
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(rows.size) { i ->
                    val o = rows[i]
                    NotifRow(o) {
                        val full = o.obj("repository").str("full_name")
                        val num = o.obj("subject").str("url").substringAfterLast("/").toIntOrNull()
                        if (full.isNotEmpty()) {
                            if (num != null && num > 0) Nav.push("issue:$full|$num|issue")
                            else Nav.push("repo:$full")
                        }
                    }
                }
                item { Spacer(Modifier.height(14.dp)) }
            }
        }
    }
}

/** 发现页 */
@Composable
fun ExploreTab() {
    Column(Modifier.fillMaxSize().background(GhBg).verticalScroll(rememberScrollState())) {
        AppBar("发现", "搜索 · MCP · Gist")
        Column(Modifier.padding(14.dp)) {
            // MCP 大卡
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = GhCyan.copy(alpha = 0.10f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GhCyan.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .clickable { Nav.push("mcp:0") }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(GhCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.Dns, null, tint = GhCyan, modifier = Modifier.size(20.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("MCP 本地服务", color = GhText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (McpServer.isRunning) "运行中 · 端口 " + McpServer.port
                                else "未启动 · 点这里开启",
                                color = if (McpServer.isRunning) GhGreen else GhDim, fontSize = 11.sp
                            )
                        }
                        Box(
                            Modifier.size(9.dp).clip(CircleShape)
                                .background(if (McpServer.isRunning) GhGreen else GhDim)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "把 GitHub 全部权限暴露成本地 MCP 工具（" + McpTools.count() + " 个），其他 AI 应用可直接调用",
                        color = GhDim, fontSize = 11.sp
                    )
                }
            }

            SectionTitle("快捷入口", Icons.Rounded.Bolt)
            ActionRow("搜索 GitHub", "仓库 / 代码 / Issue / 用户", Icons.Rounded.Search, GhBlue) {
                Nav.push("search:0")
            }
            Spacer(Modifier.height(9.dp))
            ActionRow("我的 Gist", "查看和管理代码片段", Icons.Rounded.Description, GhPurple) {
                Nav.push("gists:0")
            }
            Spacer(Modifier.height(9.dp))
            ActionRow("新建仓库", "创建一个新的 GitHub 仓库", Icons.Rounded.Add, GhGreen) {
                Nav.push("create:new")
            }

            SectionTitle("API 状态", Icons.Rounded.CheckCircle)
            GhCard {
                KV("剩余额度", Gh.lastRemaining + " / " + Gh.lastLimit)
                KV("令牌 scopes", Gh.lastScopes.ifEmpty { "（fine-grained）" })
                KV("当前用户", Session.login.ifEmpty { "（未知）" })
                KV("MCP 状态", if (McpServer.isRunning) "运行中 :" + McpServer.port else "未启动")
            }

            SectionTitle("能力清单", Icons.Rounded.Terminal)
            GhCard {
                listOf(
                    "仓库：列表 / 新建 / 删除 / Star / Fork / 编辑",
                    "代码：目录浏览 / 文件查看 / 在线提交",
                    "Issues：列表 / 详情 / 评论 / 开关 / 新建",
                    "PR：列表 / 详情 / 合并 / 关闭 / 评论",
                    "Commits：历史 / 文件 diff",
                    "Actions：工作流 / 触发 / 取消 / 重跑 / Job 日志",
                    "通知 / Gist / 全站搜索",
                    "MCP：以上全部能力对外暴露为标准工具"
                ).forEach {
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.5.dp)) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(GhGreen))
                        Spacer(Modifier.width(7.dp))
                        Text(it, color = GhDim, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 我的页 */
@Composable
fun ProfileTab(onLogout: () -> Unit) {
    var me by remember { mutableStateOf<JSONObject?>(null) }
    var busy by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun load() {
        busy = true
        err = ""
        scope.launch {
            try {
                me = JSONObject(Gh.apiGet("/user"))
            } catch (e: Exception) {
                err = errText(e)
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().background(GhBg).verticalScroll(rememberScrollState())) {
        AppBar("我的账号", Session.login)
        when {
            busy -> Box(Modifier.fillMaxWidth().height(220.dp)) { LoadingBox() }
            err.isNotEmpty() -> ErrBox(err) { load() }
            else -> {
                val u = me ?: JSONObject()
                Column(Modifier.padding(14.dp)) {
                    // 头像卡
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = GhSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GhBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Avatar(u.str("avatar_url"), 74.dp, ring = GhGreen.copy(alpha = 0.35f))
                            Text(
                                u.str("login"), color = GhText, fontSize = 19.sp,
                                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp)
                            )
                            Text(
                                u.str("name").ifEmpty { "（未设置昵称）" },
                                color = GhDim, fontSize = 12.sp
                            )
                            if (u.str("bio").isNotEmpty()) {
                                Text(
                                    u.str("bio"), color = GhText, fontSize = 11.5.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatTile(u.num("public_repos").toString(), "仓库", GhBlue, Modifier.weight(1f))
                                StatTile(u.num("followers").toString(), "关注我", GhGreen, Modifier.weight(1f))
                                StatTile(u.num("following").toString(), "我关注", GhPurple, Modifier.weight(1f))
                            }
                        }
                    }

                    SectionTitle("快捷操作", Icons.Rounded.Bolt)
                    ActionRow("MCP 本地服务", if (McpServer.isRunning) "运行中 :" + McpServer.port else "未启动", Icons.Rounded.Dns, GhCyan) {
                        Nav.push("mcp:0")
                    }
                    Spacer(Modifier.height(9.dp))
                    ActionRow("我的 Gist", "代码片段管理", Icons.Rounded.Description, GhPurple) {
                        Nav.push("gists:0")
                    }
                    Spacer(Modifier.height(9.dp))
                    ActionRow("搜索", "仓库 / 代码 / Issue / 用户", Icons.Rounded.Search, GhBlue) {
                        Nav.push("search:0")
                    }
                    Spacer(Modifier.height(9.dp))
                    ActionRow("在浏览器打开主页", u.str("html_url").ifEmpty { "https://github.com/" + Session.login }, Icons.Rounded.Explore, GhGreen) {
                        openUrl(ctxHolder, u.str("html_url").ifEmpty { "https://github.com/" + Session.login })
                    }

                    SectionTitle("令牌信息", Icons.Rounded.CheckCircle)
                    GhCard {
                        KV("权限范围", Gh.lastScopes.ifEmpty { "（fine-grained）" })
                        KV("限流额度", Gh.lastRemaining + "/" + Gh.lastLimit)
                        KV("存储位置", "本机 SharedPreferences")
                        Text(
                            "令牌只保存在本机，不会上传到任何服务器。MCP 服务调用时也使用同一个令牌。",
                            color = GhDim, fontSize = 10.5.sp, modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    PrimaryBtn("退出登录", Icons.Rounded.Person) { onLogout() }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
