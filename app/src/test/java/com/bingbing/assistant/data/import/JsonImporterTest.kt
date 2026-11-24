package com.bingbing.assistant.data.import

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** 种子/导入解析（附录 A 格式） */
class JsonImporterTest {

    @Test
    fun `parse heroes seed json`() {
        val json = """
        [
          {"id":"hero_bingnv","name":"冰女","role":"MID","faction":"元素",
           "atk":820,"hp":4200,"def":210,"spd":118,"rarity":3,
           "counterTags":"anti_aoe,magic_burst",
           "iconAsset":"heroes/hero_bingnv.webp","dataVersion":1,
           "skills":[{"name":"寒冰爆裂","type":"AOE","coef":1.8,"cd":3,"unlockStar":3}]}
        ]
        """.trimIndent()
        val heroes = JsonImporter.parseHeroes(json)
        assertEquals(1, heroes.size)
        val h = heroes[0]
        assertEquals("hero_bingnv", h.id)
        assertEquals("MID", h.role)
        assertEquals(820, h.atk)
        assertEquals(1, h.skills.size)
        assertEquals("AOE", h.skills[0].type)
        assertEquals(1.8f, h.skills[0].coef, 1e-4f)
        val e = h.toEntity()
        assertEquals(listOf("anti_aoe", "magic_burst"), e.tagList)
        assertEquals(1, h.toSkillEntities().size)
    }

    @Test
    fun `parse activities seed json maps start end`() {
        val json = """
        [{"name":"限时召唤·冰女","brief":"抽卡","start":"2026-11-22","end":"2026-11-28","priority":"HIGH","source":"MANUAL"}]
        """.trimIndent()
        val list = JsonImporter.parseActivities(json)
        assertEquals(1, list.size)
        assertEquals("2026-11-22", list[0].start)
        assertEquals("HIGH", list[0].priority)
    }

    @Test
    fun `missing required field throws ImportException`() {
        val json = """[{"id":"x","name":"无定位英雄"}]"""
        try {
            JsonImporter.parseHeroes(json)
            fail("应抛 ImportException")
        } catch (e: JsonImporter.ImportException) {
            assertTrue(e.message!!.contains("role"))
        }
    }

    @Test
    fun `malformed json throws ImportException`() {
        try {
            JsonImporter.parseHeroes("not json")
            fail("应抛 ImportException")
        } catch (e: JsonImporter.ImportException) {
            // 预期
        }
    }
}
