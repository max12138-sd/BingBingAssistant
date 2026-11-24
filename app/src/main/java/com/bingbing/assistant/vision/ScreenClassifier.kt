package com.bingbing.assistant.vision

import android.graphics.Bitmap
import com.bingbing.assistant.core.Logger

/**
 * 界面分类（§6.2 / §3 vision/ScreenClassifier.kt）。
 * M3 实现：特征模板匹配（日历页 DAY 图标、巅峰布阵队伍框、战斗界面暗幕特征）。
 * 识别失败必须返回 UNKNOWN 并由上游引导（§13），禁止猜测。
 */
class ScreenClassifier {

    suspend fun classify(bitmap: Bitmap): ScreenType {
        // TODO(M3): 按下述顺序判别，命中即返回：
        //  1) 战斗界面：整屏亮度骤降 + 回合数字样特征 → BATTLE_RUNNING（悬浮球静默，§7.1）
        //  2) 日历页：右上 DAY 图标 + 7 列网格线特征 → CALENDAR_DAY
        //  3) 巅峰布阵：3 组 / 5 组队伍框 → PEAK_SETUP
        //  4) 竞技场对手页：单队伍卡 + 挑战按钮 → ARENA_OPPONENT
        //  全部未命中 → UNKNOWN
        Logger.w("ScreenClassifier", "界面分类尚未实现（M3），返回 UNKNOWN")
        return ScreenType.UNKNOWN
    }
}
