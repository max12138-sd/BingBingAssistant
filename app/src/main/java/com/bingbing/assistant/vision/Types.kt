package com.bingbing.assistant.vision

/** vision 包公共类型：界面类型 / 识别结果 / 日历解析产物 */

/** §6.2 界面分类结果。UNKNOWN 必须被下游引导用户，不许猜（铁律 L4）。 */
enum class ScreenType {
    UNKNOWN,
    ARENA_OPPONENT,   // 竞技场对手阵容页
    PEAK_SETUP,       // 巅峰布阵页（3/5 队由槽位数量判定）
    BATTLE_RUNNING,   // 战斗播放中 —— 悬浮球静默（§7.1）
    CALENDAR_DAY,     // 活动日历页
    HERO_SELECT,      // 英雄选择页
}

/** §6.5 头像匹配结果 */
data class MatchResult(
    val heroId: String?,      // null = UNKNOWN（L4：不许猜）
    val score: Float,         // TM_CCOEFF_NORMED 最大值
    val confidence: Confidence
)

enum class Confidence { HIGH, MID, LOW }

/** §5.6 日历解析产物（入库前转 ActivityEntity） */
data class ParsedActivity(
    val name: String,          // 图例 OCR 得到
    val colorHex: String,      // 活动色块主色
    val startDay: Int?,        // 当月几号；跨月为 null
    val endDay: Int?,
    val dateConfidence: Float
)
