package com.bingbing.assistant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 阵营骨架模板（§5.5 竞技场推荐：以模板为骨架做同定位替换）。
 * TODO(SPEC-MISSING)：规格书未定义字段，按下述最小集实现；有真实数据结构后在此扩展。
 */
@Entity(tableName = "lineup_template")
data class LineupTemplateEntity(
    @PrimaryKey val id: String,        // 如 "tpl_burst"
    val name: String,                  // "速攻"
    val roleSlots: String,             // 逗号分隔 5 个定位，如 "FRONT,MID,MID,BACK,BACK"
    val coreHeroIds: String,           // 逗号分隔核心英雄，缺失位允许替换
    val tags: String,                  // 逗号分隔风格标签，如 "burst,control"
    val dataVersion: Int
) {
    val roleList: List<String> get() = roleSlots.split(',')
    val coreList: List<String> get() = if (coreHeroIds.isBlank()) emptyList() else coreHeroIds.split(',')
    val tagList: List<String> get() = if (tags.isBlank()) emptyList() else tags.split(',')
}
