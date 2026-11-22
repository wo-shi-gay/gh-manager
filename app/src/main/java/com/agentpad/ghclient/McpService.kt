package com.agentpad.ghclient

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder

/** 前台服务：让 MCP Server 在后台常驻，不被系统回收 */
class McpService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val port = intent?.getIntExtra("port", 8765) ?: 8765
        val token = intent?.getStringExtra("token") ?: ""
        Session.load(this)
        McpServer.authToken = token
        if (!McpServer.isRunning) {
            McpServer.start(port)
        }
        startForeground(NOTIF_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "MCP 本地服务", NotificationManager.IMPORTANCE_LOW
            )
            ch.description = "GitHub 管理器的本地 MCP 服务运行状态"
            nm.createNotificationChannel(ch)
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("MCP 服务运行中 · 端口 " + McpServer.port)
            .setContentText("http://127.0.0.1:" + McpServer.port + "/mcp　工具 " + McpTools.count() + " 个")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        McpServer.stop()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "gh_manager_mcp"
        private const val NOTIF_ID = 4211

        fun startService(ctx: Context, port: Int, token: String) {
            val i = Intent(ctx, McpService::class.java)
            i.putExtra("port", port)
            i.putExtra("token", token)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }

        fun stopService(ctx: Context) {
            ctx.stopService(Intent(ctx, McpService::class.java))
            McpServer.stop()
        }
    }
}
