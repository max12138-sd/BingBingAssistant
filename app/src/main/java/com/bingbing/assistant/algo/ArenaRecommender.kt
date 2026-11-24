package com.bingbing.assistant.algo

import com.bingbing.assistant.core.Logger
import com.bingbing.assistant.data.entity.CounterRuleEntity
import com.bingbing.assistant.data.entity.HeroEntity
import com.bingbing.assistant.data.entity.LineupTemplateEntity
import com.bingbing.assistant.model.Assignment
import com.bingbing.assistant.model.MyHero
import com.bingbing.assistant.model.Plan
import kotlin.math.sqrt

/**
 * 竞技场 5v5 推荐（§5.5）：
 *  1) 以 lineup_template 为骨架，用我的英雄库同定位替换缺失位
 *  2) 每个候选跑 winRate(candidate, oppTeam, nSim)
 *  3) 命中 counter_rule 加/减权重（只影响排序，展示胜率仍用模拟值）
 *  4) Top3，胜率输出区间 winRate ± 1.96*sqrt(p(1-p)/nSim)
 *     置信度：字段齐全=HIGH，缺星/装=MID，含 UNKNOWN=LOW
 */
class ArenaRecommender(
    private val simulator: BattleSimulator,
    private val heroMap: Map<String, HeroEntity>,
    private val templates: List<LineupTemplateEntity>,
    private val counterRules: List<CounterRuleEntity>,
) {

    data class Result(
        val plans: List<Plan>,
        val confidence: String,     // HIGH / MID / LOW
    )

    fun recommend(
        myPool: List<MyHero>,
        oppTeam: List<MyHero>,
        nSim: Int = SimConfig.ARENA_NSIM,
        budgetMs: Long? = SimConfig.BUDGET_ARENA_MS,
    ): Result {
        val pool = myPool.filter { heroMap.containsKey(it.heroId) }
        val unknown = myPool.size - pool.size
        if (pool.size < 5) return Result(emptyList(), "LOW")

        val candidates = buildCandidates(pool)
        if (candidates.isEmpty()) return Result(emptyList(), "LOW")

        val oppTags = oppTeam.flatMap { heroMap[it.heroId]?.tagList ?: emptyList() }.toSet()
        val start = System.currentTimeMillis()
        val scored = ArrayList<Triple<List<MyHero>, Float, Float>>(candidates.size)   // 阵容 / 胜率 / 调整分

        candidates.forEachIndexed { idx, team ->
            // 预算：超时把剩余候选的模拟次数降到 1/4（§13 快速模式）
            var ns = nSim
            if (budgetMs != null && idx > 0) {
                val elapsed = System.currentTimeMillis() - start
                val projected = elapsed.toDouble() / idx * (candidates.size - idx)
                if (projected + elapsed > budgetMs * 1.5) {
                    ns = (nSim / 4).coerceAtLeast(100)
                    Logger.w("ArenaRecommender", "超预算，快速模式 nSim=$ns")
                }
            }
            val p = simulator.winRate(team, oppTeam.take(5), ns)
            val tags = team.flatMap { heroMap[it.heroId]?.tagList ?: emptyList() }.toSet()
            val hits = counterRules.filter { rule ->
                rule.attackerTag in tags && rule.targetTag in oppTags
            }
            val adjusted = (p + hits.sumOf { it.modifier.toDouble() }.toFloat()).coerceIn(0.05f, 0.95f)
            scored += Triple(team, p, adjusted)
        }

        val top = scored.sortedByDescending { it.third }.take(SimConfig.ARENA_TOP)
        val confidence = when {
            unknown > 0 -> "LOW"
            pool.any { it.gear == 0 } -> "MID"
            else -> "HIGH"
        }

        val plans = top.mapIndexed { i, (team, p, _) ->
            val err = 1.96f * sqrt((p * (1 - p) / nSim.toDouble())).toFloat()
            val notes = counterRules.filter { rule ->
                rule.attackerTag in team.flatMap { heroMap[it.heroId]?.tagList ?: emptyList() }.toSet() &&
                    rule.targetTag in oppTags
            }.map { it.note }
            Plan(
                id = "arena_top${i + 1}",
                style = "TOP${i + 1}",
                assignments = listOf(
                    Assignment(0, 0, (p - err).coerceIn(0f, 1f), (p + err).coerceIn(0f, 1f))
                ),
                p10Wins = (p - err).coerceIn(0f, 1f),
                expectedWins = p,
                probAtLeastNeed = p,        // 单场：达标即赢 1 场
                probPerfect = p,
                riskNote = notes.joinToString("；").ifBlank {
                    if (confidence == "MID") "部分英雄装备等级未填，区间仅供参考" else ""
                }
            )
        }
        return Result(plans, confidence)
    }

    /** 骨架 + 同定位替换；无模板时贪心补一套 */
    private fun buildCandidates(pool: List<MyHero>): List<List<MyHero>> {
        val byRole = pool.groupBy { heroMap[it.heroId]?.role ?: "MID" }
        fun bestOf(role: String, exclude: Set<String>, n: Int): List<MyHero> =
            (byRole[role] ?: emptyList()).filter { it.heroId !in exclude }
                .sortedByDescending { strength(it) }.take(n)

        val out = LinkedHashSet<List<MyHero>>()
        for (tpl in templates) {
            val roles = tpl.roleList
            if (roles.size != 5) continue
            val team = ArrayList<MyHero>(5)
            val used = HashSet<String>()
            var ok = true
            // 先放库中拥有的核心英雄
            for (core in tpl.coreList) {
                val owned = pool.firstOrNull { it.heroId == core }
                if (owned != null && team.size < 5 && used.add(core)) team += owned
            }
            // 剩余槽位按模板定位取最强
            for ((i, role) in roles.withIndex()) {
                if (team.size > i) continue
                val pick = bestOf(role, used, 1).firstOrNull()
                if (pick == null) { ok = false; break }
                used.add(pick.heroId)
                team += pick
            }
            if (ok && team.size == 5) out += team.toList()
        }

        // 兜底：无模板/模板全缺位 → 贪心一套（1 前排 + 4 最强）
        if (out.isEmpty()) {
            val front = bestOf("FRONT", emptySet(), 1).firstOrNull()
            val used = mutableSetOf<String>()
            val rest = pool.filter { it.heroId !in (front?.let { f -> used.apply { add(f.heroId) } } ?: used) }
                .sortedByDescending { strength(it) }
            val team = buildList {
                front?.let { add(it) }
                addAll(rest.take(5 - size))
            }
            if (team.size == 5) out += team
        }
        return out.toList()
    }

    private fun strength(h: MyHero): Float {
        val e = heroMap[h.heroId] ?: return 0f
        return e.atk * 0.4f + e.hp / 100f * 0.35f + e.def * 0.15f
    }
}
