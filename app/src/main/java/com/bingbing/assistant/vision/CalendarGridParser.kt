package com.bingbing.assistant.vision

import android.graphics.Bitmap
import com.bingbing.assistant.core.Logger

/**
 * 日历网格反推日期（§5.6）。
 * 纯 OCR 拿不到起止日 —— 起止藏在「色块覆盖哪些格子」里，必须网格 + 色块 + 图例三步。
 *
 * 输出 dateConfidence 规则：
 *   网格线全检出 + 图例 OCR 成功 = 0.9
 *   缺图例 = 0.6
 *   跨月   = 0.5
 * UI 上 < 0.7 必须显示「日期待确认」。
 */
class CalendarGridParser(private val ocr: OcrEngine) {

    suspend fun parse(bitmap: Bitmap): List<ParsedActivity> {
        // TODO(M5): 按 §5.6 步骤 1-9 实现：
        //  1) 灰度→Otsu二值→形态学开运算
        //  2) 霍夫直线/投影法检出横竖网格线 → rows × 7 格子坐标
        //  3) OCR 顶部"YYYY年MM月" → 定位 1 号
        //  4) cell(r,c) → date 映射
        //  5) cell 内 HSV 众数聚类 → k 种活动色（ColorBlobCluster）
        //  6) 每种颜色的覆盖格集合 → 起止日
        //  7) 图例 OCR → 颜色↔活动名
        //  8) 跨月 → endDateConfidence=0.5
        //  9) 与 DB 缓存交叉校验
        Logger.w("CalendarGridParser", "日历解析尚未实现（M5），返回空")
        return emptyList()
    }
}
