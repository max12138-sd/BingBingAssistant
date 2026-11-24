package com.bingbing.assistant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 英雄主档（§4.1，字段名照抄规格书）。
 * 数据来源：assets/seed/heroes.json 首启导入，或设置页 CSV/JSON 导入。
 */
@Entity(tableName = "hero")
data class HeroEntity(
    @PrimaryKey val id: String,        // 稳定 ID，如 "hero_bingnv"
    val name: String,                  // "冰女"
    val role: String,                  // FRONT / MID / BACK
    val faction: String?,              // 阵营
    val atk: Int,
    val hp: Int,
    val def: Int,
    val spd: Int,
    val rarity: Int,                   // 初始星级
    val counterTags: String,           // 逗号分隔，如 "anti_front,anti_magic"
    val iconAsset: String,             // "heroes/hero_bingnv.webp"
    val dataVersion: Int               // 数据版本号，用于"过期提示"
) {
    /** counterTags 便捷访问，避免每个调用点重复 split */
    val tagList: List<String> get() = if (counterTags.isBlank()) emptyList() else counterTags.split(',')
}
