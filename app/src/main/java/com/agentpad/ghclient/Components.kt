package com.agentpad.ghclient

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.json.JSONObject

@Composable
fun Avatar(url: String, size: Dp = 34.dp) {
    if (url.isEmpty()) {
        Box(
            Modifier.size(size).clip(CircleShape).background(GhSurface2),
            contentAlignment = Alignment.Center
        ) { Text("?", color = GhDim, fontSize = 12.sp) }
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.size(size).clip(CircleShape)
        )
    }
}

@Composable
fun SmallBtn(
    text: String,
    color: Color = GhGreen,
    filled: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (filled) color else color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (filled) Color(0xFF04160A) else color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun StateChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

fun runColor(state: String): Color = when (state) {
    "success" -> GhGreen
    "failure", "timed_out", "startup_failure" -> GhRed
    "in_progress", "running" -> GhYellow
    "queued", "waiting", "requested", "pending" -> GhBlue
    "cancelled", "skipped" -> GhDim
    else -> GhDim
}

@Composable
fun RowDivider() {
    HorizontalDivider(color = GhBorder, thickness = 1.dp)
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        color = GhDim,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
fun ClickCard(onClick: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = GhSurface,
        border = BorderStroke(1.dp, GhBorder),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.padding(13.dp), content = content)
    }
}

@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    single: Boolean = true,
    minLines: Int = 1,
    placeholder: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp, color = GhDim) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, fontSize = 12.sp, color = GhDim.copy(alpha = 0.6f)) }
        } else null,
        singleLine = single,
        minLines = minLines,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = GhText),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

/** 仓库卡片 */
@Composable
fun RepoRow(o: JSONObject, onClick: () -> Unit) {
    ClickCard(onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(o.obj("owner").str("avatar_url"), 30.dp)
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        o.str("full_name"),
                        color = GhBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (o.flag("private")) {
                        Text("  私有", color = GhYellow, fontSize = 10.sp)
                    }
                }
                val desc = o.str("description")
                if (desc.isNotEmpty()) {
                    Text(
                        desc,
                        color = GhDim,
                        fontSize = 11.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val lang = o.str("language")
                    if (lang.isNotEmpty()) {
                        Text("● $lang", color = GhPurple, fontSize = 10.5.sp)
                        Text("   ", fontSize = 10.5.sp)
                    }
                    Text("★ ${o.num("stargazers_count")}", color = GhDim, fontSize = 10.5.sp)
                    Text("   ⑂ ${o.num("forks_count")}", color = GhDim, fontSize = 10.5.sp)
                    Text("   ${isoToRel(o.str("updated_at"))}", color = GhDim, fontSize = 10.5.sp)
                }
            }
        }
    }
}

/** Issue / PR 行 */
@Composable
fun IssueRow(o: JSONObject, isPr: Boolean, onClick: () -> Unit) {
    val merged = o.has("merged_at") && !o.isNull("merged_at")
    val state = if (merged) "merged" else o.str("state")
    val color = when (state) {
        "open" -> GhGreen
        "merged" -> GhPurple
        else -> GhRed
    }
    ClickCard(onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Avatar(o.obj("user").str("avatar_url"), 28.dp)
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "#${o.num("number")}",
                        color = GhDim,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    StateChip(if (isPr) (if (state == "merged") "已合并" else if (state == "open") "开启" else "已关闭") else (if (state == "open") "打开" else "已关闭"), color)
                }
                Text(
                    o.str("title"),
                    color = GhText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 5.dp)
                )
                Text(
                    "${o.obj("user").str("login")} · ${isoToRel(o.str("updated_at"))}" +
                        if (o.num("comments") > 0) " · 💬 ${o.num("comments")}" else "",
                    color = GhDim,
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** 提交行 */
@Composable
fun CommitRow(o: JSONObject, onClick: () -> Unit) {
    ClickCard(onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val msg = o.obj("commit").str("message").lineSequence().firstOrNull() ?: ""
                Text(
                    msg,
                    color = GhText,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val author = o.obj("commit").obj("author").str("name")
                Text(
                    "${o.str("sha").take(7)} · $author · ${isoToRel(o.obj("commit").obj("author").str("date"))}",
                    color = GhDim,
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** Actions 运行行 */
@Composable
fun RunRow(o: JSONObject, onClick: () -> Unit) {
    val status = o.str("status")
    val conclusion = o.str("conclusion")
    val c = runColor(if (status == "completed") conclusion else status)
    ClickCard(onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StateChip(
                        when (if (status == "completed") conclusion else status) {
                            "success" -> "成功"
                            "failure" -> "失败"
                            "in_progress" -> "运行中"
                            "queued" -> "排队"
                            "cancelled" -> "已取消"
                            else -> conclusion.ifEmpty { status }
                        },
                        c
                    )
                    Text(
                        "  #${o.num("run_number")}",
                        color = GhDim,
                        fontSize = 11.sp
                    )
                }
                Text(
                    o.str("display_title").ifEmpty { o.str("name") },
                    color = GhText,
                    fontSize = 12.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 5.dp)
                )
                Text(
                    "${o.str("name")} · ${o.str("head_branch")} · ${isoToRel(o.str("created_at"))}",
                    color = GhDim,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

/** 通知行 */
@Composable
fun NotifRow(o: JSONObject, onClick: () -> Unit) {
    ClickCard(onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StateChip(o.str("reason").ifEmpty { "通知" }, GhBlue)
                    Text(
                        "  " + o.obj("subject").str("type").replace("PullRequest", "PR"),
                        color = GhDim,
                        fontSize = 10.5.sp
                    )
                }
                Text(
                    o.obj("subject").str("title"),
                    color = GhText,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 5.dp)
                )
                Text(
                    "${o.obj("repository").str("full_name")} · ${isoToRel(o.str("updated_at"))}",
                    color = GhDim,
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            if (o.flag("unread")) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(GhGreen))
            }
        }
    }
}

@Composable
fun ActionRow(text: String, color: Color = GhText, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = GhSurface,
        border = BorderStroke(1.dp, GhBorder),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = color, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("›", color = GhDim, fontSize = 16.sp)
        }
    }
}

@Composable
fun TextBtnRow(text: String, color: Color = GhBlue, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Gap(h: Int = 8) {
    androidx.compose.foundation.layout.Spacer(Modifier.size(h.dp))
}
