package com.bingbing.assistant.vision

import android.graphics.Bitmap
import com.bingbing.assistant.core.Logger

/**
 * 头像多尺度模板匹配（§6.5）。
 * 模板库在 assets/heroes/（heroId.webp，约 100 张），先缩到 720p 宽再匹配（§10.1）。
 */
class HeroMatcher {

    /**
     * 对一张卡面做多尺度匹配。
     * score >= 0.82 → 直接判定 HIGH
     * 0.70..0.82   → OCR 读名字二次确认 MID（名字对上才算）
     * < 0.70       → UNKNOWN（铁律 L4：不许猜）
     */
    suspend fun match(cardBitmap: Bitmap, candidateHeroIds: List<String>): MatchResult {
        // TODO(M3): OpenCV Imgproc.matchTemplate TM_CCOEFF_NORMED，尺度 0.85/1.0/1.15，
        //  按 candidateHeroIds 并行匹配（5 协程，§10.1），取最大值。
        Logger.w("HeroMatcher", "头像匹配尚未实现（M3），返回 UNKNOWN")
        return MatchResult(heroId = null, score = 0f, confidence = Confidence.LOW)
    }
}
