package com.bingbing.assistant.core

import android.content.Context
import android.util.DisplayMetrics

/**
 * 设备画像（§3 core/DeviceProfile.kt / §6.3）：
 * 只负责给出「宽高比 + 分辨率 key」，具体布局 JSON 匹配在 vision.LayoutProfile。
 * 禁止在此硬编码任何游戏界面坐标（§14 第 5 条）。
 */
object DeviceProfile {

    /** 取"长边/短边"宽高比（横竖屏都归一为 >1），与布局配置文件的 screenAspect 对齐 */
    fun screenAspect(context: Context): Float {
        val dm: DisplayMetrics = context.resources.displayMetrics
        val longSide = maxOf(dm.widthPixels, dm.heightPixels).toFloat()
        val shortSide = minOf(dm.widthPixels, dm.heightPixels).toFloat()
        return if (shortSide <= 0f) 2.0f else longSide / shortSide
    }

    /** 分辨率 key，如 "2340x1080"，用于资产文件命名 */
    fun resolutionKey(context: Context): String {
        val dm = context.resources.displayMetrics
        return "${maxOf(dm.widthPixels, dm.heightPixels)}x${minOf(dm.widthPixels, dm.heightPixels)}"
    }

    /**
     * 从候选比例中选最接近的目标配置。
     * 超差 0.15 时返回 null，由 LayoutProfile 走"最近比例缩放"降级（§6.3）。
     */
    fun pickClosest(candidates: Map<Float, String>, target: Float): String? {
        if (candidates.isEmpty()) return null
        val best = candidates.minByOrNull { (_, name) ->
            val a = aspectFromName(name)
            kotlin.math.abs(a - target)
        } ?: return null
        val bestAspect = aspectFromName(best.value)
        return if (kotlin.math.abs(bestAspect - target) <= 0.15f) best.value else null
    }

    /** 文件名约定：{类型}_{W}x{H}.json，如 peak5_2340x1080.json */
    fun aspectFromName(name: String): Float {
        val m = Regex("""(\d+)x(\d+)""").find(name) ?: return 0f
        val (w, h) = m.destructured
        val wi = w.toLong(); val hi = h.toLong()
        if (wi <= 0 || hi <= 0) return 0f
        return maxOf(wi, hi).toFloat() / minOf(wi, hi).toFloat()
    }
}
