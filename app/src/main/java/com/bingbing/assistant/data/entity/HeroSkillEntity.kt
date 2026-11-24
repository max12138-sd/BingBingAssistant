package com.bingbing.assistant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 英雄技能表（§4.1）。战斗模拟按 type/coef 取行为。 */
@Entity(tableName = "hero_skill")
data class HeroSkillEntity(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    val heroId: String,
    val name: String,
    val type: String,                  // SINGLE / AOE / CONTROL / HEAL / BUFF
    val coef: Float,                   // 伤害/治疗系数
    val cd: Int,                       // 冷却回合
    val unlockStar: Int
)
