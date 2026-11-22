package com.agentpad.ghclient

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** 令牌与用户信息的本地存储 */
object Session {
    var token: String = ""
    var login: String = ""
    var avatar: String = ""

    private const val P = "gh_manager"

    fun load(c: Context) {
        val sp = c.getSharedPreferences(P, Context.MODE_PRIVATE)
        token = sp.getString("token", "") ?: ""
        login = sp.getString("login", "") ?: ""
        avatar = sp.getString("avatar", "") ?: ""
    }

    fun save(c: Context) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit()
            .putString("token", token)
            .putString("login", login)
            .putString("avatar", avatar)
            .apply()
    }

    fun clear(c: Context) {
        token = ""
        login = ""
        avatar = ""
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().clear().apply()
    }

    val signedIn: Boolean get() = token.isNotBlank()
}

class ApiError(val code: Int, val body: String) : IOException("HTTP $code")

/** GitHub REST API 客户端（OkHttp + org.json，零代码生成） */
object Gh {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    var lastScopes: String = ""
    var lastRemaining: String = ""
    var lastLimit: String = ""

    private fun reqSync(method: String, path: String, body: String?, accept: String): String {
        val url = if (path.startsWith("http")) path else "https://api.github.com$path"
        val rb = Request.Builder().url(url)
            .header("Accept", accept)
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "GhManager-Android/1.0")
        if (Session.token.isNotEmpty()) {
            rb.header("Authorization", "Bearer " + Session.token)
        }
        when (method) {
            "GET" -> rb.get()
            "DELETE" -> rb.delete()
            else -> rb.method(method, (body ?: "{}").toRequestBody(jsonMedia))
        }
        client.newCall(rb.build()).execute().use { r ->
            lastScopes = r.header("x-oauth-scopes") ?: lastScopes
            lastRemaining = r.header("x-ratelimit-remaining") ?: lastRemaining
            lastLimit = r.header("x-ratelimit-limit") ?: lastLimit
            val text = r.body?.string() ?: ""
            if (!r.isSuccessful) throw ApiError(r.code, text)
            return text
        }
    }

    suspend fun apiGet(path: String): String =
        withContext(Dispatchers.IO) { reqSync("GET", path, null, "application/vnd.github+json") }

    suspend fun apiGetRaw(path: String): String =
        withContext(Dispatchers.IO) { reqSync("GET", path, null, "application/vnd.github.raw") }

    suspend fun apiSend(method: String, path: String, body: JSONObject? = null): String =
        withContext(Dispatchers.IO) { reqSync(method, path, body?.toString(), "application/vnd.github+json") }

    suspend fun apiDelete(path: String): String =
        withContext(Dispatchers.IO) { reqSync("DELETE", path, null, "application/vnd.github+json") }
}

/** 把异常翻译成人类可读的错误 */
fun errText(e: Throwable): String {
    if (e is ApiError) {
        try {
            val o = JSONObject(e.body)
            val m = o.optString("message", "")
            val errs = o.optJSONArray("errors")
            var detail = ""
            if (errs != null && errs.length() > 0) {
                val first = errs.optJSONObject(0)
                if (first != null) detail = first.optString("message", "")
            }
            val base = "HTTP ${e.code}" + if (m.isNotEmpty()) " · $m" else ""
            return if (detail.isNotEmpty()) "$base（$detail）" else base
        } catch (_: Exception) {
            return "HTTP ${e.code}"
        }
    }
    return e.message ?: e.javaClass.simpleName
}

// ---------- JSON 便捷读取 ----------

fun JSONObject.str(k: String): String = if (isNull(k)) "" else optString(k, "")

fun JSONObject.num(k: String): Int = optInt(k, 0)

fun JSONObject.longNum(k: String): Long = optLong(k, 0L)

fun JSONObject.flag(k: String): Boolean = optBoolean(k, false)

fun JSONObject.obj(k: String): JSONObject = optJSONObject(k) ?: JSONObject()

fun JSONObject.arr(k: String): JSONArray = optJSONArray(k) ?: JSONArray()

fun JSONArray.toObjList(): List<JSONObject> {
    val out = ArrayList<JSONObject>(length())
    for (i in 0 until length()) {
        val o = optJSONObject(i)
        if (o != null) out.add(o)
    }
    return out
}

fun base64Decode(s: String): String {
    return try {
        val clean = s.replace("\n", "").replace("\r", "")
        String(android.util.Base64.decode(clean, android.util.Base64.DEFAULT), Charsets.UTF_8)
    } catch (_: Exception) {
        "（无法解码）"
    }
}
