package com.bingbing.assistant.algo

import com.bingbing.assistant.data.entity.HeroEntity
import com.bingbing.assistant.model.MyHero
import com.bingbing.assistant.model.TeamPlan
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * 分队候选生成（§5.3）。
 *
 * 解析式评分（§5.3.1）：
 *   score = Σ_team ( teamAtk*0.4 + teamHp/100*0.35 + teamDef*0.15 + synergyBonus*0.1 )
 *   synergyBonus：同阵营 ≥3 人 +5；纯输出无前排 -10
 *
 * TODO(SPEC-MISSING)：池大于所需人数时，按单项强度取前 15/25 名再分队；gear/power 不参与解析评分。
 */
class CandidateGenerator(private val heroMap: Map<String, HeroEntity>) {

    /** 每个英雄的解析评分项（预计算） */
    private data class HeroStats(
        val hero: MyHero,
        val atk: Float,
        val hp: Float,
        val def: Float,
        val role: String,
        val faction: String?,
    ) {
        val soloScore: Float get() = atk * 0.4f + hp / 100f * 0.35f + def * 0.15f
    }

    private fun stats(pool: List<MyHero>): List<HeroStats> = pool.mapNotNull { h ->
        heroMap[h.heroId]?.let {
            HeroStats(h, it.atk.toFloat(), it.hp.toFloat(), it.def.toFloat(), it.role, it.faction)
        }
    }

    private fun takeTop(pool: List<MyHero>, n: Int): List<MyHero> {
        if (pool.size <= n) return pool
        return stats(pool).sortedByDescending { it.soloScore }.take(n).map { it.hero }
    }

    private fun teamScore(members: List<HeroStats>): Float {
        val atk = members.sumOf { it.atk.toDouble() }.toFloat()
        val hp = members.sumOf { it.hp.toDouble() }.toFloat()
        val def = members.sumOf { it.def.toDouble() }.toFloat()
        var synergy = 0f
        val factions = members.mapNotNull { it.faction }.groupingEach { it }
        if (factions.any { it >= 3 }) synergy += 5f
        if (members.none { it.role == "FRONT" }) synergy -= 10f   // 纯输出无前排
        return atk * 0.4f + hp / 100f * 0.35f + def * 0.15f + synergy * 0.1f
    }

    private fun planScore(teams: List<List<HeroStats>>): Float = teams.sumOf { teamScore(it).toDouble() }.toFloat()

    // ---------- 5.3.1 三队：全枚举（去重，忽略队伍顺序） ----------

    fun generate3(myPool: List<MyHero>, topN: Int = 200): List<TeamPlan> {
        val pool = takeTop(myPool, 15)
        if (pool.size < 15) return emptyList()   // T1：英雄不足，由 Flow 层提示
        val st = stats(pool)
        val full = (1 shl 15) - 1

        // 预计算所有 5 人子集的评分（掩码 → 评分/成员）
        data class Sub(val mask: Int, val score: Float, val members: List<HeroStats>)
        val subsets = HashMap<Int, Sub>()
        fun combos(start: Int, chosen: List<HeroStats>, mask: Int) {
            if (chosen.size == 5) {
                subsets[mask] = Sub(mask, teamScore(chosen), chosen.toList())
                return
            }
            var i = start
            while (i < st.size) {
                combos(i + 1, chosen + st[i], mask or (1 shl i))
                i++
            }
        }
        combos(0, emptyList(), 0)

        val out = ArrayList<TeamPlan>(subsets.size * 8)
        val all = subsets.values.sortedBy { it.mask }   // 按 mask 升序配对，保证 a<b<c 只收一次
        for (si in 0 until all.size) {
            val a = all[si]
            for (sj in si + 1 until all.size) {
                val b = all[sj]
                if (a.mask and b.mask != 0) continue
                val cMask = full xor a.mask xor b.mask
                val c = subsets[cMask] ?: continue
                if (c.mask < b.mask) continue   // 只在 a<b<c 处收一次，天然去重（忽略队伍顺序）
                val score = a.score + b.score + c.score
                out += TeamPlan(
                    listOf(a.members.map { it.hero }, b.members.map { it.hero }, c.members.map { it.hero }),
                    score
                )
            }
        }
        return out.sortedByDescending { it.score }.take(topN)
    }

    // ---------- 5.3.2 五队：贪心蛇形初始解 + 模拟退火 ----------

    fun generate5(myPool: List<MyHero>, topN: Int = 80, rng: Random = Random.Default): List<TeamPlan> {
        val pool = takeTop(myPool, 25)
        if (pool.size < 25) return emptyList()   // T1：英雄不足
        val st = stats(pool)

        // 1) 初始解：按综合战力降序，蛇形填入 5 队（均衡优先：要 3 胜，堆强不如均衡）
        val sorted = st.sortedByDescending { it.soloScore }
        val teams = Array(5) { mutableListOf<HeroStats>() }
        var round = 0
        var idx = 0
        while (idx < sorted.size) {
            for (t in 0 until 5) {
                if (idx >= sorted.size) break
                val teamIdx = if (round % 2 == 0) t else 4 - t
                teams[teamIdx].add(sorted[idx])
                idx++
            }
            round++
        }

        val best = sortedMapOf<Double, MutableList<List<List<MyHero>>>>()   // score → 方案（去重）
        var current = teams.map { it.toList() }
        fun toMy(t: List<List<HeroStats>>) = t.map { team -> team.map { it.hero } }
        fun collect(t: List<List<HeroStats>>) {
            val key = planScore(t).toDouble()
            val canKey = toMy(t).map { team -> team.map { it.heroId }.sorted().joinToString(",") }
                .sorted().joinToString("|")
            val bucket = best.getOrPut(key) { mutableListOf() }
            if (bucket.none { p ->
                    p.map { it.map { h -> h.heroId }.sorted().joinToString(",") }.sorted()
                        .joinToString("|") == canKey
                } && bucket.size < topN) {
                bucket.add(toMy(t))
            }
            // 控制总内存：分值最差的桶裁剪
            while (best.size > topN * 3) {
                best.remove(best.firstKey())
            }
        }
        collect(current)

        // 2) 模拟退火：3000 次，T 从 1.0 线性降到 0.01
        var curScore = planScore(current)
        for (it in 0 until SimConfig.SA_ITERATIONS) {
            val t = SimConfig.SA_T_START + (SimConfig.SA_T_END - SimConfig.SA_T_START) *
                it.toFloat() / SimConfig.SA_ITERATIONS

            val ta = rng.nextInt(5)
            var tb = rng.nextInt(5)
            if (tb == ta) tb = (tb + 1 + rng.nextInt(4)) % 5
            val ia = rng.nextInt(current[ta].size)
            val ib = rng.nextInt(current[tb].size)

            // 交换 ia/ib
            val ha = current[ta][ia]
            val hb = current[tb][ib]
            val next = current.mapIndexed { k, team ->
                when (k) {
                    ta -> team.toMutableList().also { l -> l[ia] = hb }
                    tb -> team.toMutableList().also { l -> l[ib] = ha }
                    else -> team
                }
            }
            val nextScore = planScore(next)
            val delta = nextScore - curScore
            val accept = delta > 0 || rng.nextFloat() < exp((delta / t.coerceAtLeast(1e-4f)).toDouble())
            if (accept) {
                current = next
                curScore = nextScore
                collect(current)
            }
        }

        // 3) 收集搜索过程中出现过的较优解，按 score 取 topN（去重已在 collect 做）
        val keys = best.keys.sortedDescending().take(topN)
        val result = ArrayList<TeamPlan>(topN)
        val seen = HashSet<String>()
        for (k in keys) {
            for (plan in best[k] ?: continue) {
                val canKey = plan.map { it.map { h -> h.heroId }.sorted().joinToString(",") }
                    .sorted().joinToString("|")
                if (seen.add(canKey)) {
                    result += TeamPlan(plan, k.toFloat())
                }
            }
        }
        return result.take(topN)
    }

    /** 辅助：同值分组计数（避免引入 groupingBy 的装箱开销） */
    private inline fun <T> List<T>.groupingEach(block: (T) -> T): List<Int> =
        groupBy(block).map { it.value.size }
}
