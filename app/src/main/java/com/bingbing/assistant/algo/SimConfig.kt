package com.bingbing.assistant.algo

import com.bingbing.assistant.data.entity.BattleLogEntity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 模拟参数单例（§5.1 末注）：所有调参常数集中于此，禁止散落。
 * Calibrator（§7.6）只允许改 mitigationConst —— 单点修正，避免过拟合。
 */
object SimConfig {

    // ---- §5.1 战斗常数 ----
    const val MITIGATION_CONST_DEFAULT = 600f   // mitigation = 1 - def/(def+K)
    const val CRIT_CHANCE = 0.15f
    const val CRIT_MULT = 1.5f
    const val COUNTER_MULT = 1.15f
    const val MAX_ROUNDS = 30
    const val CONTROL_PROB = 0.35f              // §5.1 控制命中概率

    // ---- 星级成长（TODO(SPEC-MISSING)：规格书未定义，按线性近似，校准后可改）----
    const val STAR_ATK_GROWTH = 0.10f           // 每高 1 星
    const val STAR_HP_GROWTH = 0.12f
    const val STAR_DEF_GROWTH = 0.06f
    const val STAR_PENALTY = 0.05f              // 低于初始星级每星衰减

    // ---- §5.3.2 模拟退火 ----
    const val SA_ITERATIONS = 3000
    const val SA_T_START = 1.0f
    const val SA_T_END = 0.01f

    // ---- §5.4 采样 ----
    const val SAMPLE_WORLDS = 40                // 对手采样套数
    const val PRIOR_TOP_K = 30                  // 段位热度候选池
    const val STAR5_PROB = 0.7f                 // 隐藏位 5 星概率（其余 4 星）

    // ---- §5.5 竞技场 ----
    const val ARENA_NSIM = 2000
    const val ARENA_TOP = 3

    // ---- 性能预算（§10，麒麟 980）----
    const val BUDGET_PEAK3_MS = 6000L
    const val BUDGET_PEAK5_MS = 10000L
    const val BUDGET_ARENA_MS = 3000L

    /** 校准可变项：mitigation 常数（§7.6 只调 1~2 个系数） */
    @Volatile
    var mitigationConst: Float = MITIGATION_CONST_DEFAULT
        private set

    /** 校准学习场数（设置页"模型校准度：已学习 N 场"） */
    @Volatile
    var calibratedBattles: Int = 0
        private set

    fun mitigation(def: Float): Float = 1f - def / (def + mitigationConst)

    /**
     * §7.6 校准：每累积 30 条战果做一次简单梯度修正。
     * 模型偏乐观（实际胜率 < 预测）→ 减小 K（减伤更强 → 伤害更低）→ 预测下调。
     * TODO(SPEC-MISSING)：规格书未给公式，先按 ±30 步长线性修正，夹在 [400, 900]。
     */
    fun applyCalibration(logs: List<BattleLogEntity>): Boolean {
        if (logs.size < 30) return false
        val predicted = logs.map { it.predictedWinRate }.average().toFloat()
        val actual = logs.map { it.actualWins }.average()
        val error = actual - predicted
        if (abs(error) < 0.02f) {
            calibratedBattles = max(calibratedBattles, logs.size)
            return false
        }
        val step = if (error < 0) -30f else 30f
        mitigationConst = min(900f, max(400f, mitigationConst + step))
        calibratedBattles = max(calibratedBattles, logs.size)
        return true
    }

    /** 供测试/维护恢复默认 */
    fun reset() {
        mitigationConst = MITIGATION_CONST_DEFAULT
        calibratedBattles = 0
    }
}
