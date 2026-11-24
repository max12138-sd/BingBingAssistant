package com.bingbing.assistant.data.repo

import com.bingbing.assistant.data.db.AppDatabase
import com.bingbing.assistant.data.entity.CounterRuleEntity
import com.bingbing.assistant.data.entity.HeroEntity
import com.bingbing.assistant.data.entity.HeroSkillEntity
import com.bingbing.assistant.data.entity.HeroUsagePriorEntity
import com.bingbing.assistant.data.entity.LineupTemplateEntity
import com.bingbing.assistant.data.import.CsvImporter
import com.bingbing.assistant.data.import.JsonImporter
import com.bingbing.assistant.data.import.SeedHero
import kotlinx.coroutines.flow.Flow

/** 英雄主档仓储：UI 只读展示，维护走导入器。 */
class HeroRepository(private val db: AppDatabase) {

    fun observeAll(): Flow<List<HeroEntity>> = db.heroDao().observeAll()

    suspend fun getAll(): List<HeroEntity> = db.heroDao().getAll()

    suspend fun getByIds(ids: List<String>): List<HeroEntity> =
        if (ids.isEmpty()) emptyList() else db.heroDao().getByIds(ids)

    suspend fun getById(id: String): HeroEntity? = db.heroDao().getById(id)

    suspend fun getSkills(heroId: String): List<HeroSkillEntity> = db.heroDao().getSkills(heroId)

    suspend fun getAllSkills(): List<HeroSkillEntity> = db.heroDao().getAllSkills()

    /** 当前英雄库数据版本（设置页"数据待更新"黄条依据，§13） */
    suspend fun maxDataVersion(): Int = db.heroDao().maxDataVersion() ?: 0

    suspend fun getAllTemplates(): List<LineupTemplateEntity> = db.heroDao().getAllTemplates()

    suspend fun getAllCounterRules(): List<CounterRuleEntity> = db.heroDao().getAllCounterRules()

    suspend fun getUsagePriors(bracket: String): List<HeroUsagePriorEntity> =
        db.heroDao().getUsagePriors(bracket)

    // ---- 维护入口（设置页） ----

    suspend fun importFromCsv(text: String): Int {
        val heroes = CsvImporter.parse(text)
        db.heroDao().insertHeroes(heroes)
        return heroes.size
    }

    suspend fun importFromJson(json: String): Int {
        val seeds: List<SeedHero> = JsonImporter.parseHeroes(json)
        db.heroDao().insertHeroes(seeds.map { it.toEntity() })
        db.heroDao().insertSkills(seeds.flatMap { it.toSkillEntities() })
        return seeds.size
    }
}
