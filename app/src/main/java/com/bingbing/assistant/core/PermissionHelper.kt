package com.bingbing.assistant.core

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 三类权限引导（§3 core/PermissionHelper.kt）：
 * 1) 悬浮窗 SYSTEM_ALERT_WINDOW —— 特殊授权，跳系统页
 * 2) 截图 MediaProjection —— Activity Result，授权数据由 ScreenCaptureService 持有
 * 3) 通知 POST_NOTIFICATIONS —— Android 13+ 运行时权限
 */
object PermissionHelper {

    const val REQ_NOTIFICATION = 1001
    const val REQ_SCREEN_CAPTURE = 1002

    // ---- 1) 悬浮窗 ----
    fun hasOverlay(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

    fun overlaySettingsIntent(ctx: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))

    // ---- 2) 截图授权（MediaProjection） ----
    fun screenCaptureRequestIntent(activity: Activity): Intent {
        val mpm = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return mpm.createScreenCaptureIntent()
    }

    // ---- 3) 通知（Android 13+） ----
    fun hasNotificationPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun requestNotification(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission(activity)) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATION
            )
        }
    }

    // ---- 电池优化白名单（国产 ROM 保活，§9.2） ----
    fun isIgnoringBatteryOptimizations(ctx: Context): Boolean {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    fun batterySettingsIntent(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
}
