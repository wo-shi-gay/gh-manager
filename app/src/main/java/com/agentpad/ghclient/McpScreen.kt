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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** MCP 本地服务控制面板 */
@Composable
fun McpScreen() {
    val ctx = LocalContext.current
    ctxHolder = ctx
    val clipboard = LocalClipboardManager.current

    var running by remember { mutableStateOf(McpServer.isRunning) }
    var portText by remember { mutableStateOf(McpServer.port.toString()) }
    var authOn by remember { mutableStateOf(false) }
    var authKey by remember { mutableStateOf(McpServer.authToken) }
    var addrList by remember { mutableStateOf(McpServer.localAddresses()) }
    var logs by remember { mutableStateOf<List<String>>(emptyList()) }
    var reqCount by remember { mutableStateOf(McpServer.requestCount) }
    var copied by remember { mutableStateOf("") }

    fun log(s: String) {
        val t = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        logs = (logs + "[$t] $s").takeLast(60)
    }

    LaunchedEffect(Unit) {
        addrList = McpServer.localAddresses()
        while (true) {
            running = McpServer.isRunning
            reqCount = McpServer.requestCount
            delay(1200)
        }
    }

    fun startServer() {
        val p = portText.toIntOrNull() ?: 8765
        if (p < 1024 || p > 65535) {
            log("端口必须在 1024~65535 之间")
            return
        }
        McpServer.authToken = if (authOn) authKey.ifBlank { randomKey() } else ""
        if (McpServer.authToken.isNotEmpty()) authKey = McpServer.authToken
        val ok = McpServer.start(p)
        if (ok) {
            McpService.startService(ctx, p, McpServer.authToken)
            addrList = McpServer.localAddresses()
            log("服务已启动，端口 $p")
            if (McpServer.authToken.isNotEmpty()) log("鉴权已开启，token=" + McpServer.authToken)
        } else {
            log("启动失败：" + McpServer.lastError)
        }
        running = McpServer.isRunning
    }

    fun stopServer() {
        McpService.stopService(ctx)
        McpServer.stop()
        running = false
        log("服务已停止")
    }

    Column(Modifier.fillMaxSize().background(GhBg)) {
        AppBar("MCP 本地服务", "把 GitHub 权限暴露给其他应用", onBack = { Nav.pop() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {

            // ---------- 状态卡 ----------
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (running) GhGreen.copy(alpha = 0.10f) else GhSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, if (running) GhGreen.copy(alpha = 0.45f) else GhBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(13.dp))
                                .background(if (running) GhGreen.copy(alpha = 0.2f) else GhSurface2),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Dns, null,
                                tint = if (running) GhGreen else GhDim,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(8.dp).clip(CircleShape)
                                        .background(if (running) GhGreen else GhDim)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (running) "运行中" else "未启动",
                                    color = if (running) GhGreen else GhDim,
                                    fontSize = 15.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                if (running) "端口 " + McpServer.port + " · 已处理 " + reqCount + " 次请求"
                                else "点下面按钮启动本地 MCP 服务",
                                color = GhDim, fontSize = 11.sp,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                    }
                    Gap(14)
                    Row(Modifier.fillMaxWidth()) {
                        if (running) {
                            SmallBtn("停止服务", GhRed, filled = true, icon = Icons.Rounded.PowerSettingsNew) {
                                stopServer()
                            }
                        } else {
                            SmallBtn("启动服务", GhGreen, filled = true, icon = Icons.Rounded.PowerSettingsNew) {
                                startServer()
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        SmallBtn("刷新地址", GhBlue, icon = Icons.Rounded.Lan) {
                            addrList = McpServer.localAddresses()
                            log("已刷新 IP 地址")
                        }
                    }
                }
            }

            // ---------- 地址 ----------
            SectionTitle("访问地址", Icons.Rounded.Lan)
            GhCard {
                val port = if (running) McpServer.port else (portText.toIntOrNull() ?: 8765)
                KV("JSON-RPC", "http://127.0.0.1:" + port + "/mcp", mono = true)
                KV("健康检查", "http://127.0.0.1:" + port + "/health", mono = true)
                if (addrList.isEmpty()) {
                    Text("没有检测到局域网 IP（可能没连 WiFi）", color = GhDim, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp))
                } else {
                    addrList.forEach { ip ->
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("http://$ip:$port/mcp", color = GhCyan, fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                            IconBtn(Icons.Rounded.ContentCopy, GhBlue, 26.dp) {
                                clipboard.setText(AnnotatedString("http://$ip:$port/mcp"))
                                copied = ip
                                log("已复制 $ip 地址")
                            }
                        }
                    }
                }
            }

            // ---------- 配置 ----------
            SectionTitle("服务配置", Icons.Rounded.Build)
            GhCard {
                Field(portText, { portText = it }, "监听端口", mono = true, placeholder = "8765")
                Gap(10)
                Row(
                    Modifier.fillMaxWidth().clickable { authOn = !authOn },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (authOn) "☑" else "☐", color = if (authOn) GhGreen else GhDim, fontSize = 16.sp)
                    Text("  开启访问令牌校验", color = GhText, fontSize = 13.sp)
                }
                if (authOn) {
                    Gap(8)
                    Field(authKey, { authKey = it }, "访问令牌（留空自动生成）", mono = true)
                    Text(
                        "客户端需带请求头 x-mcp-token: <令牌>",
                        color = GhDim, fontSize = 10.5.sp, modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Gap(8)
                Text(
                    "已登录：${Session.login}　可用工具：" + McpTools.count() + " 个\n" +
                        "所有调用都使用本机已保存的 GitHub 令牌，权限与该令牌完全一致。",
                    color = GhDim, fontSize = 10.5.sp
                )
            }

            // ---------- 客户端配置 ----------
            SectionTitle("客户端配置（复制到 MCP 客户端）", Icons.Rounded.Terminal)
            GhCard {
                val p = if (running) McpServer.port else (portText.toIntOrNull() ?: 8765)
                val ip = addrList.firstOrNull() ?: "127.0.0.1"
                val json = "{\n" +
                    "  \"mcpServers\": {\n" +
                    "    \"gh-manager\": {\n" +
                    "      \"url\": \"http://$ip:$p/mcp\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}"
                Surface(
                    shape = RoundedCornerShape(10.dp), color = GhBg2,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Mono(json, Modifier.padding(10.dp))
                }
                Gap(8)
                SmallBtn("复制 JSON 配置", GhBlue, icon = Icons.Rounded.ContentCopy) {
                    clipboard.setText(AnnotatedString(json))
                    log("已复制客户端配置")
                }
            }

            // ---------- 工具列表 ----------
            SectionTitle("暴露的 MCP 工具（" + McpTools.count() + " 个）", Icons.Rounded.CheckCircle)
            GhCard {
                val names = mcpToolNames()
                names.forEach { n ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.5.dp)) {
                        Box(
                            Modifier.size(5.dp).clip(CircleShape).background(GhGreen)
                                .padding(top = 0.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(n, color = GhText, fontSize = 11.5.sp, fontFamily = FontFamily.Monospace,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // ---------- 日志 ----------
            SectionTitle("运行日志", Icons.Rounded.Terminal)
            Surface(
                shape = RoundedCornerShape(14.dp), color = GhBg2,
                border = androidx.compose.foundation.BorderStroke(1.dp, GhBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    if (logs.isEmpty()) {
                        Text("还没有日志喵～", color = GhDim, fontSize = 11.sp)
                    } else {
                        logs.takeLast(20).forEach { l ->
                            Text(l, color = if (l.contains("失败")) GhRed else GhDim,
                                fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 1.dp))
                        }
                    }
                }
            }
            Gap(24)
        }
    }
}

/** 从 McpTools 拿工具名列表 */
private fun mcpToolNames(): List<String> {
    val arr = McpTools.list()
    val out = ArrayList<String>(arr.length())
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        out.add(o.optString("name"))
    }
    return out
}

fun randomKey(): String {
    val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
    val sb = StringBuilder()
    val r = java.util.Random()
    repeat(24) { sb.append(chars[r.nextInt(chars.length)]) }
    return sb.toString()
}
