package com.bingbing.assistant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 段位英雄热度先验（§5.4.1 隐藏位采样用）。 */
@Entity(tableName = "hero_usage_prior")
data class HeroUsagePriorEntity(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    val bracket: String,               // BRONZE/SILVER/GOLD/LEGEND
    val heroId: String,
    val weight: Float                  // 0..1 热度
)
