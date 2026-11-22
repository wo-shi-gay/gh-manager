package com.agentpad.ghclient

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// GitHub 深色配色
val GhBg = Color(0xFF0D1117)
val GhSurface = Color(0xFF161B22)
val GhSurface2 = Color(0xFF1C2128)
val GhBorder = Color(0xFF30363D)
val GhText = Color(0xFFE6EDF3)
val GhDim = Color(0xFF8B949E)
val GhGreen = Color(0xFF3FB950)
val GhBlue = Color(0xFF58A6FF)
val GhRed = Color(0xFFF85149)
val GhYellow = Color(0xFFD29922)
val GhPurple = Color(0xFFBC8CFF)

private val Colors = darkColorScheme(
    primary = GhGreen,
    onPrimary = Color(0xFF04160A),
    secondary = GhBlue,
    onSecondary = Color(0xFF00131F),
    tertiary = GhPurple,
    background = GhBg,
    onBackground = GhText,
    surface = GhSurface,
    onSurface = GhText,
    surfaceVariant = GhSurface2,
    onSurfaceVariant = GhDim,
    outline = GhBorder,
    error = GhRed,
    onError = Color(0xFF2B0000)
)

@Composable
fun GhTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}

/** 圆角小标签按钮 */
@Composable
fun Pill(text: String, active: Boolean, accent: Color = GhGreen, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (active) accent.copy(alpha = 0.18f) else GhSurface2,
        border = BorderStroke(1.dp, if (active) accent.copy(alpha = 0.55f) else GhBorder),
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (active) accent else GhDim,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun GhCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GhSurface,
        border = BorderStroke(1.dp, GhBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
fun KV(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(k, color = GhDim, fontSize = 12.sp, modifier = Modifier.width(96.dp))
        Text(v, color = GhText, fontSize = 12.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun LoadingBox(text: String = "加载中…") {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = GhGreen, modifier = Modifier.size(28.dp))
        Text(text, color = GhDim, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
fun ErrBox(msg: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("出错了喵", color = GhRed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(msg, color = GhDim, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        TextButton(onClick = onRetry) { Text("重试", color = GhBlue) }
    }
}

@Composable
fun EmptyBox(text: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text, color = GhDim, fontSize = 13.sp)
    }
}

@Composable
fun Mono(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = GhText,
        fontSize = 11.5.sp,
        fontFamily = FontFamily.Monospace,
        modifier = modifier
    )
}

@Composable
fun HeadBar(title: String, subtitle: String = "", onBack: (() -> Unit)? = null, right: @Composable (() -> Unit)? = null) {
    Surface(color = GhSurface, border = BorderStroke(0.dp, Color.Transparent)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GhSurface2,
                    modifier = Modifier.padding(end = 10.dp).clickable { onBack() }
                ) {
                    Text("‹ 返回", color = GhBlue, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = GhText, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, color = GhDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (right != null) right()
        }
    }
}

fun isoToRel(s: String): String {
    if (s.isEmpty()) return ""
    return try {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val d = fmt.parse(s) ?: return s.take(10)
        android.text.format.DateUtils.getRelativeTimeSpanString(
            d.time, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS
        ).toString()
    } catch (_: Exception) {
        s.take(10)
    }
}

fun openUrl(ctx: android.content.Context, url: String) {
    try {
        ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    } catch (_: Exception) {
    }
}
