package com.bingbing.assistant.data.import

import android.content.Context
import com.bingbing.assistant.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 首启种子导入（M1）：assets/seed 目录下的 json 种子 → Room。
 * 只在对应表为空时导入，幂等；重复启动不会覆盖用户已勾选的 my_hero。
 */
class AssetSeeder(
    private val context: Context,
    private val db: AppDatabase,
) {

    data class SeedResult(
        var heroCount: Int,
        var activityCount: Int,
        var templateCount: Int,
        var counterRuleCount: Int,
        var usagePriorCount: Int,
    )

    suspend fun seedIfEmpty(): SeedResult = withContext(Dispatchers.IO) {
        val result = SeedResult(0, 0, 0, 0, 0)

        if (db.heroDao().count() == 0) {
            val heroes = JsonImporter.parseHeroes(readAsset("seed/heroes.json"))
            db.heroDao().insertHeroes(heroes.map { it.toEntity() })
            db.heroDao().insertSkills(heroes.flatMap { it.toSkillEntities() })
            result.heroCount = heroes.size
        }

        if (db.heroDao().getAllTemplates().isEmpty()) {
            val templates = JsonImporter.parseTemplates(readAsset("seed/templates.json"))
            db.heroDao().insertLineupTemplates(templates.map { it.toEntity() })
            result.templateCount = templates.size
        }

        if (db.heroDao().getAllCounterRules().isEmpty()) {
            val rules = JsonImporter.parseCounterRules(readAsset("seed/counter_rules.json"))
            db.heroDao().insertCounterRules(rules.map { it.toEntity() })
            result.counterRuleCount = rules.size
        }

        if (db.heroDao().countUsagePriors() == 0) {
            val priors = JsonImporter.parseUsagePriors(readAsset("seed/usage_priors.json"))
            db.heroDao().insertUsagePriors(priors.map { it.toEntity() })
            result.usagePriorCount = priors.size
        }

        if (db.activityDao().count() == 0) {
            val activities = JsonImporter.parseActivities(readAsset("seed/activities.json"))
            db.activityDao().upsertAll(activities.map { it.toEntity(now = System.currentTimeMillis()) })
            result.activityCount = activities.size
        }

        result
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
}
