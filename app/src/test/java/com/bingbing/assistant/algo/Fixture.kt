package com.bingbing.assistant.algo

import com.bingbing.assistant.data.entity.HeroEntity
import com.bingbing.assistant.data.entity.HeroSkillEntity
import com.bingbing.assistant.model.MyHero

/** 测试夹具：纯 JVM 构造英雄/技能表 */
object Fixture {

    fun hero(id: String, role: String = "MID", faction: String = "元素",
             atk: Int = 800, hp: Int = 5000, def: Int = 250, spd: Int = 110,
             rarity: Int = 3, tags: String = "") = HeroEntity(
        id = id, name = id, role = role, faction = faction,
        atk = atk, hp = hp, def = def, spd = spd,
        rarity = rarity, counterTags = tags,
        iconAsset = "heroes/$id.webp", dataVersion = 1
    )

    fun skill(heroId: String, type: String, coef: Float = 1.5f, cd: Int = 3) = HeroSkillEntity(
        heroId = heroId, name = "$heroId-$type", type = type,
        coef = coef, cd = cd, unlockStar = 3
    )

    fun buildSimulator(heroCount: Int = 30): BattleSimulator {
        val heroes = (1..heroCount).map { i ->
            val role = when (i % 3) { 0 -> "FRONT"; 1 -> "MID"; else -> "BACK" }
            hero("hero_$i", role = role, atk = 700 + i * 10, hp = 4500 + i * 100,
                def = 200 + i * 8, spd = 90 + i, tags = if (i % 4 == 0) "anti_front" else "")
        }.associateBy { it.id }
        val skills = heroes.keys.flatMap {
            listOf(skill(it, "SINGLE"), skill(it, "AOE", 1.2f, 4))
        }.groupBy { it.heroId }
        return BattleSimulator(heroes, skills)
    }

    fun pool(count: Int): List<MyHero> =
        (1..count).map { MyHero("hero_$it", 5, 0) }
}
