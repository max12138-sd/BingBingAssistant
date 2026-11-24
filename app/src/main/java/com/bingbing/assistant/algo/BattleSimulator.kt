package com.bingbing.assistant.algo

import com.bingbing.assistant.data.entity.HeroEntity
import com.bingbing.assistant.data.entity.HeroSkillEntity
import com.bingbing.assistant.model.Combatant
import com.bingbing.assistant.model.MyHero
import com.bingbing.assistant.model.SkillRuntime
import kotlin.math.max
import kotlin.random.Random

/**
 * 单场战斗模拟（§5.1）。目标不是还原战斗，而是**相对排序正确**。
 *
 * 技能行为映射（§5.1 行动规则）：
 *   HEAL    → 己方血量比例最低者回复 atk*coef（不受减伤）
 *   AOE     → 敌方全体 atk*coef*mitigation
 *   CONTROL → 敌方最前排存活者，跳过下次行动（概率 SimConfig.CONTROL_PROB）
 *   SINGLE  → 敌方最前排存活者 atk*coef*mitigation
 *   BUFF    → TODO(SPEC-MISSING)：规格书未定义 → 暂按普通攻击结算
 * 技能选取：按 AOE > CONTROL > HEAL > SINGLE 优先级取首个 CD 就绪技能，否则普攻（coef=1）。
 * 克制判定：TODO(SPEC-MISSING) 规格书写作 "attacker.counterTags contains target.tag"，
 *           英雄表无独立 tag 字段 → 暂按双方 counterTags 有交集触发。
 */
class BattleSimulator(
    private val heroMap: Map<String, HeroEntity>,
    private val skillMap: Map<String, List<HeroSkillEntity>>,
) {

    // ---------- 构建（MyHero → Combatant） ----------

    fun buildCombatant(hero: MyHero): Combatant? {
        val e = heroMap[hero.heroId] ?: return null   // 库里没有 → 调用方必须先过滤（L4：不猜）
        val starDiff = hero.star - e.rarity
        val atkF = if (starDiff >= 0) 1f + SimConfig.STAR_ATK_GROWTH * starDiff
                   else 1f - SimConfig.STAR_PENALTY * (-starDiff)
        val hpF = if (starDiff >= 0) 1f + SimConfig.STAR_HP_GROWTH * starDiff
                  else 1f - SimConfig.STAR_PENALTY * (-starDiff)
        val defF = if (starDiff >= 0) 1f + SimConfig.STAR_DEF_GROWTH * starDiff
                   else 1f - SimConfig.STAR_PENALTY * (-starDiff)

        val skills = (skillMap[hero.heroId] ?: emptyList())
            .filter { it.unlockStar <= hero.star }
            .map { SkillRuntime(it.name, it.type, it.coef, it.cd) }

        val hp = e.hp * hpF
        return Combatant(
            heroId = e.id,
            atk = e.atk * atkF,
            maxHp = hp,
            hp = hp,
            def = e.def * defF,
            spd = e.spd.toFloat(),
            skills = skills,
            pos = 0,   // 站位由 assemble 填
            isHealer = skills.any { it.type == "HEAL" },
            counterTags = e.tagList.toSet(),
        )
    }

    /** 一支队伍（含站位） → Combatant 列表；库里缺失的英雄跳过（不足按 §13 降级） */
    fun assemble(team: List<MyHero>): List<Combatant> =
        team.mapIndexedNotNull { idx, h -> buildCombatant(h)?.copy(pos = idx) }

    // ---------- 模拟 ----------

    /** 单次模拟的运行时状态（Combatant 本体保持不可变，冷却/血量在 Actor 上） */
    private class Actor(val c: Combatant) {
        var hp: Float = c.hp
        var skip: Boolean = false
        val cds = HashMap<String, Int>()
        fun alive() = hp > 0f
    }

    /** @return true = A 胜 */
    fun simulateOnce(aIn: List<Combatant>, bIn: List<Combatant>, rng: Random): Boolean {
        // 1) 复制双方状态
        val a = aIn.map { Actor(it) }
        val b = bIn.map { Actor(it) }

        for (round in 1..SimConfig.MAX_ROUNDS) {
            // 2) 每回合：按 spd 降序（同速随机）依次行动
            val order = (a + b).filter { it.alive() }
                .map { it to (it.c.spd * 1000f + rng.nextInt(1000)) }
                .sortedByDescending { it.second }
                .map { it.first }

            for (actor in order) {
                if (!actor.alive()) continue
                if (actor.skip) {
                    actor.skip = false
                    continue
                }
                val mine = if (a.contains(actor)) a else b
                val foes = if (a.contains(actor)) b else a
                act(actor, mine, foes, rng)
                if (foes.none { it.alive() }) return a.contains(actor)
            }

            // 回合末：冷却递减
            for (actor in a + b) {
                actor.cds.entries.forEach { it.setValue(it.value - 1) }
                actor.cds.entries.removeAll { it.value <= 0 }
            }

            if (a.none { it.alive() }) return false
            if (b.none { it.alive() }) return true
        }

        // 4) 超时：按（剩余总 hp / 初始总 hp）比值判胜负
        val initA = a.sumOf { it.c.initialHp.toDouble() }.coerceAtLeast(1e-6)
        val initB = b.sumOf { it.c.initialHp.toDouble() }.coerceAtLeast(1e-6)
        val ratioA = a.sumOf { max(it.hp, 0f).toDouble() } / initA
        val ratioB = b.sumOf { max(it.hp, 0f).toDouble() } / initB
        return ratioA >= ratioB
    }

    private fun act(actor: Actor, allies: List<Actor>, enemies: List<Actor>, rng: Random) {
        val ready = pickSkill(actor)
        when {
            ready != null && ready.type == "HEAL" -> {
                actor.cds[ready.name] = ready.cd
                val target = allies.filter { it.alive() }
                    .minByOrNull { it.hp / it.c.maxHp } ?: return
                target.hp = (target.hp + actor.c.atk * ready.coef).coerceAtMost(target.c.maxHp)
            }
            ready != null && ready.type == "AOE" -> {
                actor.cds[ready.name] = ready.cd
                for (enemy in enemies.filter { it.alive() }) {
                    dealDamage(actor.c, enemy, actor.c.atk * ready.coef, rng)
                }
            }
            ready != null && ready.type == "CONTROL" -> {
                actor.cds[ready.name] = ready.cd
                val target = enemies.filter { it.alive() }.minByOrNull { it.c.pos } ?: return
                if (rng.nextFloat() < SimConfig.CONTROL_PROB) target.skip = true
            }
            ready != null && ready.type == "SINGLE" -> {
                actor.cds[ready.name] = ready.cd
                val target = enemies.filter { it.alive() }.minByOrNull { it.c.pos } ?: return
                dealDamage(actor.c, target, actor.c.atk * ready.coef, rng)
            }
            else -> {
                // 普攻 / BUFF 降级：攻击敌方最前排存活者
                val target = enemies.filter { it.alive() }.minByOrNull { it.c.pos } ?: return
                dealDamage(actor.c, target, actor.c.atk, rng)
            }
        }
    }

    private fun pickSkill(actor: Actor): SkillRuntime? =
        actor.c.skills.firstOrNull { it.type == "AOE" && isReady(actor, it) }
            ?: actor.c.skills.firstOrNull { it.type == "CONTROL" && isReady(actor, it) }
            ?: actor.c.skills.firstOrNull { it.type == "HEAL" && isReady(actor, it) }
            ?: actor.c.skills.firstOrNull { it.type == "SINGLE" && isReady(actor, it) }

    private fun isReady(actor: Actor, s: SkillRuntime) = actor.cds[s.name] == null

    private fun dealDamage(attacker: Combatant, target: Actor, base: Float, rng: Random) {
        val crit = if (rng.nextFloat() < SimConfig.CRIT_CHANCE) SimConfig.CRIT_MULT else 1f
        val counter = if (attacker.counterTags.intersect(target.c.counterTags).isNotEmpty())
            SimConfig.COUNTER_MULT else 1f
        target.hp -= base * SimConfig.mitigation(target.c.def) * crit * counter
    }

    // ---------- 对外统计 ----------

    /** 返回 A 对 B 的胜率（0.0..1.0）。无法构建（库里缺人）→ 0.5 半胜，调用方按缺员感知。 */
    fun winRate(a: List<MyHero>, b: List<MyHero>, nSim: Int = 300, rng: Random = Random.Default): Float {
        val ca = assemble(a)
        val cb = assemble(b)
        return winRateCombatants(ca, cb, nSim, rng)
    }

    /** 直接用 Combatant 模拟（矩阵计算复用构建结果，避免每场重复查表） */
    fun winRateCombatants(ca: List<Combatant>, cb: List<Combatant>, nSim: Int, rng: Random): Float {
        if (ca.isEmpty() || cb.isEmpty()) return 0.5f
        var wins = 0
        repeat(nSim) { if (simulateOnce(ca, cb, rng)) wins++ }
        return wins.toFloat() / nSim
    }
}
