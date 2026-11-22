package com.agentpad.ghclient

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ============ 配色 ============
val GhBg = Color(0xFF0A0E14)
val GhBg2 = Color(0xFF11161F)
val GhSurface = Color(0xFF161C26)
val GhSurface2 = Color(0xFF1E2633)
val GhSurface3 = Color(0xFF273140)
val GhBorder = Color(0xFF2A3544)
val GhText = Color(0xFFE9EEF5)
val GhDim = Color(0xFF8B98A9)
val GhGreen = Color(0xFF3FD07C)
val GhBlue = Color(0xFF58A6FF)
val GhRed = Color(0xFFFF6B62)
val GhYellow = Color(0xFFF0B62E)
val GhPurple = Color(0xFFC08CFF)
val GhCyan = Color(0xFF39D2C0)
val GhPink = Color(0xFFFF8AC7)

val AccentGradient = Brush.linearGradient(listOf(GhGreen, GhCyan))
val BlueGradient = Brush.linearGradient(listOf(GhBlue, GhPurple))

private val Colors = darkColorScheme(
    primary = GhGreen,
    onPrimary = Color(0xFF00210F),
    secondary = GhBlue,
    onSecondary = Color(0xFF00172B),
    tertiary = GhCyan,
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

// ============ 顶部栏 ============
@Composable
fun AppBar(
    title: String,
    subtitle: String = "",
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth().background(GhBg)) {
        Row(
            Modifier.fillMaxWidth().padding(start = if (onBack != null) 6.dp else 18.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconBtn(Icons.Rounded.ArrowBack, GhText, 20.dp) { onBack() }
                Spacer(Modifier.width(6.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = GhText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        color = GhDim,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
            if (actions != null) actions()
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(GhBorder, Color.Transparent)))
        )
    }
}

// 兼容旧名字
@Composable
fun HeadBar(title: String, subtitle: String = "", onBack: (() -> Unit)? = null, right: @Composable (() -> Unit)? = null) {
    AppBar(title, subtitle, onBack, right)
}

// ============ 按钮 ============
@Composable
fun IconBtn(icon: ImageVector, tint: Color = GhDim, boxSize: Dp = 34.dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(boxSize)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(boxSize * 0.54f))
    }
}

@Composable
fun SmallBtn(
    text: String,
    color: Color = GhGreen,
    filled: Boolean = false,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (filled) color else color.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, if (filled) Color.Transparent else color.copy(alpha = 0.42f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, tint = if (filled) Color(0xFF04150A) else color, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text,
                color = if (filled) Color(0xFF04150A) else color,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** 大号主按钮（渐变） */
@Composable
fun PrimaryBtn(text: String, icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) AccentGradient else Brush.linearGradient(listOf(GhSurface3, GhSurface3)))
            .clickable(enabled = enabled) { onClick() }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, tint = Color(0xFF04150A), modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
            }
            Text(text, color = Color(0xFF04150A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ============ 胶囊 / 标签 ============
@Composable
fun Pill(text: String, active: Boolean, accent: Color = GhGreen, icon: ImageVector? = null, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (active) accent.copy(alpha = 0.17f) else GhSurface2,
        border = BorderStroke(1.dp, if (active) accent.copy(alpha = 0.55f) else GhBorder),
        modifier = Modifier
            .padding(end = 7.dp)
            .clickable { onClick() }
    ) {
        Row(
            Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, tint = if (active) accent else GhDim, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text,
                color = if (active) accent else GhDim,
                fontSize = 12.5.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun StateChip(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.16f)) {
        Text(
            text,
            color = color,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TagsRow(icon: ImageVector?, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(text, color = color, fontSize = 10.5.sp)
    }
}

// ============ 卡片 ============
@Composable
fun GhCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GhSurface,
        border = BorderStroke(1.dp, GhBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(15.dp), content = content)
    }
}

@Composable
fun ClickCard(onClick: () -> Unit, accent: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GhSurface,
        border = BorderStroke(1.dp, accent?.copy(alpha = 0.35f) ?: GhBorder),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onClick() }
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
fun KV(k: String, v: String, mono: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.5.dp)) {
        Text(k, color = GhDim, fontSize = 11.5.sp, modifier = Modifier.width(88.dp))
        Text(
            v,
            color = GhText,
            fontSize = 11.5.sp,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatTile(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
        Text(value, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(label, color = GhDim, fontSize = 9.5.sp, modifier = Modifier.padding(top = 1.dp))
    }
}

// ============ 输入框 ============
@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    single: Boolean = true,
    minLines: Int = 1,
    placeholder: String = "",
    mono: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.5.sp) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, fontSize = 12.sp) }
        } else null,
        singleLine = single,
        minLines = minLines,
        textStyle = TextStyle(
            fontSize = 13.sp,
            color = GhText,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default
        ),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GhGreen,
            unfocusedBorderColor = GhBorder,
            focusedLabelColor = GhGreen,
            unfocusedLabelColor = GhDim,
            cursorColor = GhGreen,
            focusedContainerColor = GhSurface,
            unfocusedContainerColor = GhSurface
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "搜索…",
    onSubmit: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(GhSurface2)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = GhDim, modifier = Modifier.size(17.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 12.5.sp, color = GhDim) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp, color = GhText),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = GhGreen
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

// ============ 状态页 ============
@Composable
fun LoadingBox(text: String = "加载中…") {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = GhGreen, modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
        Text(text, color = GhDim, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp))
    }
}

@Composable
fun ErrBox(msg: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(GhRed.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Text("!", color = GhRed, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Text("出错了喵", color = GhText, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
        Text(
            msg,
            color = GhDim, fontSize = 11.5.sp,
            modifier = Modifier.padding(top = 7.dp),
            maxLines = 5,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(onClick = onRetry) { Text("重试", color = GhBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
fun EmptyBox(text: String, emoji: String = "🐾") {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 32.sp)
        Text(text, color = GhDim, fontSize = 12.5.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
fun SectionTitle(text: String, icon: ImageVector? = null) {
    Row(
        Modifier.padding(start = 3.dp, top = 16.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = GhGreen, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            color = GhText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
fun RowDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(GhBorder))
}

@Composable
fun Mono(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = GhText,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        modifier = modifier
    )
}

@Composable
fun Gap(h: Int = 8) {
    Spacer(Modifier.height(h.dp))
}

// ============ 工具函数 ============
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

fun shortTime(s: String): String = if (s.length >= 16) s.substring(5, 16).replace("T", " ") else s.take(10)

fun openUrl(ctx: android.content.Context, url: String) {
    try {
        ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    } catch (_: Exception) {
    }
}
