package com.agentpad.ghclient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
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
                3 -> ProfileTab(onLogout)
            }
        }
        BottomBar(tab) { tab = it }
    }
}

@Composable
private fun BottomBar(tab: Int, onPick: (Int) -> Unit) {
    Surface(color = GhSurface) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            val items = listOf("仓库", "通知", "发现", "我")
            items.forEachIndexed { i, s ->
                Column(
                    Modifier.weight(1f).clickable { onPick(i) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        s,
                        color = if (tab == i) GhGreen else GhDim,
                        fontSize = 12.sp,
                        fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal
                    )
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
                    "all" -> "/user/repos?per_page=100&sort=updated&affiliation=owner,collaborator,organization_member"
                    "mine" -> "/user/repos?per_page=100&sort=updated&affiliation=owner"
                    "starred" -> "/user/starred?per_page=100"
                    "orgs" -> "/user/repos?per_page=100&sort=updated&affiliation=organization_member"
                    else -> "/user/repos?per_page=100&sort=updated"
                }
                val txt = Gh.apiGet(path)
                rows = org.json.JSONArray(txt).toObjList()
            } catch (e: Exception) {
                err = errText(e)
                rows = emptyList()
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(filter) { load() }

    Column(Modifier.fillMaxSize()) {
        HeadBar(
            "我的仓库", "共 ${rows.size} 个",
            right = {
                SmallBtn("+ 新建", GhBlue) { Nav.push("create:new") }
            }
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Pill("全部", filter == "all") { filter = "all" }
            Pill("我拥有", filter == "mine") { filter = "mine" }
            Pill("已 Star", filter == "starred") { filter = "starred" }
            Pill("组织", filter == "orgs") { filter = "orgs" }
        }
        if (busy) {
            LoadingBox()
        } else if (err.isNotEmpty()) {
            ErrBox(err) { load() }
        } else if (rows.isEmpty()) {
            EmptyBox("还没有仓库喵，点右上角新建一个吧")
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rows.size) { i ->
                    val o = rows[i]
                    RepoRow(o) { Nav.push("repo:" + o.str("full_name")) }
                }
                item { androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp)) }
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
                err = errText(e); rows = emptyList()
            } finally { busy = false }
        }
    }

    fun markAllRead() {
        scope.launch {
            try {
                Gh.apiSend("PUT", "/notifications", JSONObject().put("read", true))
                load()
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(onlyUnread) { load() }

    Column(Modifier.fillMaxSize()) {
        HeadBar(
            "通知", "共 ${rows.size} 条",
            right = {
                Row {
                    SmallBtn("刷新", GhBlue) { load() }
                    androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
                    SmallBtn("全部已读", GhDim) { markAllRead() }
                }
            }
        )
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Pill("未读", onlyUnread) { onlyUnread = true }
            Pill("全部", !onlyUnread) { onlyUnread = false }
        }
        when {
            busy -> LoadingBox()
            err.isNotEmpty() -> ErrBox(err) { load() }
            rows.isEmpty() -> EmptyBox("没有通知喵，很清静～")
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rows.size) { i ->
                    val o = rows[i]
                    NotifRow(o) {
                        val full = o.obj("repository").str("full_name")
                        val num = o.obj("subject").str("url").substringAfterLast("/").toIntOrNull()
                        if (full.isNotEmpty()) {
                            if (num != null && num > 0) {
                                Nav.push("issue:$full|$num|issue")
                            } else {
                                Nav.push("repo:$full")
                            }
                        }
                    }
                }
                item { androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp)) }
            }
        }
    }
}

/** 发现页（搜索入口 + Gist + 限流） */
@Composable
fun ExploreTab() {
    Column(Modifier.fillMaxSize().background(GhBg).verticalScroll(rememberScrollState())) {
        HeadBar("发现", "搜索 · Gist · 状态")
        Column(Modifier.padding(14.dp)) {
            ActionRow("🔍  搜索仓库 / 代码 / Issue / 用户") { Nav.push("search:0") }
            Gap(8)
            ActionRow("📝  我的 Gist") { Nav.push("gists:0") }
            Gap(8)
            ActionRow("⭐  已 Star 的仓库") { }
            Gap(14)
            SectionTitle("API 状态")
            GhCard {
                KV("剩余额度", "${Gh.lastRemaining} / ${Gh.lastLimit}")
                KV("令牌 scopes", Gh.lastScopes.ifEmpty { "（未返回）" })
                KV("当前用户", Session.login.ifEmpty { "（未知）" })
            }
            Gap(14)
            SectionTitle("支持的操作能力")
            GhCard {
                val caps = listOf(
                    "仓库：列表 / 新建 / 删除 / Star / Watch / Fork / 编辑",
                    "代码：目录浏览 / 文件查看 / base64 解码",
                    "Issues：列表 / 详情 / 评论 / 开关 / 新建",
                    "PR：列表 / 详情 / 合并 / 关闭 / 评论",
                    "Commits：历史 / 文件 diff",
                    "Actions：工作流 / 运行记录 / 触发 / 取消 / 重跑 / Job 日志",
                    "通知：列表 / 标记已读",
                    "Gist：列表 / 打开",
                    "搜索：仓库 / 代码 / Issue / 用户"
                )
                caps.forEach {
                    Text("· $it", color = GhDim, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
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
        busy = true; err = ""
        scope.launch {
            try { me = JSONObject(Gh.apiGet("/user")) }
            catch (e: Exception) { err = errText(e) }
            finally { busy = false }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().background(GhBg).verticalScroll(rememberScrollState())) {
        HeadBar("我的账号", Session.login)
        when {
            busy -> Box(Modifier.fillMaxWidth()) { LoadingBox() }
            err.isNotEmpty() -> ErrBox(err) { load() }
            else -> {
                val u = me ?: JSONObject()
                Column(Modifier.padding(14.dp)) {
                    GhCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(u.str("avatar_url"), 56.dp)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(u.str("login"), color = GhText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(u.str("name").ifEmpty { "（未设置昵称）" }, color = GhDim, fontSize = 12.sp)
                                Text(
                                    "创建于 " + u.str("created_at").take(10),
                                    color = GhDim, fontSize = 10.5.sp
                                )
                            }
                        }
                        if (u.str("bio").isNotEmpty()) {
                            Text(u.str("bio"), color = GhText, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                        }
                        Gap(10)
                        Row(Modifier.fillMaxWidth()) {
                            Stat("仓库", u.num("public_repos").toString())
                            Stat("关注", u.num("followers").toString())
                            Stat("关注者", u.num("following").toString())
                        }
                    }
                    Gap(10)
                    ActionRow("🌐  在浏览器打开主页") {
                        openUrl(ctxHolder, u.str("html_url").ifEmpty { "https://github.com/" + Session.login })
                    }
                    Gap(8)
                    ActionRow("📝  我的 Gist") { Nav.push("gists:0") }
                    Gap(8)
                    ActionRow("🔍  搜索") { Nav.push("search:0") }
                    Gap(14)
                    SectionTitle("令牌")
                    GhCard {
                        KV("权限", Gh.lastScopes.ifEmpty { "（fine-grained）" })
                        KV("限流", "${Gh.lastRemaining}/${Gh.lastLimit}")
                        Text(
                            "令牌保存在本机 SharedPreferences，不会上传到任何服务器",
                            color = GhDim, fontSize = 10.5.sp, modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Gap(16)
                    SmallBtn("退出登录", GhRed, filled = false) { onLogout() }
                    Gap(20)
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(Modifier.padding(end = 22.dp)) {
        Text(value, color = GhText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(label, color = GhDim, fontSize = 10.5.sp)
    }
}
