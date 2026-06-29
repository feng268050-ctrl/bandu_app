package com.bandu.tiji.transfer.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.bandu.tiji.core.model.transfer.TransferProgress

class TransferForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        return when (intent?.action) {
            ACTION_START_TRANSFER -> {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(
                        TransferNotificationContent.inProgress(
                            percentComplete = intent.getIntExtra(EXTRA_PERCENT, 0),
                            bytesText = intent.getStringExtra(EXTRA_BYTES).orEmpty(),
                        ),
                    ),
                )
                START_NOT_STICKY
            }
            ACTION_CANCEL_TRANSFER -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
                START_NOT_STICKY
            }
            else -> START_NOT_STICKY
        }
    }

    private fun buildNotification(content: TransferNotificationContent): Notification {
        ensureChannel()
        val cancelIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setOngoing(content.ongoing)
            .setOnlyAlertOnce(true)
            .setProgress(100, content.percentComplete.coerceIn(0, 100), content.indeterminate)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消", cancelIntent)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "设备迁移",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "显示伴读题集设备迁移进度"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START_TRANSFER = "com.bandu.tiji.transfer.runtime.START_TRANSFER"
        const val ACTION_CANCEL_TRANSFER = "com.bandu.tiji.transfer.runtime.CANCEL_TRANSFER"
        const val EXTRA_PERCENT = "percent"
        const val EXTRA_BYTES = "bytes"
        const val CHANNEL_ID = "bandu_tiji_transfer"
        const val NOTIFICATION_ID = 41_241

        fun startIntent(
            context: Context,
            progress: TransferProgress,
            bytesText: String,
        ): Intent =
            Intent(context, TransferForegroundService::class.java).apply {
                action = ACTION_START_TRANSFER
                putExtra(EXTRA_PERCENT, progress.percentComplete)
                putExtra(EXTRA_BYTES, bytesText)
            }

        fun cancelIntent(context: Context): Intent =
            Intent(context, TransferForegroundService::class.java).apply {
                action = ACTION_CANCEL_TRANSFER
            }
    }
}

data class TransferNotificationContent(
    val title: String,
    val text: String,
    val percentComplete: Int,
    val indeterminate: Boolean,
    val ongoing: Boolean,
) {
    companion object {
        fun inProgress(
            percentComplete: Int,
            bytesText: String,
        ): TransferNotificationContent =
            TransferNotificationContent(
                title = "设备迁移进行中",
                text = bytesText.ifBlank { "正在传输学习数据" },
                percentComplete = percentComplete.coerceIn(0, 100),
                indeterminate = false,
                ongoing = true,
            )

        fun verifying(): TransferNotificationContent =
            TransferNotificationContent(
                title = "正在校验迁移数据",
                text = "校验通过前不会替换原数据",
                percentComplete = 100,
                indeterminate = true,
                ongoing = true,
            )

        fun failed(reason: String): TransferNotificationContent =
            TransferNotificationContent(
                title = "设备迁移失败",
                text = reason.ifBlank { "目标设备原数据仍保留" },
                percentComplete = 0,
                indeterminate = false,
                ongoing = false,
            )
    }
}
