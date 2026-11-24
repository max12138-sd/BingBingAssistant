package com.bingbing.assistant.core

import android.content.ComponentName
import android.content.Intent
import android.os.Build

/**
 * 国产 ROM 差异化（§9.2 / §3 core/RomAdapter.kt）。
 * 各 ROM 自启动页组件名随版本会变，全部 try/catch 兜底，失败时回退应用详情页。
 */
object RomAdapter {

    enum class Rom { HUAWEI, HONOR, XIAOMI, OPPO, VIVO, SAMSUNG, OTHER }

    fun detect(): Rom = when (Build.MANUFACTURER.uppercase()) {
        "HUAWEI" -> Rom.HUAWEI
        "HONOR" -> Rom.HONOR
        "XIAOMI", "REDMI" -> Rom.XIAOMI
        "OPPO", "REALME", "ONEPLUS" -> Rom.OPPO
        "VIVO", "IQOO" -> Rom.VIVO
        "SAMSUNG" -> Rom.SAMSUNG
        else -> Rom.OTHER
    }

    fun romName(rom: Rom = detect()): String = when (rom) {
        Rom.HUAWEI -> "华为 / HarmonyOS"
        Rom.HONOR -> "荣耀"
        Rom.XIAOMI -> "小米 / HyperOS"
        Rom.OPPO -> "OPPO / ColorOS"
        Rom.VIVO -> "vivo / OriginOS"
        Rom.SAMSUNG -> "三星"
        Rom.OTHER -> "其他系统"
    }

    /**
     * 自启动管理页（华为"应用启动管理"等）。null = 未收录该 ROM，回退应用详情页。
     * 组件名引用自各 ROM 公开资料，失效属正常，调用方需兜底。
     */
    fun autostartIntent(rom: Rom = detect()): Intent? {
        val comps = when (rom) {
            Rom.HUAWEI -> listOf(
                "com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity",
                "com.huawei.systemmanager/.appcontrol.activity.StartupAppControlActivity",
            )
            Rom.HONOR -> listOf(
                "com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity",
            )
            Rom.XIAOMI -> listOf(
                "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity",
            )
            Rom.OPPO -> listOf(
                "com.coloros.safecenter/com.coloros.safecenter.startupapp.StartupAppListActivity",
                "com.oppo.safe/com.oppo.safe.permission.startup.StartupAppListActivity",
            )
            Rom.VIVO -> listOf(
                "com.vivo.permissionmanager/com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            )
            else -> emptyList()
        }
        for (c in comps) {
            try {
                val cn = ComponentName.unflattenFromString(c) ?: continue
                return Intent().setComponent(cn).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } catch (_: Exception) {
                // 组件不存在，试下一个
            }
        }
        return null
    }

    /** 应用详情页兜底 */
    fun appDetailsIntent(packageName: String): Intent = Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        android.net.Uri.parse("package:$packageName")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * HarmonyOS NEXT 5.x 探测（§9.1：NEXT 不能跑 APK）。
     * NEXT 不会安装普通 APK，此探测只在 HarmonyOS 4（可跑 APK）上做"升级预警"用途。
     * TODO(SPEC-MISSING)：NEXT 真机属性名未实测，先按平台版本号 >= 5 判断。
     */
    fun isHarmonyNext(): Boolean = runCatching {
        val sp = Class.forName("android.os.SystemProperties")
        val get = sp.getMethod("get", String::class.java, String::class.java)
        val v = get.invoke(null, "hw_sc.build.platform.version", "") as? String ?: ""
        val major = v.substringBefore('.').toIntOrNull() ?: 0
        Build.MANUFACTURER.uppercase() == "HUAWEI" && major >= 5
    }.getOrDefault(false)
}
