package com.bingbing.assistant.algo

import com.bingbing.assistant.data.entity.HeroEntity
import com.bingbing.assistant.data.entity.HeroUsagePriorEntity
import com.bingbing.assistant.model.OpponentWorld
import com.bingbing.assistant.model.SlotState
import com.bingbing.assistant.model.Team
import kotlin.random.Random

/**
 * 隐藏位采样（§5.4.1）。
 *
 * 关键约束（C1）：每队固定隐藏 3 个，但**隐藏位置不固定**。已看到的槽位是确定的，
 * 只对 HIDDEN 槽位采样；同一次采样内全局英雄不可重复（§5.4.1 末行）。
 * 星级：按段位常见分布抽样（5 星 0.7 / 4 星 0.3）。
 *
 * 降级（§13）：该段位无先验数据 → 全英雄均匀采样，能力下降由 Flow 层标注。
 */
class OpponentSampler(
    private val heroMap: Map<String, HeroEntity>,
    priors: List<HeroUsagePriorEntity>,
) {

    private val priorsByBracket: Map<String, List<Pair<String, Float>>> =
        priors.groupBy { it.bracket }
            .mapValues { (_, list) ->
                list.sortedByDescending { it.weight }.take(SimConfig.PRIOR_TOP_K)
                    .map { it.heroId to it.weight }
            }

    /** @return m 套完整对方阵容（每套 = N 队 × 5 英雄） */
    fun sample(
        oppPartial: List<Team>,
        bracket: String,
        m: Int = SimConfig.SAMPLE_WORLDS,
        rng: Random = Random.Default
    ): List<OpponentWorld> {
        val hot = priorsByBracket[bracket]
        val fallbackPool = heroMap.keys.toList()

        val worlds = ArrayList<OpponentWorld>(m)
        repeat(m) {
            val used = HashSet<String>()
            // 关键：先收集全部可见英雄（§5.4.1——可见位是确定的），
            // 否则槽位顺序在前的隐藏位采样可能撞上后面的可见英雄。
            for (team in oppPartial) {
                for (slot in team.slots) {
                    if (slot.state == SlotState.VISIBLE) slot.heroId?.let { used.add(it) }
                }
            }
            val teams = oppPartial.map { team ->
                val heroes = ArrayList<com.bingbing.assistant.model.MyHero>(5)
                for (slot in team.slots) {
                    when (slot.state) {
                        SlotState.VISIBLE -> {
                            val id = slot.heroId ?: continue
                            heroes += com.bingbing.assistant.model.MyHero(id, slot.star ?: 5, 0)
                        }
                        SlotState.HIDDEN -> {
                            val id = pickHidden(used, hot, fallbackPool, rng) ?: continue
                            used.add(id)
                            val star = if (rng.nextFloat() < SimConfig.STAR5_PROB) 5 else 4
                            heroes += com.bingbing.assistant.model.MyHero(id, star, 0)
                        }
                        SlotState.EMPTY -> continue   // 空位：队伍人数减少，模拟按缺员结算
                    }
                }
                heroes
            }
            worlds += teams
        }
        return worlds
    }

    /** 加权无放回抽 1 个；候选耗尽回退全英雄池，再耗尽返回 null（该位空缺） */
    private fun pickHidden(
        used: Set<String>,
        hot: List<Pair<String, Float>>?,
        fallbackPool: List<String>,
        rng: Random
    ): String? {
        val pool = (hot ?: emptyList()).filter { it.first !in used && heroMap.containsKey(it.first) }
        if (pool.isNotEmpty()) {
            val total = pool.sumOf { it.second.toDouble() }
            if (total > 0) {
                var r = rng.nextDouble() * total
                for ((id, w) in pool) {
                    r -= w
                    if (r <= 0) return id
                }
            }
            return pool.last().first
        }
        val rest = fallbackPool.filter { it !in used }
        return if (rest.isEmpty()) null else rest[rng.nextInt(rest.size)]
    }
}
