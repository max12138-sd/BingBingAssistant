package com.bingbing.assistant.data.import

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/** CSV 导入（M1 维护路径） */
class CsvImporterTest {

    @Test
    fun `parse standard rows`() {
        val csv = """
            id,name,role,faction,atk,hp,def,spd,rarity,counterTags,iconAsset,dataVersion
            hero_x,小明,FRONT,圣光,500,9000,600,80,3,front_tank,heroes/hero_x.webp,1
            hero_y,小红,MID,,800,4000,200,120,4,,heroes/hero_y.webp,2
        """.trimIndent()
        val heroes = CsvImporter.parse(csv)
        assertEquals(2, heroes.size)
        assertEquals("小明", heroes[0].name)
        assertEquals("FRONT", heroes[0].role)
        assertEquals(600, heroes[0].def)
        assertEquals("front_tank", heroes[0].counterTags)
        assertEquals(null, heroes[1].faction)
        assertEquals(2, heroes[1].dataVersion)
    }

    @Test
    fun `quoted field with comma`() {
        val csv = "id,name,role,faction,atk,hp,def,spd,rarity,counterTags,iconAsset,dataVersion\n" +
            "hero_z,\"带,逗号名字\",MID,,700,4500,220,110,3,,heroes/hero_z.webp,1"
        val heroes = CsvImporter.parse(csv)
        assertEquals("带,逗号名字", heroes[0].name)
    }

    @Test
    fun `missing column throws with message`() {
        val csv = "id,name\nhero_x,只有两列"
        try {
            CsvImporter.parse(csv)
            fail("应抛 ImportException")
        } catch (e: CsvImporter.ImportException) {
            assertEquals(true, e.message!!.contains("缺少列"))
        }
    }
}
