package com.agentpad.ghclient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Comment
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.json.JSONObject

/** 通用带头像的卡片容器 */
@Composable
fun Avatar(url: String, size: Dp = 36.dp, ring: Color? = null) {
    Box(
        Modifier
            .size(size + if (ring != null) 4.dp else 0.dp)
            .clip(CircleShape)
            .background(ring ?: Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (url.isEmpty()) {
            Box(
                Modifier.size(size).clip(CircleShape).background(GhSurface3),
                contentAlignment = Alignment.Center
            ) { Text("?", color = GhDim, fontSize = 12.sp) }
        } else {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape)
            )
        }
    }
}

fun runColor(state: String): Color = when (state) {
    "success" -> GhGreen
    "failure", "timed_out", "startup_failure", "action_required" -> GhRed
    "in_progress", "running" -> GhYellow
    "queued", "waiting", "requested", "pending" -> GhBlue
    "cancelled", "skipped", "neutral" -> GhDim
    else -> GhDim
}

/** 仓库卡片 */
@Composable
fun RepoRow(o: JSONObject, onClick: () -> Unit) {
    val priv = o.flag("private")
    ClickCard(onClick, accent = if (priv) GhYellow else GhBlue) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (priv) GhYellow.copy(alpha = 0.14f) else GhBlue.copy(alpha = 0.14f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (priv) Icons.Rounded.Lock else Icons.Rounded.Public,
                    null,
                    tint = if (priv) GhYellow else GhBlue,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    o.str("full_name"),
                    color = GhText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val desc = o.str("description")
                Text(
                    if (desc.isEmpty()) "（无描述）" else desc,
                    color = GhDim,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
                Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    val lang = o.str("language")
                    if (lang.isNotEmpty()) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(GhPurple))
                        Spacer(Modifier.width(4.dp))
                        Text(lang, color = GhPurple, fontSize = 10.sp)
                        Spacer(Modifier.width(9.dp))
                    }
                    Icon(Icons.Rounded.Star, null, tint = GhYellow, modifier = Modifier.size(10.dp))
                    Text(" ${o.num("stargazers_count")}", color = GhDim, fontSize = 10.sp)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Rounded.Folder, null, tint = GhDim, modifier = Modifier.size(10.dp))
                    Text(" ${o.num("forks_count")}", color = GhDim, fontSize = 10.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(isoToRel(o.str("updated_at")), color = GhDim, fontSize = 10.sp)
                }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = GhDim, modifier = Modifier.size(18.dp))
        }
    }
}

/** Issue / PR 行 */
@Composable
fun IssueRow(o: JSONObject, isPr: Boolean, onClick: () -> Unit) {
    val merged = o.has("merged_at") && !o.isNull("merged_at")
    val rawState = o.str("state")
    val open = rawState == "open"
    val color = when {
        merged -> GhPurple
        open -> GhGreen
        else -> GhRed
    }
    val label = when {
        merged -> "已合并"
        open -> "开启"
        else -> "已关闭"
    }
    ClickCard(onClick, accent = color) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isPr) "PR" else "#",
                    color = color, fontSize = 10.5.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StateChip(label, color)
                    Text(
                        "  #${o.num("number")}",
                        color = GhDim, fontSize = 10.5.sp
                    )
                }
                Text(
                    o.str("title"),
                    color = GhText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(o.obj("user").str("avatar_url"), 14.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${o.obj("user").str("login")} · ${isoToRel(o.str("updated_at"))}",
                        color = GhDim, fontSize = 10.sp
                    )
                    if (o.num("comments") > 0) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.Comment, null, tint = GhDim, modifier = Modifier.size(10.dp))
                        Text(" ${o.num("comments")}", color = GhDim, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

/** 提交行 */
@Composable
fun CommitRow(o: JSONObject, onClick: () -> Unit) {
    val commit = o.obj("commit")
    val author = commit.obj("author")
    ClickCard(onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(GhCyan.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Bolt, null, tint = GhCyan, modifier = Modifier.size(15.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    commit.str("message").lineSequence().firstOrNull() ?: "",
                    color = GhText,
                    fontSize = 12.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${o.str("sha").take(7)} · ${author.str("name")} · ${isoToRel(author.str("date"))}",
                    color = GhDim, fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = GhDim, modifier = Modifier.size(16.dp))
        }
    }
}

/** Actions 运行行 */
@Composable
fun RunRow(o: JSONObject, onClick: () -> Unit) {
    val status = o.str("status")
    val conclusion = o.str("conclusion")
    val state = if (status == "completed") conclusion else status
    val c = runColor(state)
    val label = when (state) {
        "success" -> "成功"
        "failure" -> "失败"
        "in_progress" -> "运行中"
        "queued" -> "排队"
        "cancelled" -> "已取消"
        "timed_out" -> "超时"
        "skipped" -> "跳过"
        else -> state.ifEmpty { "未知" }
    }
    ClickCard(onClick, accent = c) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(c.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (state) {
                        "success" -> "✓"
                        "failure" -> "✕"
                        "in_progress" -> "●"
                        else -> "○"
                    },
                    color = c, fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StateChip(label, c)
                    Text("  #${o.num("run_number")}", color = GhDim, fontSize = 10.5.sp)
                }
                Text(
                    o.str("display_title").ifEmpty { o.str("name") },
                    color = GhText, fontSize = 12.sp, maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 5.dp)
                )
                Text(
                    "${o.str("head_branch")} · ${isoToRel(o.str("created_at"))}",
                    color = GhDim, fontSize = 10.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

/** 通知行 */
@Composable
fun NotifRow(o: JSONObject, onClick: () -> Unit) {
    val unread = o.flag("unread")
    ClickCard(onClick, accent = if (unread) GhGreen else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
                    .background(if (unread) GhGreen.copy(alpha = 0.14f) else GhSurface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Notifications, null, tint = if (unread) GhGreen else GhDim, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StateChip(o.str("reason").ifEmpty { "通知" }, if (unread) GhGreen else GhDim)
                    Text(
                        "  " + o.obj("subject").str("type").replace("PullRequest", "PR"),
                        color = GhDim, fontSize = 10.sp
                    )
                }
                Text(
                    o.obj("subject").str("title"),
                    color = GhText, fontSize = 12.5.sp, maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 5.dp)
                )
                Text(
                    "${o.obj("repository").str("full_name")} · ${isoToRel(o.str("updated_at"))}",
                    color = GhDim, fontSize = 10.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            if (unread) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(GhGreen))
            }
        }
    }
}

/** 列表里的功能入口 */
@Composable
fun ActionRow(
    title: String,
    subtitle: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color = GhGreen,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = GhSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GhBorder),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onClick() }
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = GhText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, color = GhDim, fontSize = 10.5.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = GhDim, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
fun TextBtnRow(text: String, color: Color = GhBlue, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
