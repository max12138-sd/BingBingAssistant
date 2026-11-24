package com.bingbing.assistant.vision

import android.graphics.Bitmap
import com.bingbing.assistant.core.Logger
import com.bingbing.assistant.model.SlotState

/**
 * 卡槽存在性检测（§6.4，C1 关键改动：隐藏位置不固定，必须逐槽判断）。
 *
 * 判定规则：
 *  - 主色接近"暗底 + 问号/剪影色" → HIDDEN（有英雄但被隐藏）
 *  - 边缘密度 > 阈值（有头像轮廓）  → VISIBLE
 *  - 纯背景                        → EMPTY
 *
 * HIDDEN 与 EMPTY 必须区分（T3 验收：隐藏位在 2/4/5 不能误判为 EMPTY）。
 */
class SlotDetector {

    suspend fun detect(screen: Bitmap, rects: List<RectNorm>): List<SlotState> {
        // TODO(M3/M4): 对每个 rect 裁图 → 灰度 → Otsu 二值 → 边缘密度 + 主色判定
        //  返回与 rects 等长的状态列表；任何异常都视为整组识别失败（抛给上游降级）。
        Logger.w("SlotDetector", "卡槽检测尚未实现（M3），返回空")
        return List(rects.size) { SlotState.EMPTY }
    }
}
