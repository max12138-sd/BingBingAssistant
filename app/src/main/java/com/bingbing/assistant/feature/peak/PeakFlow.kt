package com.bingbing.assistant.feature.peak

import com.bingbing.assistant.core.AppGraph
import com.bingbing.assistant.core.Logger
import com.bingbing.assistant.algo.PlanPicker
import com.bingbing.assistant.algo.RobustEvaluator
import com.bingbing.assistant.algo.SimConfig
import com.bingbing.assistant.data.repo.PoolMode
import com.bingbing.assistant.model.MyHero
import com.bingbing.assistant.model.Plan
import com.bingbing.assistant.model.Slot
import com.bingbing.assistant.model.SlotState
import com.bingbing.assistant.model.Team

/**
 * 巅峰方案流程（§7.4）：3 队 / 5 队共用，按 teamCount 分派。
 * 视觉侧（识别我方 N 队 / 对方部分信息）由调用方输入；本类只做算法编排 ——
 * 这使得算法链路可以在没有真机截图的情况下先行验证（M4 验收：构造数据）。
 */
class PeakFlow {

    enum class State { OK, POOL_INSUFFICIENT, INVALID_INPUT, EMPTY }

    data class PeakResult(
        val state: State,
        val plans: List<Plan> = emptyList(),
        val visibleCount: Int = 0,       // 信息完整度分子（§15）
        val totalCount: Int = 0,         // 分母：15 / 25
        val message: String = "",
    )

    /**
     * @param myTeams    我方 N 队（每队 5 人，来自识别或手动）
     * @param oppPartial 对方 N 队：每队 2 个 VISIBLE（含槽位索引）+ 3 个 HIDDEN
     * @param bracket    段位（隐藏位采样先验），如 "LEGEND"
     */
    suspend fun generate(
        teamCount: Int,                       // 3 或 5
        myTeams: List<List<MyHero>>,
        oppPartial: List<Team>,
        bracket: String = "LEGEND",
    ): PeakResult {
        val need = if (teamCount == 3) PoolMode.PEAK3 else PoolMode.PEAK5
        if (!AppGraph.poolRepo.isEnoughFor(need)) {
            return PeakResult(State.POOL_INSUFFICIENT, message = "英雄不足，请先到「我的英雄库」补充")   // T1
        }
        if (myTeams.size != teamCount || oppPartial.size != teamCount ||
            myTeams.any { it.size != 5 } || oppPartial.any { it.slots.size != 5 }
        ) {
            return PeakResult(State.INVALID_INPUT, message = "队伍数量/槽位不完整，未识别完整不强算")   // T4
        }

        val visible = oppPartial.sumOf { it.visibleCount }
        val total = teamCount * 5
        Logger.i("PeakFlow", "生成方案 teamCount=$teamCount 信息完整度 $visible/$total")

        val bundle = AppGraph.buildAlgo()
        val pool = AppGraph.poolRepo.getAll().map { MyHero(it.heroId, it.star, it.gearLevel) }

        val candidates = if (teamCount == 3) {
            bundle.generator.generate3(pool, topN = 200)
        } else {
            bundle.generator.generate5(pool, topN = 80)
        }
        if (candidates.isEmpty()) {
            return PeakResult(State.EMPTY, message = "候选分队生成失败（英雄库数据异常？）")
        }

        val worlds = bundle.sampler.sample(oppPartial, bracket, m = SimConfig.SAMPLE_WORLDS)
        val options = RobustEvaluator.EvalOptions(
            needWins = if (teamCount == 3) 2 else 3,          // E1：5 队先按 ≥3 胜，参数化可改
            nSimPerPair = 120,
            budgetMs = if (teamCount == 3) SimConfig.BUDGET_PEAK3_MS else SimConfig.BUDGET_PEAK5_MS,
        )
        val scored = bundle.evaluator.evaluate(candidates, worlds, options)
        val plans = PlanPicker.pickThreePlan(scored, needWins = options.needWins)
        if (plans.isEmpty()) return PeakResult(State.EMPTY, message = "未产生方案")

        return PeakResult(State.OK, plans, visible, total)
    }

    /**
     * M4 验收自测（T3/T9 辅助）：构造"6 可见 / 9 隐藏、隐藏位在 2/4/5"的场景。
     * 设置页可触发，用于真机冒烟；同样被单元测试调用。
     */
    suspend fun selfTest(): PeakResult {
        val pool = AppGraph.poolRepo.getAll()
        if (pool.size < 15) return PeakResult(State.POOL_INSUFFICIENT, message = "英雄不足（<15）")
        val ids = pool.map { MyHero(it.heroId, it.star, it.gearLevel) }

        // 我方 3 队
        val myTeams = listOf(ids.subList(0, 5), ids.subList(5, 10), ids.subList(10, 15))

        // 对方 3 队：每队可见 2（0、3 位），隐藏 3（1、2、4 位 —— 非连续，覆盖 T3）
        val oppIds = ids.take(15).map { it.heroId }
        fun partialTeam(offset: Int) = Team(
            listOf(
                Slot(0, SlotState.VISIBLE, oppIds[offset + 0], 5),
                Slot(1, SlotState.HIDDEN),
                Slot(2, SlotState.HIDDEN),
                Slot(3, SlotState.VISIBLE, oppIds[offset + 3], 5),
                Slot(4, SlotState.HIDDEN),
            )
        )
        val opp = listOf(
            partialTeam(0), partialTeam(5), partialTeam(10)
        )
        return generate(teamCount = 3, myTeams = myTeams, oppPartial = opp)
    }
}
