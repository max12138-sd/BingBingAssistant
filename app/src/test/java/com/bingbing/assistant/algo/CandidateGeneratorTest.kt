package com.bingbing.assistant.algo

import com.bingbing.assistant.data.entity.HeroEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** §5.3 分队候选生成 */
class CandidateGeneratorTest {

    private fun generator(): CandidateGenerator {
        val heroes = (1..25).map { i ->
            val role = when (i % 3) { 0 -> "FRONT"; 1 -> "MID"; else -> "BACK" }
            Fixture.hero(
                "hero_$i", role = role,
                faction = if (i % 5 == 0) "机械" else "元素",
                atk = 600 + i * 20, hp = 4000 + i * 150, def = 200 + i * 10
            )
        }.associateBy { it.id }
        return CandidateGenerator(heroes)
    }

    @Test
    fun `generate3 enumerates disjoint teams of 5`() {
        val plans = generator().generate3(Fixture.pool(15), topN = 200)
        assertTrue("应有候选方案", plans.isNotEmpty())
        assertTrue("候选数 ≤ 200", plans.size <= 200)
        for (plan in plans.take(20)) {
            assertEquals(3, plan.teams.size)
            val all = plan.teams.flatten().map { it.heroId }
            assertEquals("15 人无重复", 15, all.toSet().size)
            plan.teams.forEach { assertEquals(5, it.size) }
        }
        // 评分降序
        val scores = plans.map { it.score }
        assertEquals(scores.sortedDescending(), scores)
    }

    @Test
    fun `generate3 with insufficient pool returns empty`() {
        assertTrue(generator().generate3(Fixture.pool(14), topN = 200).isEmpty())   // T1
    }

    @Test
    fun `generate5 produces balanced 5x5 via annealing`() {
        val plans = generator().generate5(Fixture.pool(25), topN = 80, rng = Random(42))
        assertTrue(plans.isNotEmpty())
        assertTrue(plans.size <= 80)
        for (plan in plans.take(10)) {
            assertEquals(5, plan.teams.size)
            val all = plan.teams.flatten().map { it.heroId }
            assertEquals("25 人无重复", 25, all.toSet().size)
            plan.teams.forEach { assertEquals(5, it.size) }
        }
    }

    @Test
    fun `pool larger than needed gets capped`() {
        val plans = generator().generate3(Fixture.pool(25), topN = 100)
        assertTrue(plans.isNotEmpty())   // 取前 15 名再分队，不崩溃
    }
}
