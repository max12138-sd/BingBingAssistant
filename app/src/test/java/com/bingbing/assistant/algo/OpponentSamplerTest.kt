package com.bingbing.assistant.algo

import com.bingbing.assistant.data.entity.HeroEntity
import com.bingbing.assistant.data.entity.HeroUsagePriorEntity
import com.bingbing.assistant.model.Slot
import com.bingbing.assistant.model.SlotState
import com.bingbing.assistant.model.Team
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** §5.4.1 隐藏位采样：C1 关键约束（隐藏位置不固定、全局不重复） */
class OpponentSamplerTest {

    private fun sampler(): OpponentSampler {
        val heroes = (1..30).map { i ->
            Fixture.hero("hero_$i", role = if (i % 3 == 0) "FRONT" else "MID")
        }.associateBy { it.id }
        val priors = (1..20).map {
            HeroUsagePriorEntity(bracket = "LEGEND", heroId = "hero_$it", weight = 1f - it * 0.03f)
        }
        return OpponentSampler(heroes, priors)
    }

    private fun partial(): List<Team> {
        // 每队可见 0/3 位，隐藏 1/2/4 位（T3：非连续隐藏）
        fun team(v0: String, v3: String) = Team(listOf(
            Slot(0, SlotState.VISIBLE, v0, 5),
            Slot(1, SlotState.HIDDEN),
            Slot(2, SlotState.HIDDEN),
            Slot(3, SlotState.VISIBLE, v3, 5),
            Slot(4, SlotState.HIDDEN),
        ))
        return listOf(team("hero_1", "hero_3"), team("hero_2", "hero_4"), team("hero_5", "hero_6"))
    }

    @Test
    fun `visible slots are preserved exactly`() {
        val worlds = sampler().sample(partial(), "LEGEND", m = 10, rng = Random(1))
        assertEquals(10, worlds.size)
        for (world in worlds) {
            assertEquals(3, world.size)
            // 输出按槽位顺序（0..4）展开：可见位在 index 0 与 3
            assertEquals("slot0 可见位保留", "hero_1", world[0][0].heroId)
            assertEquals("slot3 可见位保留", "hero_3", world[0][3].heroId)
        }
    }

    @Test
    fun `hidden slots sampled to full team`() {
        val worlds = sampler().sample(partial(), "LEGEND", m = 10, rng = Random(2))
        for (world in worlds) {
            for (team in world) {
                assertEquals("每队应补满 5 人（2 可见 + 3 隐藏）", 5, team.size)
            }
        }
    }

    @Test
    fun `no duplicate heroes within one world`() {
        val worlds = sampler().sample(partial(), "LEGEND", m = 20, rng = Random(3))
        for (world in worlds) {
            val all = world.flatten().map { it.heroId }
            assertEquals("全局 15 人不可重复", 15, all.toSet().size)
        }
    }

    @Test
    fun `star distribution only 4 or 5`() {
        val worlds = sampler().sample(partial(), "LEGEND", m = 20, rng = Random(4))
        val stars = worlds.flatten().flatten().map { it.star }
        assertTrue(stars.all { it == 4 || it == 5 })
    }

    @Test
    fun `bracket without priors degrades to uniform sampling`() {
        val worlds = sampler().sample(partial(), "BRONZE", m = 5, rng = Random(5))
        assertEquals(5, worlds.size)
        for (world in worlds) {
            world.forEach { assertEquals(5, it.size) }
        }
    }
}
