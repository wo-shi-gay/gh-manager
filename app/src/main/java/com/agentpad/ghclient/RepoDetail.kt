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
    val tabs = listOf("概览", "代码", "Issues", "PR", "提交", "Actions", "设置")

    Column(Modifier.fillMaxSize().background(GhBg)) {
        HeadBar(fullName, "仓库详情", onBack = { Nav.pop() },
            right = { SmallBtn("↗", GhDim) { openUrl(ctxHolder, "https://github.com/$fullName") } })
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            tabs.forEachIndexed { i, s -> Pill(s, tab == i) { tab = i } }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> RepoOverview(fullName, owner, repo, onOpenIssue = { n, isPr ->
                    Nav.push("issue:$fullName|$n|${if (isPr) "pr" else "issue"}")
                })
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
fun RepoOverview(fullName: String, owner: String, repo: String, onOpenIssue: (Int, Boolean) -> Unit) {
    var o by remember { mutableStateOf<JSONObject?>(null) }
    var busy by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var starred by remember { mutableStateOf(false) }
    var watching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        busy = true; err = ""
        scope.launch {
            try {
                o = JSONObject(Gh.apiGet("/repos/$fullName"))
                starred = try { Gh.apiGet("/user/starred/$fullName"); true } catch (_: Exception) { false }
                watching = try {
                    JSONObject(Gh.apiGet("/repos/$fullName/subscription")).flag("subscribed")
                } catch (_: Exception) { false }
            } catch (e: Exception) {
                err = errText(e)
            } finally {
                busy = false
            }
        }
    }

    fun toggleStar(starred: Boolean) {
        scope.launch {
            try {
                if (starred) Gh.apiDelete("/user/starred/$fullName")
                else Gh.apiSend("PUT", "/user/starred/$fullName", JSONObject())
                msg = if (starred) "已取消 Star" else "已 Star ⭐"
                load()
            } catch (e: Exception) { msg = errText(e) }
        }
    }

    fun toggleWatch(watching: Boolean) {
        scope.launch {
            try {
                if (watching) Gh.apiDelete("/repos/$fullName/subscription")
                else Gh.apiSend("PUT", "/repos/$fullName/subscription", JSONObject().put("subscribed", true))
                msg = if (watching) "已取消关注" else "已关注 👀"
                load()
            } catch (e: Exception) { msg = errText(e) }
        }
    }

    fun fork() {
        scope.launch {
            try {
                Gh.apiSend("POST", "/repos/$fullName/forks", JSONObject())
                msg = "Fork 成功 ⑂"
            } catch (e: Exception) { msg = errText(e) }
        }
    }

    LaunchedEffect(fullName) { load() }

    when {
        busy -> LoadingBox()
        err.isNotEmpty() -> ErrBox(err) { load() }
        else -> {
            val d = o ?: JSONObject()
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                GhCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(d.obj("owner").str("avatar_url"), 36.dp)
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(d.str("full_name"), color = GhBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                (if (d.flag("private")) "私有" else "公开") + " · " +
                                    d.str("visibility") + (if (d.flag("fork")) " · Fork" else ""),
                                color = GhDim, fontSize = 10.5.sp
                            )
                        }
                    }
                    if (d.str("description").isNotEmpty()) {
                        Text(
                            d.str("description"), color = GhText, fontSize = 12.5.sp,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                    Gap(10)
                    Row(Modifier.fillMaxWidth()) {
                        StatCol("★ Star", d.num("stargazers_count").toString())
                        StatCol("⑂ Fork", d.num("forks_count").toString())
                        StatCol("👁 Watch", d.num("subscribers_count").toString())
                        StatCol("⚠ Issue", d.num("open_issues_count").toString())
                    }
                }
                Gap(10)
                Row(Modifier.fillMaxWidth()) {
                    SmallBtn(if (starred) "★ 取消 Star" else "★ Star", GhYellow) { toggleStar(starred) }
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                    SmallBtn(if (watching) "👁 取消 Watch" else "👁 Watch", GhBlue) { toggleWatch(watching) }
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                    SmallBtn("⑂ Fork", GhPurple) { fork() }
                }
                if (msg.isNotEmpty()) {
                    Gap(8)
                    Text(msg, color = GhGreen, fontSize = 11.5.sp)
                }
                Gap(12)
                GhCard {
                    KV("默认分支", d.str("default_branch"))
                    KV("主语言", d.str("language").ifEmpty { "—" } + " · ${d.num("size")} KB")
                    KV("创建", d.str("created_at").take(10))
                    KV("最后推送", isoToRel(d.str("pushed_at")))
                    KV("License", d.obj("license").str("spdx_id").ifEmpty { "—" })
                    KV("主页", d.str("homepage").ifEmpty { "—" })
                    KV("克隆", d.str("clone_url"))
                    if (d.obj("permissions").length() > 0) {
                        val p = d.obj("permissions")
                        KV("我的权限", listOfNotNull(
                            if (p.flag("admin")) "admin" else null,
                            if (p.flag("maintain")) "maintain" else null,
                            if (p.flag("push")) "push" else null,
                            if (p.flag("triage")) "triage" else null,
                            if (p.flag("pull")) "pull" else null
                        ).joinToString(", ").ifEmpty { "—" })
                    }
                }
                Gap(10)
                Row(Modifier.fillMaxWidth()) {
                    SmallBtn("+ 新建 Issue", GhGreen) { Nav.push("newissue:$fullName|") }
                }
            }
        }
    }
}

@Composable
private fun StatCol(label: String, value: String) {
    Column(Modifier.padding(end = 16.dp)) {
        Text(value, color = GhText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = GhDim, fontSize = 9.5.sp)
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
        busy = true; err = ""
        scope.launch {
            try {
                val txt = Gh.apiGet("/repos/$fullName/contents/$path")
                rows = when {
                    txt.trimStart().startsWith("[") -> JSONArray(txt).toObjList()
                    else -> emptyList()
                }
            } catch (e: Exception) { err = errText(e); rows = emptyList() }
            finally { busy = false }
        }
    }

    fun openFile(p: String, name: String) {
        busy = true; err = ""
        scope.launch {
            try {
                val txt = Gh.apiGet("/repos/$fullName/contents/$p")
                val o = JSONObject(txt)
                fileName = name
                fileContent = base64Decode(o.str("content"))
            } catch (e: Exception) { err = errText(e) }
            finally { busy = false }
        }
    }

    LaunchedEffect(path) { load() }

    if (fileContent.isNotEmpty()) {
        Column(Modifier.fillMaxSize()) {
            HeadBar(fileName, "文件内容", onBack = { fileContent = "" })
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp), color = GhSurface2,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Mono(fileContent, Modifier.padding(10.dp))
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallBtn("/ " + (if (path.isEmpty()) "根目录" else path), GhBlue) {
                path = if (path.contains("/")) path.substringBeforeLast("/") else ""
            }
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            SmallBtn("刷新", GhDim) { load() }
        }
        when {
            busy -> LoadingBox()
            err.isNotEmpty() -> ErrBox(err) { load() }
            rows.isEmpty() -> EmptyBox("这个目录是空的喵")
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(rows.size) { i ->
                    val f = rows[i]
                    val isDir = f.str("type") == "dir"
                    Surface(
                        shape = RoundedCornerShape(9.dp), color = GhSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GhBorder),
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (isDir) path = f.str("path") else openFile(f.str("path"), f.str("name"))
                        }
                    ) {
                        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isDir) "📁" else "📄", fontSize = 13.sp)
                            Text(
                                f.str("name"), color = if (isDir) GhBlue else GhText,
                                fontSize = 12.5.sp, modifier = Modifier.padding(start = 8.dp).weight(1f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text("${f.num("size")} B", color = GhDim, fontSize = 10.sp)
                        }
                    }
                }
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
        busy = true; err = ""
        scope.launch {
            try {
                rows = JSONArray(Gh.apiGet("/repos/$fullName/issues?state=$state&per_page=60")).toObjList()
                    .filter { it.str("pull_request").isEmpty() }
            } catch (e: Exception) { err = errText(e); rows = emptyList() }
            finally { busy = false }
        }
    }

    LaunchedEffect(state) { load() }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Pill("开启", state == "open") { state = "open" }
            Pill("已关闭", state == "closed") { state = "closed" }
            Pill("全部", state == "all") { state = "all" }
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            SmallBtn("+ 新建", GhGreen) { Nav.push("newissue:$fullName|") }
        }
        when {
            busy -> LoadingBox()
            err.isNotEmpty() -> ErrBox(err) { load() }
            rows.isEmpty() -> EmptyBox("没有 Issue 喵")
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(rows.size) { i ->
                    val o = rows[i]
                    IssueRow(o, false) { Nav.push("issue:$fullName|${o.num("number")}|issue") }
                }
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
        busy = true; err = ""
        scope.launch {
            try {
                rows = JSONArray(Gh.apiGet("/repos/$fullName/pulls?state=$state&per_page=60")).toObjList()
            } catch (e: Exception) { err = errText(e); rows = emptyList() }
            finally { busy = false }
        }
    }

    LaunchedEffect(state) { load() }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Pill("开启", state == "open") { state = "open" }
            Pill("已关闭", state == "closed") { state = "closed" }
            Pill("全部", state == "all") { state = "all" }
        }
        when {
            busy -> LoadingBox()
            err.isNotEmpty() -> ErrBox(err) { load() }
            rows.isEmpty() -> EmptyBox("没有 PR 喵")
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(rows.size) { i ->
                    val o = rows[i]
                    IssueRow(o, true) { Nav.push("issue:$fullName|${o.num("number")}|pr") }
                }
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
        busy = true; err = ""
        scope.launch {
            try { rows = JSONArray(Gh.apiGet("/repos/$fullName/commits?per_page=60")).toObjList() }
            catch (e: Exception) { err = errText(e); rows = emptyList() }
            finally { busy = false }
        }
    }

    fun open(sha: String) {
        busy = true; err = ""
        scope.launch {
            try { detail = JSONObject(Gh.apiGet("/repos/$fullName/commits/$sha")) }
            catch (e: Exception) { err = errText(e) }
            finally { busy = false }
        }
    }

    LaunchedEffect(fullName) { load() }

    val d = detail
    if (d != null) {
        Column(Modifier.fillMaxSize()) {
            HeadBar("提交 " + d.str("sha").take(7), d.obj("commit").obj("author").str("name"),
                onBack = { detail = null })
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp)) {
                GhCard {
                    Text(d.obj("commit").str("message"), color = GhText, fontSize = 12.5.sp,
                        fontFamily = FontFamily.Monospace)
                }
                Gap(10)
                val files = d.arr("files").toObjList()
                Text("改动 ${files.size} 个文件", color = GhDim, fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 6.dp))
                files.forEach { f ->
                    GhCard(Modifier.padding(bottom = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (f.str("status")) {
                                    "added" -> "🟢"; "removed" -> "🔴"; "renamed" -> "🔵"; else -> "🟡"
                                }, fontSize = 11.sp
                            )
                            Text(f.str("filename"), color = GhBlue, fontSize = 11.5.sp,
                                modifier = Modifier.padding(start = 6.dp).weight(1f),
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("+${f.num("additions")} -${f.num("deletions")}", color = GhDim, fontSize = 10.sp)
                        }
                        val patch = f.str("patch")
                        if (patch.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp), color = GhSurface2,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Text(
                                    patch.take(3000), color = GhText, fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        HeadBar("提交历史", "共 ${rows.size} 条", right = { SmallBtn("刷新", GhBlue) { load() } })
        when {
            busy -> LoadingBox()
            err.isNotEmpty() -> ErrBox(err) { load() }
            rows.isEmpty() -> EmptyBox("没有提交记录喵")
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(rows.size) { i ->
                    val o = rows[i]
                    CommitRow(o) { open(o.str("sha")) }
                }
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
        busy = true; err = ""
        scope.launch {
            try {
                workflows = JSONObject(Gh.apiGet("/repos/$fullName/actions/workflows"))
                    .arr("workflows").toObjList()
                runs = JSONObject(Gh.apiGet("/repos/$fullName/actions/runs?per_page=40"))
                    .arr("workflow_runs").toObjList()
            } catch (e: Exception) { err = errText(e) }
            finally { busy = false }
        }
    }

    fun dispatch(wfId: Long, ref: String) {
        scope.launch {
            try {
                Gh.apiSend(
                    "POST", "/repos/$fullName/actions/workflows/$wfId/dispatches",
                    JSONObject().put("ref", ref)
                )
                msg = "✅ 已触发工作流（ref=$ref），几秒后刷新看进度喵"
            } catch (e: Exception) { msg = errText(e) }
        }
    }

    LaunchedEffect(fullName) { loadAll() }

    Column(Modifier.fillMaxSize()) {
        HeadBar("Actions 云构建", "${workflows.size} 个工作流 · ${runs.size} 次运行",
            right = { SmallBtn("刷新", GhBlue) { loadAll() } })

        if (msg.isNotEmpty()) {
            Surface(color = GhSurface2, modifier = Modifier.fillMaxWidth()) {
                Text(msg, color = GhGreen, fontSize = 11.sp, modifier = Modifier.padding(10.dp))
            }
        }

        when {
            busy -> LoadingBox()
            err.isNotEmpty() -> ErrBox(err) { loadAll() }
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(Modifier.padding(10.dp)) {
                    if (workflows.isNotEmpty()) {
                        SectionTitle("工作流（点击可触发）")
                        workflows.forEach { wf ->
                            Surface(
                                shape = RoundedCornerShape(10.dp), color = GhSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GhBorder),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp).clickable {
                                    pickedWf = if (pickedWf?.longNum("id") == wf.longNum("id")) null else wf
                                }
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(wf.str("name"), color = GhText, fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(wf.str("path"), color = GhDim, fontSize = 10.sp,
                                        modifier = Modifier.padding(top = 3.dp))
                                    if (pickedWf?.longNum("id") == wf.longNum("id")) {
                                        Gap(8)
                                        Text("选择分支触发：", color = GhDim, fontSize = 10.5.sp)
                                        Gap(6)
                                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                                            SmallBtn("main", GhGreen, filled = true) {
                                                dispatch(wf.longNum("id"), "main")
                                            }
                                            androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
                                            SmallBtn("master", GhBlue) {
                                                dispatch(wf.longNum("id"), "master")
                                            }
                                            androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
                                            SmallBtn("dev", GhPurple) {
                                                dispatch(wf.longNum("id"), "dev")
                                            }
                                        }
                                        Text(
                                            "⚠ workflow_dispatch 需要 workflow 文件里声明 on: workflow_dispatch",
                                            color = GhYellow, fontSize = 9.5.sp,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text("这个仓库没有配置 Actions 工作流喵", color = GhDim, fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp))
                    }

                    SectionTitle("运行记录")
                    runs.forEach { r ->
                        RunRow(r) { Nav.push("run:$fullName|${r.longNum("id")}") }
                        androidx.compose.foundation.layout.Spacer(Modifier.size(7.dp))
                    }
                    androidx.compose.foundation.layout.Spacer(Modifier.size(20.dp))
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
        } catch (_: Exception) { }
    }

    fun save() {
        busy = true; msg = ""
        scope.launch {
            try {
                val b = JSONObject()
                b.put("description", desc)
                b.put("private", isPrivate)
                b.put("archived", archived)
                Gh.apiSend("PATCH", "/repos/$fullName", b)
                msg = "✅ 已保存"
            } catch (e: Exception) { msg = errText(e) }
            finally { busy = false }
        }
    }

    fun del() {
        busy = true; msg = ""
        scope.launch {
            try {
                Gh.apiDelete("/repos/$fullName")
                msg = "✅ 仓库已删除"
                Nav.pop()
            } catch (e: Exception) { msg = errText(e) }
            finally { busy = false }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        SectionTitle("基本信息")
        Field(desc, { desc = it }, "描述", single = false, minLines = 2)
        Gap(10)
        GhCard {
            Row(
                Modifier.fillMaxWidth().clickable { isPrivate = !isPrivate },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (isPrivate) "☑" else "☐", color = if (isPrivate) GhGreen else GhDim, fontSize = 16.sp)
                Text("  私有仓库", color = GhText, fontSize = 13.sp)
            }
            Gap(6)
            Row(
                Modifier.fillMaxWidth().clickable { archived = !archived },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (archived) "☑" else "☐", color = if (archived) GhYellow else GhDim, fontSize = 16.sp)
                Text("  归档仓库（只读）", color = GhText, fontSize = 13.sp)
            }
        }
        if (msg.isNotEmpty()) {
            Gap(8)
            Text(msg, color = if (msg.startsWith("✅")) GhGreen else GhRed, fontSize = 11.5.sp)
        }
        Gap(12)
        SmallBtn(if (busy) "保存中…" else "保存修改", GhGreen, filled = true) { if (!busy) save() }

        Gap(24)
        SectionTitle("危险操作")
        if (!confirmDelete) {
            SmallBtn("删除这个仓库", GhRed) { confirmDelete = true }
        } else {
            GhCard {
                Text("确认删除 $fullName ？此操作不可撤销！", color = GhRed, fontSize = 12.sp)
                Gap(10)
                Row {
                    SmallBtn("确认删除", GhRed, filled = true) { if (!busy) del() }
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                    SmallBtn("取消", GhDim) { confirmDelete = false }
                }
            }
        }
        Gap(24)
    }
}
