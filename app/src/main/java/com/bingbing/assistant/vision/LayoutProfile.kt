package com.bingbing.assistant.vision

import android.content.Context
import com.bingbing.assistant.core.DeviceProfile
import com.bingbing.assistant.core.Logger
import org.json.JSONObject

/**
 * 归一化坐标布局配置（§6.3）。
 * 禁止硬编码坐标：所有区域都是 [x, y, w, h]（0..1），存 assets/layouts/{type}_{WxH}.json。
 * 多设备：按宽高比挑最接近的配置；无匹配用最近比例（归一化坐标本身可缩放）。
 */

/** 归一化矩形 [x, y, w, h]（相对长边截图 720p 宽坐标系） */
data class RectNorm(val x: Float, val y: Float, val w: Float, val h: Float) {
    fun toPixels(screenW: Int, screenH: Int): android.graphics.Rect =
        android.graphics.Rect(
            (x * screenW).toInt(), (y * screenH).toInt(),
            ((x + w) * screenW).toInt(), ((y + h) * screenH).toInt()
        )
}

data class SlotRects(
    /** key = "slot0".."slot4"，value = 归一化矩形 */
    val slots: Map<String, RectNorm>
) {
    /** 按站位序号 0..4 取，缺槽位返回 null（调用方按 §13 降级） */
    fun slot(index: Int): RectNorm? = slots["slot$index"]

    val size: Int get() = slots.size
}

data class PeaksLayout(
    val myTeams: List<SlotRects>,
    val oppTeams: List<SlotRects>
)

data class CalendarLayout(
    val monthHeaderRoi: RectNorm,   // "2026年11月" OCR 区
    val gridRoi: RectNorm,          // 日历网格区
    val legendRoi: RectNorm         // 图例区
)

data class LayoutProfile(
    val sourceFile: String,
    val screenAspect: Float,
    val arenaOppSlots: SlotRects?,
    val arenaMySlots: SlotRects?,
    val peaks: PeaksLayout?,
    val calendar: CalendarLayout?
) {
    companion object {

        private const val TAG = "LayoutProfile"
        const val DIR = "layouts"

        /**
         * 按「界面类型 + 宽高比」加载最接近的配置。
         * 文件命名：{type}_{W}x{H}.json，type ∈ {arena, peak3, peak5, calendar}
         */
        fun load(context: Context, type: String, aspect: Float): LayoutProfile? {
            val names = runCatching { context.assets.list(DIR)?.toList() ?: emptyList() }
                .getOrDefault(emptyList())
                .filter { it.startsWith("${type}_") && it.endsWith(".json") }
            if (names.isEmpty()) {
                Logger.w(TAG, "无 $type 布局配置（assets/layouts/）")
                return null
            }
            val candidates = names.associateBy({ DeviceProfile.aspectFromName(it) }, { it })
            val chosen = DeviceProfile.pickClosest(candidates, aspect)
                ?: candidates.minByOrNull { (a, _) -> kotlin.math.abs(a - aspect) }!!.value
            Logger.i(TAG, "布局配置 $type: $chosen (目标比例 $aspect)")

            return runCatching { parse(context.assets.open("$DIR/$chosen").readBytes().decodeToString(), chosen) }
                .onFailure { Logger.e(TAG, "布局配置解析失败: $chosen", it) }
                .getOrNull()
        }

        internal fun parse(json: String, sourceFile: String): LayoutProfile {
            val root = JSONObject(json)
            return LayoutProfile(
                sourceFile = sourceFile,
                screenAspect = root.optDouble("screenAspect", 2.0).toFloat(),
                arenaOppSlots = root.optJSONObject("arena")?.optJSONObject("oppSlots")?.let(::parseSlots),
                arenaMySlots = root.optJSONObject("arena")?.optJSONObject("mySlots")?.let(::parseSlots),
                peaks = root.optJSONObject("peaks")?.let { p ->
                    PeaksLayout(
                        myTeams = parseTeams(p.optJSONArray("myTeams")),
                        oppTeams = parseTeams(p.optJSONArray("oppTeams"))
                    )
                },
                calendar = root.optJSONObject("calendar")?.let { c ->
                    CalendarLayout(
                        monthHeaderRoi = parseRect(c.getJSONArray("monthHeaderRoi")),
                        gridRoi = parseRect(c.getJSONArray("gridRoi")),
                        legendRoi = parseRect(c.getJSONArray("legendRoi"))
                    )
                }
            )
        }

        private fun parseTeams(arr: org.json.JSONArray?): List<SlotRects> {
            if (arr == null) return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                runCatching { parseSlots(arr.getJSONObject(i)) }.getOrNull()
            }
        }

        private fun parseSlots(obj: JSONObject): SlotRects = SlotRects(
            obj.keys().asSequence().toList().associate { key ->
                key to parseRect(obj.getJSONArray(key))
            }
        )

        private fun parseRect(arr: org.json.JSONArray): RectNorm = RectNorm(
            arr.getDouble(0).toFloat(), arr.getDouble(1).toFloat(),
            arr.getDouble(2).toFloat(), arr.getDouble(3).toFloat()
        )
    }
}
