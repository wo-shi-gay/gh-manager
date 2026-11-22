package com.agentpad.ghclient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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

/** Issue / PR 详情：正文 + 评论 + 开关 + 评论输入（PR 额外支持合并） */
@Composable
fun IssueDetailScreen(fullName: String, number: Int, isPr: Boolean) {
    val ctx = LocalContext.current
    ctxHolder = ctx
    var issue by remember { mutableStateOf<JSONObject?>(null) }
    var comments by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var busy by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var prInfo by remember { mutableStateOf<JSONObject?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        busy = true; err = ""
        scope.launch {
            try {
                issue = JSONObject(Gh.apiGet("/repos/$fullName/issues/$number"))
                comments = JSONArray(
                    Gh.apiGet("/repos/$fullName/issues/$number/comments?per_page=100")
                ).toObjList()
                if (isPr) {
                    prInfo = try { JSONObject(Gh.apiGet("/repos/$fullName/pulls/$number")) } catch (_: Exception) { null }
                }
            } catch (e: Exception) { err = errText(e) }
            finally { busy = false }
        }
    }

    fun toggleState() {
        val cur = issue?.str("state") ?: return
        val next = if (cur == "open") "closed" else "open"
        scope.launch {
            try {
                Gh.apiSend(
                    "PATCH", "/repos/$fullName/issues/$number",
                    JSONObject().put("state", next)
                )
                msg = if (next == "closed") "✅ 已关闭" else "✅ 已重新打开"
                load()
            } catch (e: Exception) { msg = errText(e) }
        }
    }

    fun comment() {
        if (draft.isBlank()) return
        scope.launch {
            try {
                Gh.apiSend(
                    "POST", "/repos/$fullName/issues/$number/comments",
                    JSONObject().put("body", draft)
                )
                draft = ""
                msg = "✅ 评论已发布"
                load()
            } catch (e: Exception) { msg = errText(e) }
        }
    }

    fun merge() {
        scope.launch {
            try {
                Gh.apiSend("PUT", "/repos/$fullName/pulls/$number/merge", JSONObject())
                msg = "✅ 已合并 PR"
                load()
            } catch (e: Exception) { msg = errText(e) }
        }
    }

    LaunchedEffect(number) { load() }

    Column(Modifier.fillMaxSize().background(GhBg)) {
        HeadBar(
            (if (isPr) "PR " else "Issue ") + "#$number",
            fullName,
            onBack = { Nav.pop() },
            right = { SmallBtn("↗", GhDim) { openUrl(ctxHolder, "https://github.com/$fullName/issues/$number") } }
        )
        when {
            busy -> LoadingBox()
            err.isNotEmpty() -> ErrBox(err) { load() }
            else -> {
                val o = issue ?: JSONObject()
                val state = o.str("state")
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                    GhCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StateChip(
                                if (state == "open") "开启" else "已关闭",
                                if (state == "open") GhGreen else GhRed
                            )
                            Text(
                                "#$number",
                                color = GhDim, fontSize = 11.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Text(
                            o.str("title"),
                            color = GhText, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(o.obj("user").str("avatar_url"), 22.dp)
                            Text(
                                " ${o.obj("user").str("login")} · 创建于 ${isoToRel(o.str("created_at"))}",
                                color = GhDim, fontSize = 10.5.sp
                            )
                        }
                    }
                    if (o.str("body").isNotEmpty()) {
                        Gap(10)
                        Surface(shape = RoundedCornerShape(12.dp), color = GhSurface, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(13.dp)) {
                                Text("描述", color = GhDim, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    o.str("body"), color = GhText, fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }

                    val labels = o.arr("labels").toObjList()
                    if (labels.isNotEmpty()) {
                        Gap(8)
                        Row(Modifier.fillMaxWidth()) {
                            labels.forEach { l ->
                                StateChip(l.str("name"), GhBlue)
                                androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
                            }
                        }
                    }

                    val pr = prInfo
                    if (isPr && pr != null) {
                        Gap(12)
                        GhCard {
                            Text("Pull Request", color = GhPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Gap(6)
                            KV("分支", pr.str("head").obj("label") + " → " + pr.str("base").obj("label"))
                            KV("可合并", if (pr.flag("mergeable")) "✅ 是" else "❌ 有冲突")
                            KV("改动", "+${pr.num("additions")} / -${pr.num("deletions")} · ${pr.num("changed_files")} 文件")
                            KV("提交数", pr.num("commits").toString())
                            if (state == "open") {
                                Gap(8)
                                Row {
                                    SmallBtn("合并 PR", GhPurple, filled = true) { merge() }
                                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                                    SmallBtn("关闭", GhRed) { toggleState() }
                                }
                            }
                        }
                    }

                    if (msg.isNotEmpty()) {
                        Gap(8)
                        Text(msg, color = if (msg.startsWith("✅")) GhGreen else GhRed, fontSize = 11.5.sp)
                    }

                    if (!isPr && state == "open") {
                        Gap(10)
                        SmallBtn("关闭 Issue", GhRed) { toggleState() }
                    } else if (!isPr && state == "closed") {
                        Gap(10)
                        SmallBtn("重新打开", GhGreen) { toggleState() }
                    }

                    SectionTitle("评论（${comments.size}）")
                    comments.forEach { c ->
                        Surface(
                            shape = RoundedCornerShape(12.dp), color = GhSurface,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Avatar(c.obj("user").str("avatar_url"), 20.dp)
                                    Text(
                                        " ${c.obj("user").str("login")} · ${isoToRel(c.str("created_at"))}",
                                        color = GhDim, fontSize = 10.5.sp
                                    )
                                }
                                Text(
                                    c.str("body"), color = GhText, fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }

                    SectionTitle("发表评论")
                    Field(draft, { draft = it }, "写点什么…", single = false, minLines = 3)
                    Gap(8)
                    SmallBtn("发布评论", GhGreen, filled = true) { comment() }
                    Gap(24)
                }
            }
        }
    }
}

/** Actions 运行详情：状态 + Job/Step 列表 + 取消/重跑 */
@Composable
fun RunDetailScreen(fullName: String, runId: Long) {
    val ctx = LocalContext.current
    ctxHolder = ctx
    var run by remember { mutableStateOf<JSONObject?>(null) }
    var jobs by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var busy by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var logText by remember { mutableStateOf("") }
    var logTitle by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun load() {
        busy = true; err = ""
        scope.launch {
            try {
                run = JSONObject(Gh.apiGet("/repos/$fullName/actions/runs/$runId"))
                jobs = JSONObject(
                    Gh.apiGet("/repos/$fullName/actions/runs/$runId/jobs?per_page=100")
                ).arr("jobs").toObjList()
            } catch (e: Exception) { err = errText(e) }
            finally { busy = false }
        }
    }

    fun cancel() {
        scope.launch {
            try {
                Gh.apiSend("POST", "/repos/$fullName/actions/runs/$runId/cancel", JSONObject())
                msg = "✅ 已请求取消"
                load()
            } catch (e: Exception) { msg = errText(e) }
        }
    }

    fun rerun() {
        scope.launch {
            try {
                Gh.apiSend("POST", "/repos/$fullName/actions/runs/$runId/rerun", JSONObject())
                msg = "✅ 已请求重新运行"
                load()
            } catch (e: Exception) { msg = errText(e) }
        }
    }

    fun loadLog(jobId: Long, name: String) {
        logTitle = name
        logText = "加载日志中…"
        scope.launch {
            try {
                logText = Gh.apiGet("/repos/$fullName/actions/jobs/$jobId/logs")
                if (logText.isBlank()) logText = "（日志为空）"
            } catch (e: Exception) {
                logText = "日志获取失败：" + errText(e) +
                    "\n\n（注：日志接口返回 302 跳转到临时下载地址，需跟随重定向）"
            }
        }
    }

    LaunchedEffect(runId) { load() }

    if (logText.isNotEmpty()) {
        Column(Modifier.fillMaxSize().background(GhBg)) {
            HeadBar(logTitle, "Job 日志", onBack = { logText = "" })
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp)) {
                Surface(shape = RoundedCornerShape(10.dp), color = GhSurface2, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        logText, color = GhText, fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace, modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(GhBg)) {
        HeadBar("运行 #" + (run?.num("run_number") ?: 0), fullName, onBack = { Nav.pop() },
            right = { SmallBtn("刷新", GhBlue) { load() } })

        when {
            busy -> LoadingBox()
            err.isNotEmpty() -> ErrBox(err) { load() }
            else -> {
                val r = run ?: JSONObject()
                val status = r.str("status")
                val conclusion = r.str("conclusion")
                val c = runColor(if (status == "completed") conclusion else status)
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                    GhCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StateChip(
                                when (if (status == "completed") conclusion else status) {
                                    "success" -> "成功"
                                    "failure" -> "失败"
                                    "in_progress" -> "运行中"
                                    "queued" -> "排队中"
                                    "cancelled" -> "已取消"
                                    else -> conclusion.ifEmpty { status }
                                },
                                c
                            )
                            Text("  ${r.str("name")}", color = GhText, fontSize = 12.5.sp)
                        }
                        Text(
                            r.str("display_title").ifEmpty { r.str("name") },
                            color = GhText, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Gap(8)
                        KV("分支", r.str("head_branch"))
                        KV("SHA", r.str("head_sha").take(10))
                        KV("事件", r.str("event"))
                        KV("尝试", "#" + r.num("run_attempt"))
                        KV("开始", isoToRel(r.str("run_started_at")))
                        KV("更新", isoToRel(r.str("updated_at")))
                    }
                    if (msg.isNotEmpty()) {
                        Gap(8)
                        Text(msg, color = if (msg.startsWith("✅")) GhGreen else GhRed, fontSize = 11.5.sp)
                    }
                    Gap(10)
                    Row(Modifier.fillMaxWidth()) {
                        if (status != "completed") {
                            SmallBtn("取消运行", GhRed) { cancel() }
                            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                        }
                        SmallBtn("重新运行", GhBlue) { rerun() }
                        androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                        SmallBtn("↗ 网页", GhDim) { openUrl(ctxHolder, r.str("html_url")) }
                    }

                    SectionTitle("Jobs（${jobs.size}）")
                    jobs.forEach { j ->
                        Surface(
                            shape = RoundedCornerShape(12.dp), color = GhSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GhBorder),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val jc = runColor(
                                        if (j.str("status") == "completed") j.str("conclusion") else j.str("status")
                                    )
                                    StateChip(
                                        if (j.str("status") == "completed") j.str("conclusion") else j.str("status"),
                                        jc
                                    )
                                    Text(
                                        "  ${j.str("name")}",
                                        color = GhText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    "运行于 ${j.str("runner_name").ifEmpty { "—" }} · ${isoToRel(j.str("started_at"))}",
                                    color = GhDim, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp)
                                )
                                val steps = j.arr("steps").toObjList()
                                if (steps.isNotEmpty()) {
                                    Gap(6)
                                    steps.forEach { s ->
                                        Row(
                                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val sc = runColor(
                                                if (s.str("status") == "completed") s.str("conclusion") else s.str("status")
                                            )
                                            Text(
                                                when (if (s.str("status") == "completed") s.str("conclusion") else s.str("status")) {
                                                    "success" -> "✔"
                                                    "failure" -> "✖"
                                                    "skipped" -> "⊘"
                                                    "in_progress" -> "◐"
                                                    else -> "○"
                                                },
                                                color = sc, fontSize = 11.sp
                                            )
                                            Text(
                                                "  " + s.str("name"),
                                                color = if (s.str("status") == "completed" && s.str("conclusion") == "success") GhDim else GhText,
                                                fontSize = 10.5.sp, modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                                Gap(8)
                                SmallBtn("查看日志", GhBlue) { loadLog(j.longNum("id"), j.str("name")) }
                            }
                        }
                    }
                    Gap(20)
                }
            }
        }
    }
}
