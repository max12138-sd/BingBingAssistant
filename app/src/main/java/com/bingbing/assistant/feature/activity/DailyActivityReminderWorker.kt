package com.bingbing.assistant.feature.activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bingbing.assistant.R
import com.bingbing.assistant.core.Logger
import java.time.LocalDate

/**
 * 每日活动提醒（§7.5）：WorkManager 每日检查一次，24h 内开始/结束 → 本地通知一次。
 * 无网络依赖（L1）；通知权限未授予时静默跳过。
 */
class DailyActivityReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val flow = ActivityFlow()
        val activities = try {
            com.bingbing.assistant.core.AppGraph.activityRepo.getAll()
        } catch (e: Exception) {
            Logger.e("Reminder", "读取活动失败", e)
            return Result.retry()
        }
        val targets = flow.within24h(activities, LocalDate.now())
        if (targets.isEmpty()) return Result.success()

        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Logger.i("Reminder", "无通知权限，跳过提醒")
            return Result.success()
        }
        for (a in targets.take(3)) {   // 一天最多提示 3 条，避免打扰
            val text = if (a.startDate == LocalDate.now().plusDays(1).toString()) "即将开始" else "即将结束"
            val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_capture)
                .setContentTitle("${a.name}（$text）")
                .setContentText(a.brief.ifBlank { "点击进入应用查看详情" })
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(ctx).notify(a.name.hashCode(), notification)
        }
        Logger.i("Reminder", "已提醒 ${targets.take(3).size} 条临期活动")
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "activity_reminder"
        const val WORK_NAME = "daily_activity_reminder"
    }
}
