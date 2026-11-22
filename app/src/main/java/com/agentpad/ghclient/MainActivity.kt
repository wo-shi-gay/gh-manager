package com.agentpad.ghclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.json.JSONObject

/** 全局导航状态 */
object Nav {
    var stack by mutableStateOf(listOf<String>())
    fun push(route: String) {
        stack = stack + route
    }
    fun pop() {
        if (stack.isNotEmpty()) stack = stack.dropLast(1)
    }
    fun reset() {
        stack = emptyList()
    }
    val current: String? get() = stack.lastOrNull()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ctxHolder = applicationContext
        Session.load(this)
        setContent {
            GhTheme {
                Surface(Modifier.fillMaxSize().background(GhBg)) {
                    Root()
                }
            }
        }
    }
}

@Composable
fun Root() {
    val ctx = LocalContext.current
    var signedIn by remember { mutableStateOf(Session.signedIn) }

    if (!signedIn) {
        LoginScreen(onDone = {
            Session.save(ctx)
            signedIn = true
            Nav.reset()
        })
        return
    }

    Box(Modifier.fillMaxSize()) {
        val route = Nav.current
        when {
            route == null -> HomeScreen(onLogout = {
                Session.clear(ctx)
                signedIn = false
                Nav.reset()
            })
            route.startsWith("repo:") -> RepoDetailScreen(route.removePrefix("repo:"))
            route.startsWith("issue:") -> {
                val p = route.removePrefix("issue:").split("|")
                if (p.size >= 3) IssueDetailScreen(p[0], p[1].toIntOrNull() ?: 0, p[2] == "pr")
                else Nav.pop()
            }
            route.startsWith("run:") -> {
                val p = route.removePrefix("run:").split("|")
                if (p.size >= 2) RunDetailScreen(p[0], p[1].toLongOrNull() ?: 0L)
                else Nav.pop()
            }
            route.startsWith("create:") -> CreateRepoScreen()
            route.startsWith("gists") -> GistsScreen()
            route.startsWith("search") -> SearchScreen()
            route.startsWith("newissue:") -> {
                val p = route.removePrefix("newissue:").split("|")
                if (p.size >= 2) NewIssueScreen(p[0], p[1])
                else Nav.pop()
            }
            else -> HomeScreen(onLogout = {
                Session.clear(ctx)
                signedIn = false
                Nav.reset()
            })
        }
    }
}

/** 供各屏幕共用的简单仓库信息缓存键 */
fun repoKey(o: JSONObject): String = o.str("full_name")

fun splitRepo(fullName: String): Pair<String, String> {
    val i = fullName.indexOf('/')
    return if (i <= 0) Pair(fullName, "") else Pair(fullName.substring(0, i), fullName.substring(i + 1))
}
