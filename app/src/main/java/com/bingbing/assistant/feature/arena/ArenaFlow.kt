package com.bingbing.assistant.feature.arena

import com.bingbing.assistant.core.AppGraph
import com.bingbing.assistant.core.Logger
import com.bingbing.assistant.data.repo.PoolMode
import com.bingbing.assistant.model.MyHero
import com.bingbing.assistant.model.Plan

/**
 * 竞技场流程（§7.3）：
 * 点入口 → 判定界面 → 截屏识别我方5+对方5 → UNKNOWN 修正 → 推荐 Top3 → 展示 → 存记录。
 * 视觉识别（M3）未就绪时走降级：识别步骤由调用方提供输入或返回 NEED_RECOGNITION。
 */
class ArenaFlow {

    enum class State { OK, POOL_INSUFFICIENT, NEED_RECOGNITION, WRONG_SCREEN, EMPTY }

    data class ArenaResult(
        val state: State,
        val plans: List<Plan> = emptyList(),
        val confidence: String = "LOW",     // HIGH / MID / LOW（§5.5）
        val unknownSlots: List<Int> = emptyList(),
        val message: String = "",
    )

    /**
     * 用识别结果推荐。
     * @param myTeam   我方 5 人（识别或修正面板补全后）
     * @param oppTeam  对方 5 人；含 UNKNOWN 时先走修正面板，补不全则不推荐（L4）
     */
    suspend fun recommend(myTeam: List<MyHero>, oppTeam: List<MyHero>): ArenaResult {
        if (!AppGraph.poolRepo.isEnoughFor(PoolMode.ARENA)) {
            return ArenaResult(State.POOL_INSUFFICIENT, message = "英雄不足，请先到「我的英雄库」补充")   // T1
        }
        if (myTeam.size != 5 || oppTeam.size != 5 ||
            oppTeam.any { AppGraph.heroMap()[it.heroId] == null }
        ) {
            return ArenaResult(State.NEED_RECOGNITION, message = "存在未识别英雄（UNKNOWN），请修正后重试")   // T2
        }

        val bundle = AppGraph.buildAlgo()
        val pool = AppGraph.poolRepo.getAll().map { MyHero(it.heroId, it.star, it.gearLevel) }
        val result = bundle.arena.recommend(pool, oppTeam)
        if (result.plans.isEmpty()) {
            return ArenaResult(State.EMPTY, message = "候选阵容生成失败（模板/英雄库数据不足）")
        }
        Logger.i("ArenaFlow", "推荐 Top${result.plans.size} confidence=${result.confidence}")
        return ArenaResult(State.OK, result.plans, result.confidence)
    }
}
