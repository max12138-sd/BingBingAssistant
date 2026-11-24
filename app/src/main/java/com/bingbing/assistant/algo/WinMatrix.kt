package com.bingbing.assistant.algo

import com.bingbing.assistant.model.MyHero
import kotlin.random.Random

/**
 * 胜率矩阵与对位求解（§5.2）。
 * 核心思想：先算「我方 i 队 vs 对方 j 队」成对胜率得 N×N 矩阵（模拟次数 N² 而非 N!），
 * 再在对位全排列里找期望胜场最大的匹配。
 */
class WinMatrix(private val simulator: BattleSimulator) {

    /**
     * @return matrix[i][j] = 我方第 i 队胜对方第 j 队的胜率
     */
    fun buildMatrix(
        myTeams: List<List<MyHero>>,
        oppTeams: List<List<MyHero>>,
        nSim: Int,
        rng: Random = Random.Default
    ): Array<FloatArray> {
        val matrix = Array(myTeams.size) { FloatArray(oppTeams.size) }
        // 预构建 Combatant：每队只查表一次
        val myC = myTeams.map { simulator.assemble(it) }
        val oppC = oppTeams.map { simulator.assemble(it) }
        for (i in myTeams.indices) {
            for (j in oppTeams.indices) {
                matrix[i][j] = simulator.winRateCombatants(myC[i], oppC[j], nSim, rng)
            }
        }
        return matrix
    }

    /**
     * 枚举全排列（N=3 → 6 种；N=5 → 120 种），返回期望胜场最大的对位。
     * @return Pair(perm, expectedWins)，perm[k] = 我方第 k 队应对对方第 perm[k] 队
     */
    fun bestMatching(matrix: Array<FloatArray>): Pair<IntArray, Float> {
        val n = matrix.size
        require(n == matrix.firstOrNull()?.size) { "矩阵需为方阵" }
        require(n in 1..8) { "对位规模异常: $n" }

        var bestPerm = IntArray(n) { it }
        var bestExp = Float.NEGATIVE_INFINITY
        val perm = IntArray(n) { it }

        // 字典序全排列（Heap 算法亦可；N≤5 规模小，直接交换法）
        fun find(start: Int) {
            if (start == n) {
                var exp = 0f
                for (k in 0 until n) exp += matrix[k][perm[k]]
                if (exp > bestExp) {
                    bestExp = exp
                    bestPerm = perm.copyOf()
                }
                return
            }
            for (i in start until n) {
                perm[i] = perm[start].also { perm[start] = perm[i] }
                find(start + 1)
                perm[i] = perm[start].also { perm[start] = perm[i] }
            }
        }
        find(0)
        return bestPerm to bestExp
    }
}
