package com.bingbing.assistant.feature.pool

import com.bingbing.assistant.core.AppGraph
import com.bingbing.assistant.data.entity.HeroEntity
import com.bingbing.assistant.data.entity.MyHeroEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 我的英雄库流程（§7.2）：网格展示 + 勾选/星级 + 快速起步。
 * M1 必须先完成本模块 —— 后面所有推荐都以它为硬约束。
 */
class MyPoolFlow {

    data class PoolUiState(
        val heroes: List<HeroEntity>,
        val owned: Map<String, MyHeroEntity>,
    )

    /** 英雄 + 拥有状态合并流（按 定位/阵营 排序） */
    fun observe(): Flow<PoolUiState> =
        combine(AppGraph.heroRepo.observeAll(), AppGraph.poolRepo.observeAll()) { heroes, owned ->
            PoolUiState(
                heroes = heroes.sortedWith(compareBy({ it.role }, { it.faction ?: "" }, { it.id })),
                owned = owned.associateBy { it.heroId }
            )
        }

    /** 勾选/取消拥有 */
    suspend fun toggle(heroId: String, currentlyOwned: Boolean) {
        if (currentlyOwned) AppGraph.poolRepo.setUnowned(heroId)
        else AppGraph.poolRepo.setOwned(heroId)
    }

    /** 长按改星级（3~5 星，§7.2） */
    suspend fun setStar(heroId: String, star: Int) {
        AppGraph.poolRepo.setStar(heroId, star)
    }

    /** 一键全选 5 星（快速起步） */
    suspend fun selectAllFiveStar(): Int = AppGraph.poolRepo.selectAllFiveStar(AppGraph.heroRepo)
}
