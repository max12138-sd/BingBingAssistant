package com.bingbing.assistant.algo

import com.bingbing.assistant.core.Logger
import com.bingbing.assistant.model.Assignment
import com.bingbing.assistant.model.MyHero
import com.bingbing.assistant.model.OpponentWorld
import com.bingbing.assistant.model.ScoredPlan
import com.bingbing.assistant.model.TeamPlan

/**
 * 鲁棒评估（§5.4.2）：候选方案 × 对手世界 → 胜场分布。
 *
 * 每个方案 × 每套世界：
 *   1) buildMatrix（N² 次 winRate）
 *   2) bestMatching → 该世界的胜场数（胜率>0.5 记 1 胜，§5.4.2）
 * 汇总 dist → p10 / expected / P(≥need) / P(全胜)。
 *
 * 展示对位表：按跨世界**平均矩阵**求最优匹配，区间取该对位在各世界的 min/max。
 *
 * 性能（§13 降级）：预测耗时超预算 1.5× 时自动把 nSimPerPair 降到下限，结果标注"快速模式"。
 */
class RobustEvaluator(
    private val simulator: BattleSimulator,
    private val matrix: WinMatrix,
    /** 英雄 → 定位（FRONT/MID/BACK），由构造方注入，避免反向依赖 DB */
    private val roleOf: (String) -> String,
) {

    data class EvalOptions(
        val needWins: Int,                    // 3队=2，5队=3
        val nSimPerPair: Int = 120,
        val budgetMs: Long? = null,           // null = 不限
        val minNSim: Int = 8,                 // 快速模式底线
    )

    fun evaluate(
        plans: List<TeamPlan>,
        oppSamples: List<OpponentWorld>,
        options: EvalOptions
    ): List<ScoredPlan> {
        val start = System.currentTimeMillis()
        val out = ArrayList<ScoredPlan>(plans.size)
        var fastMode = false
        var nSim = options.nSimPerPair

        for ((planIdx, plan) in plans.withIndex()) {
            if (!fastMode && options.budgetMs != null && planIdx > 0) {
                val elapsed = System.currentTimeMillis() - start
                val projected = elapsed.toDouble() / planIdx * (plans.size - planIdx)
                if (projected + elapsed > options.budgetMs * 1.5) {
                    nSim = options.minNSim
                    fastMode = true
                    Logger.w("RobustEvaluator", "预估超预算，进入快速模式 nSim=$nSim")
                }
            }

            val n = plan.teams.size
            val dist = HashMap<Int, Int>()
            val avgMatrix = Array(n) { FloatArray(n) }
            val minM = Array(n) { FloatArray(n) { Float.MAX_VALUE } }
            val maxM = Array(n) { FloatArray(n) { Float.NEGATIVE_INFINITY } }

            for (world in oppSamples) {
                val m = matrix.buildMatrix(plan.teams, world, nSim)
                for (i in 0 until n) for (j in 0 until n) {
                    avgMatrix[i][j] += m[i][j] / oppSamples.size
                    minM[i][j] = minOf(minM[i][j], m[i][j])
                    maxM[i][j] = maxOf(maxM[i][j], m[i][j])
                }
                val (permW, _) = matrix.bestMatching(m)
                val wins = permW.indices.count { k -> m[k][permW[k]] > 0.5f }
                dist[wins] = (dist[wins] ?: 0) + 1
            }

            val (perm, _) = matrix.bestMatching(avgMatrix)
            val assignments = perm.mapIndexed { k, oppIdx ->
                Assignment(k, oppIdx, minM[k][oppIdx], maxM[k][oppIdx])
            }

            val total = dist.values.sum().coerceAtLeast(1)
            var p10 = 0f
            var cum = 0
            for ((wins, cnt) in dist.toSortedMap()) {
                cum += cnt
                if (cum >= 0.1 * total) {
                    p10 = wins.toFloat()
                    break
                }
            }
            val expected = dist.entries.sumOf { (wins, cnt) -> wins.toDouble() * cnt / total }.toFloat()
            val pAtLeast = dist.entries.filter { it.key >= options.needWins }
                .sumOf { it.value.toDouble() / total }.toFloat()
            val pPerfect = (dist[n] ?: 0).toFloat() / total

            out += ScoredPlan(
                teams = plan.teams,
                assignments = assignments,
                dist = dist,
                p10Wins = p10,
                expectedWins = expected,
                probAtLeastNeed = pAtLeast,
                probPerfect = pPerfect,
                riskNote = riskNote(plan.teams) + if (fastMode) "｜快速模式" else ""
            )
        }
        return out
    }

    /** 组队结构风险（§7.4 方案卡"⚠"行）：前排缺员的队点名 */
    private fun riskNote(teams: List<List<MyHero>>): String {
        val notes = teams.mapIndexedNotNull { idx, team ->
            val fronts = team.count { roleOf(it.heroId) == "FRONT" }
            when {
                fronts == 0 -> "我${idx + 1}队无前排"
                fronts == 1 -> "我${idx + 1}队前排仅1人"
                else -> null
            }
        }
        return notes.joinToString("；")
    }
}
