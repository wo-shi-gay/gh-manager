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
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun RepoDetailScreen(fullName: String) {
    val pair = splitRepo(fullName)
    val owner = pair.first
    val repo = pair.second
    val ctx = LocalContext.current
    ctxHolder = ctx

    var tab by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().background(GhBg)) {
        AppBar(
            repo.ifEmpty { fullName },
            owner,
            onBack = { Nav.pop() },
            actions = {
                IconBtn(Icons.Rounded.OpenInNew, GhBlue) {
                    openUrl(ctxHolder, "https://github.com/$fullName")
                }
            }
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 8.dp)
        ) {
            Pill("概览", tab == 0, GhGreen, Icons.Rounded.Explore) { tab = 0 }
            Pill("代码", tab == 1, GhBlue, Icons.Rounded.Code) { tab = 1 }
            Pill("Issues", tab == 2, GhGreen, Icons.Rounded.CheckCircle) { tab = 2 }
            Pill("PR", tab == 3, GhPurple, Icons.Rounded.Bolt) { tab = 3 }
            Pill("提交", tab == 4, GhCyan, Icons.Rounded.Bolt) { tab = 4 }
            Pill("Actions", tab == 5, GhYellow, Icons.Rounded.PlayArrow) { tab = 5 }
            Pill("设置", tab == 6, GhDim, Icons.Rounded.Settings) { tab = 6 }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> RepoOverview(fullName)
                1 -> RepoCode(fullName)
                2 -> RepoIssues(fullName)
                3 -> RepoPulls(fullName)
                4 -> RepoCommits(fullName)
                5 -> RepoActions(fullName)
                else -> RepoSettings(fullName)
            }
        }
    }
}

/** 概览 */
@Composable
fun RepoOverview(fullName: String) {
    var o by remember { mutableStateOf<JSONObject?>(null) }
    var busy by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var starred by remember { mutableStateOf(false) }
    var watching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        busy = true
        err = ""
        scope.launch {
            try {
                o = JSONObject(Gh.apiGet("/repos/$fullName"))
                starred = try {
                    Gh.apiGet("/user/starred/$fullName")
                    true
                } catch (_: Exception) {
                    false
                }
                watching = try {
                    JSONObject(Gh.apiGet("/repos/$fullName/subscription")).flag("subscribed")
                } catch (_: Exception) {
                    false
                }
            } catch (e: Exception) {
                err = errText(e)
            } finally {
                busy = false
            }
        }
    }

    fun toggleStar() {
        scope.launch {
            try {
                if (starred) Gh.apiDelete("/user/starred/$fullName")
                else Gh.apiSend("PUT", "/user/starred/$fullName", JSONObject())
                msg = if (starred) "已取消 Star" else "已 Star ⭐"
                load()
            } catch (e: Exception) {
                msg = errText(e)
            }
        }
    }

    fun toggleWatch() {
        scope.launch {
            try {
                if (watching) Gh.apiDelete("/repos/$fullName/subscription")
                else Gh.apiSend("PUT", "/repos/$fullName/subscription", JSONObject().put("subscribed", true))
                msg = if (watching) "已取消关注" else "已关注 👀"
                load()
            } catch (e: Exception) {
                msg = errText(e)
            }
        }
    }

    fun fork() {
        scope.launch {
            try {
                Gh.apiSend("POST", "/repos/$fullName/forks", JSONObject())
                msg = "Fork 成功 ⑂"
            } catch (e: Exception) {
                msg = errText(e)
            }
        }
    }

    LaunchedEffect(fullName) { load() }

    when {
        busy -> LoadingBox()
        err.isNotEmpty() -> ErrBox(err) { load() }
        else -> {
            val d = o ?: JSONObject()
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
                // 头部卡
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = GhSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GhBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(d.obj("owner").str("avatar_url"), 38.dp)
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    d.str("full_name"), color = GhText,
                                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StateChip(
                                        if (d.flag("private")) "私有" else "公开",
                                        if (d.flag("private")) GhYellow else GhGreen
                                    )
                                    if (d.flag("fork")) {
                                        Spacer(Modifier.width(5.dp))
                                        StateChip("Fork", GhDim)
                                    }
                                    if (d.flag("archived")) {
                                        Spacer(Modifier.width(5.dp))
                                        StateChip("已归档", GhRed)
                                    }
                                    Text(
                                        "  " + d.str("default_branch"),
                                        color = GhDim, fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        if (d.str("description").isNotEmpty()) {
                            Text(
                                d.str("description"), color = GhText, fontSize = 12.5.sp,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatTile(d.num("stargazers_count").toString(), "★ Star", GhYellow, Modifier.weight(1f))
                            StatTile(d.num("forks_count").toString(), "⑂ Fork", GhPurple, Modifier.weight(1f))
                            StatTile(d.num("subscribers_count").toString(), "👁 Watch", GhBlue, Modifier.weight(1f))
                            StatTile(d.num("open_issues_count").toString(), "⚠ Issue", GhRed, Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    SmallBtn(
                        if (starred) "已 Star" else "Star", GhYellow, filled = starred,
                        icon = Icons.Rounded.Star
                    ) { toggleStar() }
                    Spacer(Modifier.width(8.dp))
                    SmallBtn(
                        if (watching) "已关注" else "Watch", GhBlue, filled = watching,
                        icon = Icons.Rounded.Explore
                    ) { toggleWatch() }
                    Spacer(Modifier.width(8.dp))
                    SmallBtn("Fork", GhPurple, icon = Icons.Rounded.Folder) { fork() }
                }
                if (msg.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(msg, color = GhGreen, fontSize = 11.5.sp)
                }

                SectionTitle("仓库信息", Icons.Rounded.Description)
                GhCard {
                    KV("主语言", d.str("language").ifEmpty { "—" } + " · " + d.num("size") + " KB")
                    KV("创建时间", d.str("created_at").take(10))
                    KV("最后推送", isoToRel(d.str("pushed_at")))
                    KV("License", d.obj("license").str("spdx_id").ifEmpty { "—" })
                    KV("默认分支", d.str("default_branch"))
                    KV("Wiki", if (d.flag("has_wiki")) "有" else "无")
                    KV("Issues", if (d.flag("has_issues")) "已启用" else "已关闭")
                    KV("主页", d.str("homepage").ifEmpty { "—" })
                    KV("克隆地址", d.str("clone_url"), mono = true)
                    val perm = d.obj("permissions")
                    if (perm.length() > 0) {
                        KV(
                            "我的权限",
                            listOfNotNull(
                                if (perm.flag("admin")) "admin" else null,
                                if (perm.flag("maintain")) "maintain" else null,
                                if (perm.flag("push")) "push" else null,
                                if (perm.flag("triage")) "triage" else null,
                                if (perm.flag("pull")) "pull" else null
                            ).joinToString(", ").ifEmpty { "—" }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                PrimaryBtn("新建 Issue", Icons.Rounded.Add) { Nav.push("newissue:$fullName|") }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** 代码浏览 */
@Composable
fun RepoCode(fullName: String) {
    var path by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var busy by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var fileContent by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun load() {
        busy = true
        err = ""
        scope.launch {
            try {
                val txt = Gh.apiGet("/repos/$fullName/contents/$path")
                rows = if (txt.trimStart().startsWith("[")) JSONArray(txt).toObjList() else emptyList()
            } catch (e: Exception) {
                err = errText(e)
                rows = emptyList()
            } finally {
                busy = false
            }
        }
    }

    fun openFile(p: String, name: String) {
        busy = true
        err = ""
        scope.launch {
            try {
                val txt = Gh.apiGet("/repos/$fullName/contents/$p")
                val o = JSONObject(txt)
                fileName = name
                fileContent = base64Decode(o.str("content"))
            } catch (e: Exception) {
                err = errText(e)
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(path) { load() }

    if (fileContent.isNotEmpty()) {
        Column(Modifier.fillMaxSize()) {
            AppBar(fileName, "文件内容", onBack = { fileContent = "" })
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp), color = GhBg2,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GhBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Mono(fileContent, Modifier.padding(12.dp))
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallBtn(
                if (path.isEmpty()) "根目录" else "‹ " + path.substringAfterLast('/'),
                GhBlue, icon = Icons.Rounded.Folder
            ) {
                path = if (path.contains("/")) path.substringBeforeLast("/") else ""
            }
            Spacer(Modifier.width(8.dp))
            SmallBtn("刷新", GhDim, icon = Icons.Rounded.Refresh) { load() }
        }
        when {
            busy -> LoadingBox()
            err.isNotEmpty() -> ErrBox(err) { load() }
            rows.isEmpty() -> EmptyBox("这个目录是空的喵", "📂")
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(rows.size) { i ->
                    val f = rows[i]
                    val isDir = f.str("type") == "dir"
                    Surface(
                        shape = RoundedCornerShape(11.dp), color = GhSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GhBorder),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                            .clickable {
                                if (isDir) path = f.str("path")
                                else openFile(f.str("path"), f.str("name"))
                            }
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(26.dp).clip(RoundedCornerShape(8.dp))
                                    .background(if (isDir) GhBlue.copy(alpha = 0.14f) else GhSurface2),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isDir) Icons.Rounded.Folder else Icons.Rounded.Description,
                                    null,
                                    tint = if (isDir) GhBlue else GhDim,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                f.str("name"), color = if (isDir) GhBlue else GhText,
                                fontSize = 12.5.sp, modifier = Modifier.weight(1f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(f.num("size").toString() + " B", color = GhDim, fontSize = 9.5.sp)
                        }
                    }
                }
                item { Spacer(Modifier.height(14.dp)) }
            }
        }
    }
}

/** Issues */
@Composable
fun RepoIssues(fullName: String) {
    var state by remember { mutableStateOf("open") }
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var busy by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun load() {
        busy = true
        err = ""
        scope.launch {
            try {
                rows = JSONArray(Gh.apiGet("/repos/$fullName/issues?state=$state&per_page=60"))
                    .toObjList().filter { it.str("pull_request").isEmpty() }
            } catch (e: Exception) {
                err = errText(e)
                rows = emptyList()
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(state) { load() }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Pill("开启", state == "open", GhGreen) { state = "open" }
            Pill("已关闭", state == "closed", GhRed) { state = "closed" }
            Pill("全部", state == "all", GhBlue) { state = "all" }
            Spacer(Modifier.weight(1f))
            SmallBtn("新建", GhGreen, icon = Icons.Rounded.Add) { Nav.push("newissue:$fullName|") }
        }
        when {
            busy -> LoadingBox()
            err.isNotEmpty() -> ErrBox(err) { load() }
            rows.isEmpty() -> EmptyBox("没有 Issue 喵", "🎉")
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rows.size) { i ->
                    val o = rows[i]
                    IssueRow(o, false) { Nav.push("issue:$fullName|${o.num("number")}|issue") }
                }
                item { Spacer(Modifier.height(14.dp)) }
            }
        }
    }
}

/** PRs */
@Composable
fun RepoPulls(fullName: String) {
    var state by remember { mutableStateOf("open") }
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var busy by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun load() {
        busy = true
        err = ""
        scope.launch {
            try {
                rows = JSONArray(Gh.apiGet("/repos/$fullName/pulls?state=$state&per_page=60")).toObjList()
            } catch (e: Exception) {
                err = errText(e)
                rows = emptyList()
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(state) { load() }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Pill("开启", state == "open", GhGreen) { state = "open" }
            Pill("已关闭", state == "closed", GhRed) { state = "closed" }
            Pill("全部", state == "all", GhBlue) { state = "all" }
        }
        when {
            busy -> LoadingBox()
            err.isNotEmpty() -> ErrBox(err) { load() }
            rows.isEmpty() -> EmptyBox("没有 PR 喵", "🎉")
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rows.size) { i ->
                    val o = rows[i]
                    IssueRow(o, true) { Nav.push("issue:$fullName|${o.num("number")}|pr") }
                }
                item { Spacer(Modifier.height(14.dp)) }
            }
        }
    }
}

/** 提交历史 */
@Composable
fun RepoCommits(fullName: String) {
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var busy by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<JSONObject?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        busy = true
        err = ""
        scope.launch {
            try {
                rows = JSONArray(Gh.apiGet("/repos/$fullName/commits?per_page=60")).toObjList()
            } catch (e: Exception) {
                err = errText(e)
                rows = emptyList()
            } finally {
                busy = false
            }
        }
    }

    fun open(sha: String) {
        busy = true
        err = ""
        scope.launch {
            try {
                detail = JSONObject(Gh.apiGet("/repos/$fullName/commits/$sha"))
            } catch (e: Exception) {
                err = errText(e)
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(fullName) { load() }

    val d = detail
    if (d != null) {
        Column(Modifier.fillMaxSize()) {
            AppBar(
                "提交 " + d.str("sha").take(7),
                d.obj("commit").obj("author").str("name"),
                onBack = { detail = null }
            )
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                GhCard {
                    Text(
                        d.obj("commit").str("message"), color = GhText, fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                val files = d.arr("files").toObjList()
                SectionTitle("改动 " + files.size + " 个文件", Icons.Rounded.Description)
                files.forEach { f ->
                    GhCard(Modifier.padding(bottom = 9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val st = f.str("status")
                            StateChip(
                                when (st) {
                                    "added" -> "新增"
                                    "removed" -> "删除"
                                    "renamed" -> "改名"
                                    else -> "修改"
                                },
                                when (st) {
                                    "added" -> GhGreen
                                    "removed" -> GhRed
                                    "renamed" -> GhBlue
                                    else -> GhYellow
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                f.str("filename"), color = GhText, fontSize = 11.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "+" + f.num("additions") + " -" + f.num("deletions"),
                                color = GhDim, fontSize = 9.5.sp
                            )
                        }
                        val patch = f.str("patch")
                        if (patch.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(9.dp), color = GhBg2,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    patch.take(3000), color = GhText, fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(9.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "共 " + rows.size + " 次提交", color = GhDim, fontSize = 11.sp,
                modifier = Modifier.weight(1f)
            )
            SmallBtn("刷新", GhBlue, icon = Icons.Rounded.Refresh) { load() }
        }
        when {
            busy -> LoadingBox()
            err.isNotEmpty() -> ErrBox(err) { load() }
            rows.isEmpty() -> EmptyBox("没有提交记录喵", "📭")
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rows.size) { i ->
                    val o = rows[i]
                    CommitRow(o) { open(o.str("sha")) }
                }
                item { Spacer(Modifier.height(14.dp)) }
            }
        }
    }
}

/** Actions 工作流 */
@Composable
fun RepoActions(fullName: String) {
    var workflows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var runs by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var busy by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var pickedWf by remember { mutableStateOf<JSONObject?>(null) }
    val scope = rememberCoroutineScope()

    fun loadAll() {
        busy = true
        err = ""
        scope.launch {
            try {
                workflows = JSONObject(Gh.apiGet("/repos/$fullName/actions/workflows"))
                    .arr("workflows").toObjList()
                runs = JSONObject(Gh.apiGet("/repos/$fullName/actions/runs?per_page=40"))
                    .arr("workflow_runs").toObjList()
            } catch (e: Exception) {
                err = errText(e)
            } finally {
                busy = false
            }
        }
    }

    fun dispatch(wfId: Long, ref: String) {
        scope.launch {
            try {
                Gh.apiSend(
                    "POST", "/repos/$fullName/actions/workflows/$wfId/dispatches",
                    JSONObject().put("ref", ref)
                )
                msg = "✅ 已触发工作流（ref=$ref）"
            } catch (e: Exception) {
                msg = errText(e)
            }
        }
    }

    LaunchedEffect(fullName) { loadAll() }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                workflows.size.toString() + " 个工作流 · " + runs.size + " 次运行",
                color = GhDim, fontSize = 11.sp, modifier = Modifier.weight(1f)
            )
            SmallBtn("刷新", GhBlue, icon = Icons.Rounded.Refresh) { loadAll() }
        }

        if (msg.isNotEmpty()) {
            Surface(color = GhSurface2, modifier = Modifier.fillMaxWidth()) {
                Text(msg, color = GhGreen, fontSize = 11.sp, modifier = Modifier.padding(10.dp))
            }
        }

        when {
            busy -> LoadingBox()
            err.isNotEmpty() -> ErrBox(err) { loadAll() }
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(Modifier.padding(12.dp)) {
                    if (workflows.isNotEmpty()) {
                        SectionTitle("工作流（点开可触发）", Icons.Rounded.PlayArrow)
                        workflows.forEach { wf ->
                            val open = pickedWf?.longNum("id") == wf.longNum("id")
                            Surface(
                                shape = RoundedCornerShape(13.dp), color = GhSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, if (open) GhGreen.copy(alpha = 0.5f) else GhBorder
                                ),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .clickable { pickedWf = if (open) null else wf }
                            ) {
                                Column(Modifier.padding(13.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.size(28.dp).clip(RoundedCornerShape(9.dp))
                                                .background(GhYellow.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Rounded.PlayArrow, null,
                                                tint = GhYellow, modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(9.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                wf.str("name"), color = GhText, fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                wf.str("path"), color = GhDim, fontSize = 9.5.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                    if (open) {
                                        Spacer(Modifier.height(10.dp))
                                        Text("选择分支触发：", color = GhDim, fontSize = 10.5.sp)
                                        Spacer(Modifier.height(7.dp))
                                        Row(
                                            Modifier.fillMaxWidth()
                                                .horizontalScroll(rememberScrollState())
                                        ) {
                                            SmallBtn(
                                                "main", GhGreen, filled = true,
                                                icon = Icons.Rounded.PlayArrow
                                            ) { dispatch(wf.longNum("id"), "main") }
                                            Spacer(Modifier.width(6.dp))
                                            SmallBtn("master", GhBlue) { dispatch(wf.longNum("id"), "master") }
                                            Spacer(Modifier.width(6.dp))
                                            SmallBtn("dev", GhPurple) { dispatch(wf.longNum("id"), "dev") }
                                        }
                                        Text(
                                            "⚠ 需要 workflow 文件里声明 on: workflow_dispatch",
                                            color = GhYellow, fontSize = 9.5.sp,
                                            modifier = Modifier.padding(top = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            "这个仓库没有配置 Actions 工作流喵", color = GhDim, fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    SectionTitle("运行记录", Icons.Rounded.Bolt)
                    runs.forEach { r ->
                        RunRow(r) { Nav.push("run:$fullName|${r.longNum("id")}") }
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/** 仓库设置 */
@Composable
fun RepoSettings(fullName: String) {
    var desc by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var archived by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(fullName) {
        try {
            val o = JSONObject(Gh.apiGet("/repos/$fullName"))
            desc = o.str("description")
            isPrivate = o.flag("private")
            archived = o.flag("archived")
        } catch (_: Exception) {
        }
    }

    fun save() {
        busy = true
        msg = ""
        scope.launch {
            try {
                val b = JSONObject()
                b.put("description", desc)
                b.put("private", isPrivate)
                b.put("archived", archived)
                Gh.apiSend("PATCH", "/repos/$fullName", b)
                msg = "✅ 已保存"
            } catch (e: Exception) {
                msg = errText(e)
            } finally {
                busy = false
            }
        }
    }

    fun del() {
        busy = true
        msg = ""
        scope.launch {
            try {
                Gh.apiDelete("/repos/$fullName")
                msg = "✅ 仓库已删除"
                Nav.pop()
            } catch (e: Exception) {
                msg = errText(e)
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        SectionTitle("基本信息", Icons.Rounded.Settings)
        Field(desc, { desc = it }, "描述", single = false, minLines = 2)
        Spacer(Modifier.height(10.dp))
        GhCard {
            Row(
                Modifier.fillMaxWidth().clickable { isPrivate = !isPrivate },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isPrivate) "☑" else "☐",
                    color = if (isPrivate) GhYellow else GhDim, fontSize = 17.sp
                )
                Spacer(Modifier.width(8.dp))
                Text("私有仓库", color = GhText, fontSize = 13.sp)
            }
            Spacer(Modifier.height(9.dp))
            Row(
                Modifier.fillMaxWidth().clickable { archived = !archived },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (archived) "☑" else "☐",
                    color = if (archived) GhRed else GhDim, fontSize = 17.sp
                )
                Spacer(Modifier.width(8.dp))
                Text("归档仓库（只读）", color = GhText, fontSize = 13.sp)
            }
        }
        if (msg.isNotEmpty()) {
            Spacer(Modifier.height(9.dp))
            Text(
                msg, color = if (msg.startsWith("✅")) GhGreen else GhRed, fontSize = 11.5.sp
            )
        }
        Spacer(Modifier.height(14.dp))
        PrimaryBtn(if (busy) "保存中…" else "保存修改", Icons.Rounded.CheckCircle, enabled = !busy) {
            if (!busy) save()
        }

        Spacer(Modifier.height(26.dp))
        SectionTitle("危险操作", Icons.Rounded.Delete)
        if (!confirmDelete) {
            SmallBtn("删除这个仓库", GhRed, icon = Icons.Rounded.Delete) { confirmDelete = true }
        } else {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = GhRed.copy(alpha = 0.10f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GhRed.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "确认删除 $fullName ？此操作不可撤销！",
                        color = GhRed, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(11.dp))
                    Row {
                        SmallBtn("确认删除", GhRed, filled = true, icon = Icons.Rounded.Delete) {
                            if (!busy) del()
                        }
                        Spacer(Modifier.width(8.dp))
                        SmallBtn("取消", GhDim) { confirmDelete = false }
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}
