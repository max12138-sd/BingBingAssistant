package com.bingbing.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bingbing.assistant.core.AppGraph
import com.bingbing.assistant.core.Logger
import com.bingbing.assistant.data.import.AssetSeeder
import com.bingbing.assistant.feature.activity.DailyActivityReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Application（§3）：初始化 Logger / DB / OpenCV / OCR 模型 / 种子数据。
 * OCR 模型异步加载，保证冷启动到浮球 ≤ 2.5s（§10）。
 */
class App : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Logger.init(this)
        logDevice()                      // 附录 B / M0 验收：打印 SDK_INT 实际值
        AppGraph.init(this)
        createNotificationChannels()

        appScope.launch {
            // M0 冒烟：OpenCV 原生库加载（失败不阻断，视觉模块在 M3 提供真实调用）
            val openCvOk = org.opencv.android.OpenCVLoader.initLocal()
            Logger.i("App", "OpenCV initLocal=$openCvOk")

            AppGraph.ocrEngine.extractModelsIfNeeded()   // assets 未含模型时静默跳过（M3 提供资产）

            // M1：首启种子导入（幂等）
            runCatching {
                val result = AssetSeeder(this@App, AppGraph.db).seedIfEmpty()
                Logger.i(
                    "App",
                    "种子导入 heroes=${result.heroCount} activities=${result.activityCount} " +
                        "templates=${result.templateCount} rules=${result.counterRuleCount} priors=${result.usagePriorCount}"
                )
            }.onFailure { Logger.e("App", "种子导入失败", it) }

            // M6：启动时恢复校准度显示
            runCatching {
                val logs = AppGraph.battleLogRepo.recent(30)
                com.bingbing.assistant.algo.SimConfig.applyCalibration(logs)
            }
        }

        scheduleDailyReminder()
    }

    /** 附录 B：SDK_INT 一行日志 */
    private fun logDevice() {
        Log.i("DEVICE", "SDK_INT=${Build.VERSION.SDK_INT}, " +
            "${Build.MANUFACTURER} ${Build.MODEL}, ${Build.DISPLAY}")
        Logger.i("App", "SDK_INT=${Build.VERSION.SDK_INT} ${Build.MANUFACTURER} ${Build.MODEL}")
        if (com.bingbing.assistant.core.RomAdapter.isHarmonyNext()) {
            Logger.w("App", "检测到 HarmonyOS NEXT，普通 APK 无法安装运行（§9.1）")
        }
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                DailyActivityReminderWorker.CHANNEL_ID,
                getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    /** §7.5：WorkManager 每日检查临期活动（本地，无网络） */
    private fun scheduleDailyReminder() {
        val request = PeriodicWorkRequestBuilder<DailyActivityReminderWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DailyActivityReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
