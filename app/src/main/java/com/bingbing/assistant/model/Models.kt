package com.bingbing.assistant.model

/**
 * 运行时模型（规格书 §4.2，字段名照抄）+ §5 算法间传递结构。
 * 集中放在 model 包：algo / vision / feature 三方共用，避免互相依赖。
 */

// ---------- 阵容与槽位（§4.2） ----------

enum class SlotState { VISIBLE, HIDDEN, EMPTY }

data class Slot(
    val index: Int,                    // 0..4，站位序号（0=最前）
    val state: SlotState,
    val heroId: String? = null,
    val star: Int? = null
)

data class Team(val slots: List<Slot>) {
    val visibleCount: Int get() = slots.count { it.state == SlotState.VISIBLE }
}

/** 我方出战单位（5 个一队） */
data class MyHero(val heroId: String, val star: Int, val gear: Int)

/** 我方一支队（§4.2 写作 MyTeam(heroes: List<MyHero>)） */
data class MyTeam(val heroes: List<MyHero>)

// ---------- 方案（§4.2 + §5.4） ----------

data class Assignment(
    val myTeamIdx: Int,
    val oppTeamIdx: Int,
    val winRateLow: Float,
    val winRateHigh: Float
)

data class Plan(
    val id: String,
    val style: String,                 // SAFE / BALANCED / AGGRESSIVE
    val assignments: List<Assignment>,
    val p10Wins: Float,
    val expectedWins: Float,
    val probAtLeastNeed: Float,        // 3队=P(胜≥2)，5队=P(胜≥3)
    val probPerfect: Float,            // 3队=P(胜=3)，5队=P(胜=5)
    val riskNote: String
)

// ---------- 算法内部结构（§5.3 / §5.4） ----------

/** CandidateGenerator 的候选：一套分队方案 + 解析式评分 */
data class TeamPlan(
    val teams: List<List<MyHero>>,
    val score: Float
)

/** 对手世界：一套完整对方 N 队（隐藏位已采样填充） */
typealias OpponentWorld = List<List<MyHero>>

/** RobustEvaluator 的输出：带胜场分布的候选（assignments 按跨世界平均矩阵的最优对位） */
data class ScoredPlan(
    val teams: List<List<MyHero>>,
    val assignments: List<Assignment>,
    val dist: Map<Int, Int>,           // 胜场 -> 出现次数
    val p10Wins: Float,
    val expectedWins: Float,
    val probAtLeastNeed: Float,
    val probPerfect: Float,
    val riskNote: String
)

// ---------- 战斗模拟（§5.1） ----------

data class SkillRuntime(
    val name: String,
    val type: String,                  // SINGLE / AOE / CONTROL / HEAL / BUFF
    val coef: Float,
    val cd: Int
)

data class Combatant(
    val heroId: String,
    val atk: Float,
    val maxHp: Float,
    var hp: Float,
    val def: Float,
    val spd: Float,
    val skills: List<SkillRuntime>,
    val pos: Int,                      // 0=最前
    val isHealer: Boolean = false,
    val counterTags: Set<String> = emptySet()
) {
    var skipNext: Boolean = false      // CONTROL 命中标记
    val initialHp: Float = maxHp
}
