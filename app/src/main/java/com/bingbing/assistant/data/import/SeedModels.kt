package com.bingbing.assistant.data.import

import com.bingbing.assistant.data.entity.ActivityEntity
import com.bingbing.assistant.data.entity.CounterRuleEntity
import com.bingbing.assistant.data.entity.HeroEntity
import com.bingbing.assistant.data.entity.HeroSkillEntity
import com.bingbing.assistant.data.entity.HeroUsagePriorEntity
import com.bingbing.assistant.data.entity.LineupTemplateEntity

/**
 * 种子数据 DTO（附录 A 的 JSON 结构）与实体转换。
 * 独立于 Room 实体，保证导入格式变更不影响表结构。
 */

data class SeedSkill(
    val name: String,
    val type: String,          // SINGLE / AOE / CONTROL / HEAL / BUFF
    val coef: Float,
    val cd: Int,
    val unlockStar: Int
)

data class SeedHero(
    val id: String,
    val name: String,
    val role: String,          // FRONT / MID / BACK
    val faction: String?,
    val atk: Int,
    val hp: Int,
    val def: Int,
    val spd: Int,
    val rarity: Int,
    val counterTags: String,
    val iconAsset: String,
    val dataVersion: Int,
    val skills: List<SeedSkill>
) {
    fun toEntity() = HeroEntity(
        id = id, name = name, role = role, faction = faction,
        atk = atk, hp = hp, def = def, spd = spd,
        rarity = rarity, counterTags = counterTags,
        iconAsset = iconAsset, dataVersion = dataVersion
    )

    fun toSkillEntities(heroId: String = id) = skills.map {
        HeroSkillEntity(
            heroId = heroId, name = it.name, type = it.type,
            coef = it.coef, cd = it.cd, unlockStar = it.unlockStar
        )
    }
}

/** activities.json / 手动导入格式（字段是 start/end，导入时映射到 startDate/endDate） */
data class SeedActivity(
    val name: String,
    val brief: String,
    val start: String,
    val end: String,
    val priority: String,
    val source: String
) {
    fun toEntity(now: Long, dateConfidence: Float = 1.0f) = ActivityEntity(
        id = "act_${name.hashCode()}",   // 手动数据无稳定 ID，用名称哈希；OCR 导入的走同名覆盖
        name = name, brief = brief,
        startDate = start, endDate = end,
        priority = priority, source = source,
        dateConfidence = dateConfidence, updatedAt = now
    )
}

data class SeedTemplate(
    val id: String,
    val name: String,
    val roles: List<String>,
    val coreHeroIds: List<String>,
    val tags: List<String>,
    val dataVersion: Int
) {
    fun toEntity() = LineupTemplateEntity(
        id = id, name = name,
        roleSlots = roles.joinToString(","),
        coreHeroIds = coreHeroIds.joinToString(","),
        tags = tags.joinToString(","),
        dataVersion = dataVersion
    )
}

data class SeedCounterRule(
    val id: String,
    val attackerTag: String,
    val targetTag: String,
    val modifier: Float,
    val note: String
) {
    fun toEntity() = CounterRuleEntity(
        id = id, attackerTag = attackerTag, targetTag = targetTag,
        modifier = modifier, note = note
    )
}

data class SeedUsagePrior(
    val bracket: String,
    val heroId: String,
    val weight: Float
) {
    fun toEntity() = HeroUsagePriorEntity(bracket = bracket, heroId = heroId, weight = weight)
}
