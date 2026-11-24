package com.bingbing.assistant.algo

import com.bingbing.assistant.model.Plan
import com.bingbing.assistant.model.ScoredPlan

/**
 * 三套方案选择（§5.4.3）：
 *   SAFE      = P(≥need) 最大   —— 稳健
 *   BALANCED  = 期望胜场最大    —— 均衡
 *   AGGRESSIVE= P(全胜) 最大    —— 搏杀
 * 三者阵容重叠时用"次优且不同"的方案替换，保证三套确实不同（T9）。
 */
object PlanPicker {

    private fun key(s: ScoredPlan): String =
        s.teams.map { team -> team.map { it.heroId }.sorted().joinToString(",") }
            .sorted().joinToString("|")

    fun pickThreePlan(scored: List<ScoredPlan>, needWins: Int): List<Plan> {
        if (scored.isEmpty()) return emptyList()

        val safe = scored.maxByOrNull { it.probAtLeastNeed }!!
        val balanced = pickDifferent(scored, setOf(safe)) { it.expectedWins }
        val aggressive = pickDifferent(scored, setOf(safe, balanced)) { it.probPerfect }

        val dupNote = if (
            key(safe) == key(balanced) && key(balanced) == key(aggressive)
        ) "候选不足，三套一致" else ""

        return listOf(
            safe.toPlan("SAFE", needWins, dupNote),
            balanced.toPlan("BALANCED", needWins, dupNote),
            aggressive.toPlan("AGGRESSIVE", needWins, dupNote),
        )
    }

    /** 按 metric 取最优且阵容与 taken 不同的方案；全部相同时退化为原最优 */
    private inline fun pickDifferent(
        scored: List<ScoredPlan>,
        taken: Set<ScoredPlan>,
        metric: (ScoredPlan) -> Float
    ): ScoredPlan {
        val takenKeys = taken.map { key(it) }.toSet()
        return scored.filter { key(it) !in takenKeys }.maxByOrNull(metric)
            ?: scored.maxByOrNull(metric)!!
    }

    private fun ScoredPlan.toPlan(style: String, needWins: Int, extraNote: String) = Plan(
        id = "plan_${style.lowercase()}_${teams.hashCode()}",
        style = style,
        assignments = assignments,
        p10Wins = p10Wins,
        expectedWins = expectedWins,
        probAtLeastNeed = probAtLeastNeed,
        probPerfect = probPerfect,
        riskNote = listOf(riskNote, extraNote).filter { it.isNotBlank() }.joinToString("；")
    )
}
