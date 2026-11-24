package com.bingbing.assistant.feature.activity

import com.bingbing.assistant.core.AppGraph
import com.bingbing.assistant.data.entity.ActivityEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 活动流程（§7.5）：
 * 缓存新鲜(≤24h) 直接展示；否则引导用户打开日历页 → 轮询识别 → 合并去重 → 刷新。
 * 失败 3 次 → 提示手动 JSON 导入（§13）。
 */
class ActivityFlow {

    enum class Group { RUNNING, UPCOMING, ENDING_TODAY }   // 进行中 / 即将开始 / 今日到期

    data class SevenDayView(
        val groups: Map<Group, List<ActivityEntity>>,
        val stale: Boolean,                 // 缓存超 24h，需要重新识别
        val lowConfidence: List<String>,    // dateConfidence < 0.7 的活动名（UI 标"日期待确认"）
    )

    /** 7 天视图（纯函数，可单测；stale 由调用方传入，见 [sevenDayView]） */
    fun groupSevenDays(activities: List<ActivityEntity>, today: LocalDate, stale: Boolean = false): SevenDayView {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val running = mutableListOf<ActivityEntity>()
        val upcoming = mutableListOf<ActivityEntity>()
        val endingToday = mutableListOf<ActivityEntity>()

        for (a in activities) {
            val start = runCatching { LocalDate.parse(a.startDate, fmt) }.getOrNull() ?: continue
            val end = runCatching { LocalDate.parse(a.endDate, fmt) }.getOrNull() ?: continue
            when {
                // 今日到期最紧急，优先归组（不再重复进"进行中"）
                end == today -> endingToday += a
                start.isAfter(today) && !start.isAfter(today.plusDays(7)) -> upcoming += a
                !start.isAfter(today) && !end.isBefore(today) -> running += a
            }
        }
        val low = activities.filter { it.dateConfidence < 0.7f }.map { it.name }   // §5.6 待确认
        return SevenDayView(
            mapOf(Group.RUNNING to running, Group.UPCOMING to upcoming, Group.ENDING_TODAY to endingToday),
            stale = stale,
            lowConfidence = low
        )
    }

    /** 挂起版：取库内活动 + 缓存新鲜度，供 UI 直接调用 */
    suspend fun sevenDayView(): SevenDayView {
        val all = com.bingbing.assistant.core.AppGraph.activityRepo.getAll()
        return groupSevenDays(all, LocalDate.now(), stale = stale())
    }

    /** 缓存是否新鲜（≤24h，按最近 updatedAt 判断） */
    suspend fun stale(): Boolean {
        val all = AppGraph.activityRepo.getAll()
        if (all.isEmpty()) return true
        val latest = all.maxOf { it.updatedAt }
        return System.currentTimeMillis() - latest > 24 * 3600_000L
    }

    /** 日历扫描结果入库（CalendarGridParser → 本方法）；M5 接入视觉后调用 */
    suspend fun mergeScanned(scanned: List<ActivityEntity>): Int =
        AppGraph.activityRepo.mergeFromScan(scanned, System.currentTimeMillis())

    /** 24h 内开始或结束的活动（每日提醒的判定，§7.5） */
    fun within24h(activities: List<ActivityEntity>, now: LocalDate): List<ActivityEntity> {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val tomorrow = now.plusDays(1)
        return activities.filter { a ->
            val start = runCatching { LocalDate.parse(a.startDate, fmt) }.getOrNull()
            val end = runCatching { LocalDate.parse(a.endDate, fmt) }.getOrNull()
            start == tomorrow || end == tomorrow
        }
    }
}
