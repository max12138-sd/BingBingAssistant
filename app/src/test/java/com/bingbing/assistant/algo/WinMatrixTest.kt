package com.bingbing.assistant.algo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** §5.2 胜率矩阵与对位求解 */
class WinMatrixTest {

    @Test
    fun `bestMatching finds optimal permutation`() {
        val matrix = arrayOf(
            floatArrayOf(0.2f, 0.8f, 0.3f),   // 我1 队
            floatArrayOf(0.7f, 0.4f, 0.9f),   // 我2 队
            floatArrayOf(0.5f, 0.6f, 0.1f),   // 我3 队
        )
        // 最优：我1→对2（0.8），我2→对3（0.9），我3→对1（0.5）→ 期望 2.2
        val (perm, exp) = WinMatrix(Fixture.buildSimulator()).bestMatching(matrix)
        assertEquals(intArrayOf(1, 2, 0).toList(), perm.toList())
        assertEquals(2.2f, exp, 1e-4f)
    }

    @Test
    fun `identity best when diagonal dominates`() {
        val matrix = arrayOf(
            floatArrayOf(0.9f, 0.1f, 0.1f),
            floatArrayOf(0.1f, 0.9f, 0.1f),
            floatArrayOf(0.1f, 0.1f, 0.9f),
        )
        val (perm, exp) = WinMatrix(Fixture.buildSimulator()).bestMatching(matrix)
        assertTrue(perm.contentEquals(intArrayOf(0, 1, 2)))
        assertEquals(2.7f, exp, 1e-4f)
    }

    @Test
    fun `single pair matrix`() {
        val m = arrayOf(floatArrayOf(0.66f))
        val (perm, exp) = WinMatrix(Fixture.buildSimulator()).bestMatching(m)
        assertEquals(0, perm[0])
        assertEquals(0.66f, exp, 1e-4f)
    }
}
