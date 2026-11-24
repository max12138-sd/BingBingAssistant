package com.bingbing.assistant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 活动（§4.1）。日期可能来自日历 OCR 反推，故带 dateConfidence；<0.7 时 UI 必须标"日期待确认"。 */
@Entity(tableName = "activity")
data class ActivityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brief: String,
    val startDate: String,             // yyyy-MM-dd
    val endDate: String,
    val priority: String,              // HIGH/MID/LOW
    val source: String,                // OCR / MANUAL
    val dateConfidence: Float,         // 0..1，日历反推的置信度
    val updatedAt: Long
)
