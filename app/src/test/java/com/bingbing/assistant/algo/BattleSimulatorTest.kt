package com.bingbing.assistant.algo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** §5.1 战斗模拟：相对排序正确性冒烟 */
class BattleSimulatorTest {

    @Test
    fun `stronger team beats weaker team majority of sims`() {
        val sim = Fixture.buildSimulator()
        // 明确构造：强队 hero_25..29（高属性 5 星），弱队 hero_1..5（低属性 3 星）
        val weak = (1..5).map { com.bingbing.assistant.model.MyHero("hero_$it", 3, 0) }
        val strong5 = (25..29).map { com.bingbing.assistant.model.MyHero("hero_$it", 5, 0) }
        val wr = sim.winRate(strong5, weak, nSim = 200, rng = Random(42))
        assertTrue("强队胜率应显著>50%，实际=$wr", wr > 0.6f)
    }

    @Test
    fun `simulateOnce returns within budget without exception`() {
        val sim = Fixture.buildSimulator()
        val a = sim.assemble(Fixture.pool(5))
        val b = sim.assemble((6..10).map { com.bingbing.assistant.model.MyHero("hero_$it", 5, 0) })
        val rng = Random(7)
        repeat(100) { sim.simulateOnce(a, b, rng) }
    }

    @Test
    fun `assemble skips unknown heroes`() {
        val sim = Fixture.buildSimulator()
        val team = Fixture.pool(5) + com.bingbing.assistant.model.MyHero("hero_unknown", 5, 0)
        assertEquals(5, sim.assemble(team).size)
        assertEquals(0, sim.assemble(listOf(com.bingbing.assistant.model.MyHero("ghost", 5, 0))).size)
    }

    @Test
    fun `winRate unknown team falls back to 0_5`() {
        val sim = Fixture.buildSimulator()
        val wr = sim.winRate(
            listOf(com.bingbing.assistant.model.MyHero("ghost", 5, 0)),
            Fixture.pool(5), nSim = 50
        )
        assertEquals(0.5f, wr, 1e-6f)
    }

    @Test
    fun `sim is stateless across runs`() {
        val sim = Fixture.buildSimulator()
        val a = sim.assemble(Fixture.pool(5))
        val b = sim.assemble((6..10).map { com.bingbing.assistant.model.MyHero("hero_$it", 5, 0) })
        val r1 = sim.winRateCombatants(a, b, 300, Random(1))
        val r2 = sim.winRateCombatants(a, b, 300, Random(1))
        assertEquals("同种子两次模拟应一致（无状态泄漏）", r1, r2, 1e-6f)
    }
}
