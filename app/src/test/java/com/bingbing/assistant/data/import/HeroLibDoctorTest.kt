package com.bingbing.assistant.data.import

import com.bingbing.assistant.data.entity.HeroSkillEntity
import com.bingbing.assistant.data.entity.HeroEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 英雄库体检（M1 验收：报出缺图/缺字段项） */
class HeroLibDoctorTest {

    private fun hero(id: String, name: String = id, role: String = "MID",
                     atk: Int = 800, hp: Int = 5000, rarity: Int = 3) = HeroEntity(
        id = id, name = name, role = role, faction = null,
        atk = atk, hp = hp, def = 250, spd = 110,
        rarity = rarity, counterTags = "", iconAsset = "heroes/$id.webp", dataVersion = 1
    )

    @Test
    fun `missing icon reported`() {
        val report = HeroLibDoctor.examine(
            listOf(hero("hero_a")),
            listOf(HeroSkillEntity(heroId = "hero_a", name = "技能", type = "AOE", coef = 1f, cd = 3, unlockStar = 3)),
            iconExists = { false }
        )
        assertFalse(report.healthy)
        assertTrue(report.issues.any { it.type == HeroLibDoctor.IssueType.MISSING_ICON })
    }

    @Test
    fun `invalid role and missing skill reported`() {
        val report = HeroLibDoctor.examine(
            listOf(hero("hero_b", role = "TANK")),
            emptyList(),
            iconExists = { true }
        )
        assertTrue(report.issues.any { it.type == HeroLibDoctor.IssueType.INVALID_VALUE })
        assertTrue(report.issues.any { it.type == HeroLibDoctor.IssueType.NO_SKILL })
    }

    @Test
    fun `clean library passes`() {
        val report = HeroLibDoctor.examine(
            listOf(hero("hero_c")),
            listOf(HeroSkillEntity(heroId = "hero_c", name = "技能", type = "HEAL", coef = 1f, cd = 3, unlockStar = 3)),
            iconExists = { true }
        )
        assertTrue(report.healthy)
        assertEquals(1, report.heroCount)
    }
}
