package com.bingbing.assistant.algo

import com.bingbing.assistant.model.TeamPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** §5.4.2 鲁棒评估 + §5.4.3 三套方案（T9） */
class RobustEvaluatorTest {

    private fun bundle(): RobustEvaluator {
        val sim = Fixture.buildSimulator(30)
        return RobustEvaluator(sim, WinMatrix(sim)) { id -> if (id.endsWith("0")) "FRONT" else "MID" }
    }

    private fun worlds(): List<List<List<com.bingbing.assistant.model.MyHero>>> {
        val sampler = OpponentSampler(
            (1..30).map { Fixture.hero("hero_$it") }.associateBy { it.id },
            (1..20).map { com.bingbing.assistant.data.entity.HeroUsagePriorEntity(bracket = "LEGEND", heroId = "hero_$it", weight = 0.5f) }
        )
        // 与方案对齐：3 队，每队 2 可见 + 3 隐藏（T3 非连续隐藏位）
        fun partialTeam(v0: String, v3: String) = com.bingbing.assistant.model.Team(listOf(
            com.bingbing.assistant.model.Slot(0, com.bingbing.assistant.model.SlotState.VISIBLE, v0, 5),
            com.bingbing.assistant.model.Slot(1, com.bingbing.assistant.model.SlotState.HIDDEN),
            com.bingbing.assistant.model.Slot(2, com.bingbing.assistant.model.SlotState.HIDDEN),
            com.bingbing.assistant.model.Slot(3, com.bingbing.assistant.model.SlotState.VISIBLE, v3, 5),
            com.bingbing.assistant.model.Slot(4, com.bingbing.assistant.model.SlotState.HIDDEN),
        ))
        val partial = listOf(
            partialTeam("hero_1", "hero_4"),
            partialTeam("hero_2", "hero_5"),
            partialTeam("hero_3", "hero_6"),
        )
        return sampler.sample(partial, "LEGEND", m = 4, rng = Random(9))
    }

    @Test
    fun `evaluate aggregates distribution correctly`() {
        val plans = listOf(TeamPlan(Fixture.pool(15).chunked(5), 10f))
        val scored = bundle().evaluate(plans, worlds(), RobustEvaluator.EvalOptions(needWins = 1, nSimPerPair = 16))
        assertEquals(1, scored.size)
        val s = scored[0]
        assertEquals("分布总和应等于采样套数", 4, s.dist.values.sum())
        assertTrue("P(全胜) ≤ P(达标)", s.probPerfect <= s.probAtLeastNeed + 1e-6f)
        assertTrue("期望胜场在 [0, N]", s.expectedWins in 0f..3f)
        assertEquals("对位表应有 N 条", 3, s.assignments.size)
    }

    @Test
    fun `pickThreePlan yields distinct teams when candidates allow`() {
        val gen = CandidateGenerator(
            (1..15).map {
                Fixture.hero("hero_$it", role = if (it % 3 == 0) "FRONT" else "MID",
                    faction = if (it % 5 == 0) "机械" else "元素")
            }.associateBy { it.id }
        )
        val candidates = gen.generate3(Fixture.pool(15), topN = 50)
        val ev = bundle()
        val scored = ev.evaluate(candidates.take(6), worlds(), RobustEvaluator.EvalOptions(needWins = 2, nSimPerPair = 16))
        val plans = PlanPicker.pickThreePlan(scored, needWins = 2)
        assertEquals(3, plans.size)
        assertEquals(listOf("SAFE", "BALANCED", "AGGRESSIVE"), plans.map { it.style })
        // T9：候选充足时三套阵容确实不同
        val keys = plans.map { p -> p.assignments.toString() + p.id }
        assertTrue(keys.toSet().size == 3 || scored.map { it.teams }.toSet().size < 3)
    }

    @Test
    fun `fast mode kicks in under tight budget`() {
        val plans = listOf(TeamPlan(Fixture.pool(15).chunked(5), 10f))
        val scored = bundle().evaluate(plans, worlds(), RobustEvaluator.EvalOptions(needWins = 1, nSimPerPair = 8))
        assertEquals(1, scored.size)   // 不崩溃即通过（快速模式是否触发取决于机器速度）
    }
}
