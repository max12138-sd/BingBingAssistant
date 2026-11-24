package com.bingbing.assistant.feature.activity

import com.bingbing.assistant.data.entity.ActivityEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** §7.5 活动分组（纯函数部分） */
class ActivityFlowTest {

    private fun act(name: String, start: String, end: String, confidence: Float = 0.9f) = ActivityEntity(
        id = "act_$name", name = name, brief = "", startDate = start, endDate = end,
        priority = "MID", source = "MANUAL", dateConfidence = confidence, updatedAt = 0L
    )

    private val today: LocalDate = LocalDate.of(2026, 11, 23)

    @Test
    fun `groups running upcoming and ending today`() {
        val flow = ActivityFlow()
        val list = listOf(
            act("进行中", "2026-11-20", "2026-11-25"),
            act("即将开始", "2026-11-25", "2026-11-30"),
            act("今天结束", "2026-11-18", "2026-11-23"),
            act("过期", "2026-11-01", "2026-11-10"),
            act("太远", "2026-12-20", "2026-12-30"),
        )
        val view = flow.groupSevenDays(list, today)
        assertEquals(listOf("进行中"), view.groups[ActivityFlow.Group.RUNNING]!!.map { it.name })
        assertEquals(listOf("即将开始"), view.groups[ActivityFlow.Group.UPCOMING]!!.map { it.name })
        assertEquals(listOf("今天结束"), view.groups[ActivityFlow.Group.ENDING_TODAY]!!.map { it.name })
    }

    @Test
    fun `low confidence flagged for date confirm`() {
        val flow = ActivityFlow()
        val list = listOf(
            act("跨月活动", "2026-11-20", "2026-12-05", confidence = 0.5f),
            act("正常活动", "2026-11-20", "2026-11-25", confidence = 0.9f),
        )
        val view = flow.groupSevenDays(list, today)
        assertEquals(listOf("跨月活动"), view.lowConfidence)   // §5.6：<0.7 必须标"日期待确认"
    }

    @Test
    fun `within24h catches start or end tomorrow`() {
        val flow = ActivityFlow()
        val list = listOf(
            act("明天开始", "2026-11-24", "2026-11-30"),
            act("明天结束", "2026-11-18", "2026-11-24"),
            act("无关", "2026-11-20", "2026-11-26"),
        )
        val targets = flow.within24h(list, today)
        assertEquals(2, targets.size)
        assertTrue(targets.any { it.name == "明天开始" })
        assertTrue(targets.any { it.name == "明天结束" })
    }
}
