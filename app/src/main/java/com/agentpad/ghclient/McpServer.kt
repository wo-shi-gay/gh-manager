package com.agentpad.ghclient

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * 极简 MCP Server（HTTP 传输）
 *
 * 支持：
 *  - POST /mcp           JSON-RPC 2.0（initialize / tools/list / tools/call / ping）
 *  - GET  /mcp           SSE 握手（老式 SSE 客户端兼容），返回 endpoint 事件
 *  - GET  /health        健康检查
 *  - GET  /              简单说明页
 *  - OPTIONS *           CORS 预检
 *
 * 无第三方依赖：直接用 ServerSocket 手写 HTTP/1.1。
 */
object McpServer {
    const val PROTOCOL_VERSION = "2024-11-05"
    const val SERVER_NAME = "gh-manager-mcp"
    const val SERVER_VERSION = "2.0.0"

    private var serverSocket: ServerSocket? = null
    private val running = AtomicBoolean(false)

    var port: Int = 8765
        private set
    var startedAt: Long = 0
        private set
    var requestCount: Int = 0
        private set
    var lastError: String = ""
        private set

    /** 非空时要求请求头 x-mcp-token / Authorization: Bearer 匹配 */
    var authToken: String = ""

    val isRunning: Boolean get() = running.get()

    fun start(p: Int): Boolean {
        if (running.get()) return true
        if (Session.token.isBlank()) {
            lastError = "还没登录 GitHub，没有令牌可用"
            return false
        }
        return try {
            val ss = ServerSocket(p)
            serverSocket = ss
            port = p
            running.set(true)
            startedAt = System.currentTimeMillis()
            lastError = ""
            requestCount = 0
            thread(name = "mcp-accept", isDaemon = true) {
                while (running.get()) {
                    try {
                        val client = ss.accept()
                        thread(name = "mcp-conn", isDaemon = true) { handle(client) }
                    } catch (e: Exception) {
                        if (running.get()) lastError = e.message ?: "accept 失败"
                    }
                }
            }
            true
        } catch (e: Exception) {
            lastError = e.message ?: "端口无法监听"
            running.set(false)
            false
        }
    }

    fun stop() {
        running.set(false)
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
    }

    /** 本机可访问的 IPv4 地址列表 */
    fun localAddresses(): List<String> {
        val out = ArrayList<String>()
        try {
            val ifaces = NetworkInterface.getNetworkInterfaces()
            while (ifaces != null && ifaces.hasMoreElements()) {
                val iface = ifaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val a = addrs.nextElement()
                    if (a is Inet4Address && !a.isLoopbackAddress) {
                        out.add(a.hostAddress ?: "")
                    }
                }
            }
        } catch (_: Exception) {
        }
        return out.filter { it.isNotEmpty() }
    }

    // ---------------- HTTP ----------------

    private fun handle(client: Socket) {
        try {
            client.soTimeout = 20000
            val input = client.getInputStream()
            val output = BufferedOutputStream(client.getOutputStream())

            val requestLine = readLine(input) ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 3) return
            val method = parts[0].uppercase()
            var path = parts[1]
            val qIdx = path.indexOf('?')
            val query = if (qIdx >= 0) path.substring(qIdx + 1) else ""
            if (qIdx >= 0) path = path.substring(0, qIdx)

            val headers = HashMap<String, String>()
            while (true) {
                val line = readLine(input) ?: break
                if (line.isEmpty()) break
                val i = line.indexOf(':')
                if (i > 0) {
                    headers[line.substring(0, i).trim().lowercase()] = line.substring(i + 1).trim()
                }
            }

            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            val body = if (contentLength > 0) readBody(input, contentLength) else ""

            requestCount++

            // CORS 预检
            if (method == "OPTIONS") {
                writeResp(output, 204, "text/plain", "", cors = true)
                return
            }

            if (!authOk(headers, query)) {
                writeResp(
                    output, 401, "application/json",
                    JSONObject().put("error", "unauthorized").put("message", "需要在请求头带 x-mcp-token").toString(),
                    cors = true
                )
                return
            }

            when {
                path == "/" && method == "GET" -> {
                    writeResp(output, 200, "text/html; charset=utf-8", helpHtml(), cors = true)
                }

                path == "/health" -> {
                    val o = JSONObject()
                        .put("ok", true)
                        .put("server", SERVER_NAME)
                        .put("version", SERVER_VERSION)
                        .put("protocolVersion", PROTOCOL_VERSION)
                        .put("githubUser", Session.login)
                        .put("requests", requestCount)
                        .put("uptimeSeconds", if (startedAt > 0) (System.currentTimeMillis() - startedAt) / 1000 else 0)
                        .put("tools", McpTools.count())
                    writeResp(output, 200, "application/json", o.toString(), cors = true)
                }

                path == "/mcp" && method == "POST" -> {
                    if (body.isBlank()) {
                        writeResp(output, 400, "application/json", rpcError(null, -32600, "空请求体").toString(), cors = true)
                        return
                    }
                    val arr = try {
                        org.json.JSONArray(body)
                    } catch (_: Exception) {
                        null
                    }
                    val resp = if (arr != null) {
                        // 批量请求
                        val res = JSONArray()
                        for (i in 0 until arr.length()) {
                            val item = arr.optJSONObject(i) ?: continue
                            val r = handleRpc(item)
                            if (r != null) res.put(r)
                        }
                        if (res.length() == 0) null else res.toString()
                    } else {
                        val item = try {
                            JSONObject(body)
                        } catch (_: Exception) {
                            writeResp(
                                output, 400, "application/json",
                                rpcError(null, -32700, "JSON 解析失败").toString(), cors = true
                            )
                            return
                        }
                        handleRpc(item)?.toString()
                    }
                    if (resp == null) {
                        writeResp(output, 202, "text/plain", "", cors = true)
                    } else {
                        writeResp(output, 200, "application/json", resp, cors = true)
                    }
                }

                path == "/mcp" && method == "GET" -> {
                    sseHandshake(output)
                }

                else -> {
                    writeResp(
                        output, 404, "application/json",
                        JSONObject().put("error", "not_found").put("path", path).toString(),
                        cors = true
                    )
                }
            }
            output.flush()
        } catch (e: Exception) {
            lastError = e.message ?: e.javaClass.simpleName
        } finally {
            try {
                client.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun authOk(headers: Map<String, String>, query: String): Boolean {
        if (authToken.isBlank()) return true
        if (headers["x-mcp-token"] == authToken) return true
        val a = headers["authorization"] ?: ""
        if (a == "Bearer $authToken") return true
        return query.contains("token=" + authToken)
    }

    private fun sseHandshake(output: BufferedOutputStream) {
        val head = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: text/event-stream; charset=utf-8\r\n")
            append("Cache-Control: no-cache\r\n")
            append("Connection: keep-alive\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Access-Control-Allow-Headers: *\r\n")
            append("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n")
            append("\r\n")
        }
        output.write(head.toByteArray())
        output.write("event: endpoint\ndata: /mcp\n\n".toByteArray())
        output.flush()
        // 保持一小会儿，供客户端确认，随后关闭（客户端后续应改用 POST /mcp）
        try {
            Thread.sleep(1500)
            output.write(": keepalive\n\n".toByteArray())
            output.flush()
        } catch (_: Exception) {
        }
    }

    // ---------------- JSON-RPC ----------------

    private fun handleRpc(req: JSONObject): JSONObject? {
        val id = if (req.has("id") && !req.isNull("id")) req.get("id") else null
        val method = req.optString("method", "")
        val params = req.optJSONObject("params") ?: JSONObject()

        // 通知类消息没有 id → 不返回
        if (id == null && method.startsWith("notifications/")) {
            return null
        }

        return try {
            when (method) {
                "initialize" -> rpcResult(id, initializeResult())
                "ping" -> rpcResult(id, JSONObject())
                "tools/list" -> rpcResult(id, JSONObject().put("tools", McpTools.list()))
                "tools/call" -> {
                    val name = params.optString("name", "")
                    val args = params.optJSONObject("arguments") ?: JSONObject()
                    if (name.isBlank()) {
                        rpcError(id, -32602, "缺少 tools/call 的 name")
                    } else {
                        try {
                            val text = McpTools.call(name, args)
                            rpcResult(
                                id, JSONObject()
                                    .put("content", JSONArray().put(JSONObject().put("type", "text").put("text", text)))
                                    .put("isError", false)
                            )
                        } catch (e: Exception) {
                            rpcResult(
                                id, JSONObject()
                                    .put(
                                        "content",
                                        JSONArray().put(
                                            JSONObject().put("type", "text").put("text", "调用失败：" + errText(e))
                                        )
                                    )
                                    .put("isError", true)
                            )
                        }
                    }
                }
                "resources/list" -> rpcResult(id, JSONObject().put("resources", JSONArray()))
                "prompts/list" -> rpcResult(id, JSONObject().put("prompts", JSONArray()))
                "notifications/initialized" -> null
                "initialized" -> null
                "" -> rpcError(id, -32600, "缺少 method")
                else -> rpcError(id, -32601, "未知方法：$method")
            }
        } catch (e: Exception) {
            rpcError(id, -32603, "内部错误：" + (e.message ?: "unknown"))
        }
    }

    private fun initializeResult(): JSONObject {
        val caps = JSONObject().put("tools", JSONObject().put("listChanged", false))
        val info = JSONObject()
            .put("name", SERVER_NAME)
            .put("version", SERVER_VERSION)
        return JSONObject()
            .put("protocolVersion", PROTOCOL_VERSION)
            .put("capabilities", caps)
            .put("serverInfo", info)
            .put(
                "instructions",
                "这是 GitHub 管理器的本地 MCP 服务。用 tools/list 获取工具，用 tools/call 调用。" +
                    "所有调用都使用 App 内已登录的 GitHub 令牌，权限与你令牌一致。"
            )
    }

    private fun rpcResult(id: Any?, result: JSONObject): JSONObject =
        JSONObject().put("jsonrpc", "2.0").put("id", id ?: JSONObject.NULL).put("result", result)

    private fun rpcError(id: Any?, code: Int, message: String): JSONObject =
        JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", id ?: JSONObject.NULL)
            .put("error", JSONObject().put("code", code).put("message", message))

    // ---------------- Socket 工具 ----------------

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        var b = input.read()
        if (b == -1) return null
        while (b != -1 && b != '\n'.code) {
            if (b != '\r'.code) sb.append(b.toChar())
            b = input.read()
        }
        return sb.toString()
    }

    private fun readBody(input: InputStream, len: Int): String {
        val buf = ByteArray(len)
        var off = 0
        while (off < len) {
            val n = input.read(buf, off, len - off)
            if (n <= 0) break
            off += n
        }
        return String(buf, 0, off, Charsets.UTF_8)
    }

    private fun writeResp(
        output: BufferedOutputStream,
        code: Int,
        contentType: String,
        body: String,
        cors: Boolean = false
    ) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val head = buildString {
            append("HTTP/1.1 $code ${statusText(code)}\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Connection: close\r\n")
            if (cors) {
                append("Access-Control-Allow-Origin: *\r\n")
                append("Access-Control-Allow-Headers: *\r\n")
                append("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n")
            }
            append("\r\n")
        }
        output.write(head.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun statusText(code: Int): String = when (code) {
        200 -> "OK"
        202 -> "Accepted"
        204 -> "No Content"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        404 -> "Not Found"
        500 -> "Internal Server Error"
        else -> "OK"
    }

    private fun helpHtml(): String = """
        <!doctype html><html><head><meta charset="utf-8">
        <title>GitHub 管理器 · MCP Server</title>
        <style>body{background:#0A0E14;color:#E9EEF5;font-family:system-ui;padding:24px;line-height:1.7}
        code{background:#1E2633;padding:2px 6px;border-radius:6px;color:#3FD07C}
        h1{font-size:20px}a{color:#58A6FF}</style></head><body>
        <h1>🐾 GitHub 管理器 · MCP Server</h1>
        <p>正在运行，协议版本 <code>${PROTOCOL_VERSION}</code></p>
        <p>JSON-RPC 端点：<code>POST /mcp</code>　健康检查：<code>GET /health</code></p>
        <p>已登录账号：<code>${Session.login}</code>　可用工具：<code>${McpTools.count()}</code> 个</p>
        <p>客户端配置示例（SSE / HTTP）：</p>
        <pre><code>{"mcpServers":{"gh-manager":{"url":"http://手机IP:$port/mcp"}}}</code></pre>
        </body></html>
    """.trimIndent()
}
