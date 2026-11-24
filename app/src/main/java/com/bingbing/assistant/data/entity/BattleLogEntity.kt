package com.bingbing.assistant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 战果回填（§7.6）：每套方案打完记录实际胜场，累积 30 条触发一次校准。 */
@Entity(tableName = "battle_log")
data class BattleLogEntity(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    val mode: String,                  // ARENA / PEAK3 / PEAK5
    val planId: String,                // 用了哪套方案
    val predictedWinRate: Float,       // 预测总胜率或期望胜场
    val actualWins: Int,               // 实际赢了几场
    val createdAt: Long
)
