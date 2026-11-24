package com.bingbing.assistant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 我的英雄库（Q4：没有固定主力池 → 用户勾选拥有情况，§7.2）。
 * 推荐算法的硬约束：候选只从这里出。
 */
@Entity(tableName = "my_hero")
data class MyHeroEntity(
    @PrimaryKey val heroId: String,
    val star: Int,                     // 用户实际星级（3..5）
    val gearLevel: Int,                // 装备等级（可粗填 0..N）
    val power: Int?                    // 可选，用户填的战力
)
