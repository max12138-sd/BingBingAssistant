package com.bingbing.assistant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 克制规则（§5.5 第 3 步：命中 counter_rule 加/减权重）。
 * TODO(SPEC-MISSING)：规格书未定义字段结构，按下述最小集实现。
 * 匹配语义：候选阵容中含 attackerTag 标签的英雄，面对含 targetTag 的对方阵容时，胜率权重加 modifier。
 */
@Entity(tableName = "counter_rule")
data class CounterRuleEntity(
    @PrimaryKey val id: String,        // 如 "cr_anti_front_vs_tank"
    val attackerTag: String,           // 我方英雄标签，如 "anti_front"
    val targetTag: String,             // 对方英雄标签，如 "front_tank"
    val modifier: Float,               // 权重修正，正负均可，如 +0.05 / -0.08
    val note: String                   // "克制提示" 文案，UI 直接展示
)
