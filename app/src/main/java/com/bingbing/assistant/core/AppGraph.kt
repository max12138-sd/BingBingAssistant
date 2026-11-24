package com.bingbing.assistant.core

import android.content.Context
import com.bingbing.assistant.algo.ArenaRecommender
import com.bingbing.assistant.algo.BattleSimulator
import com.bingbing.assistant.algo.CandidateGenerator
import com.bingbing.assistant.algo.OpponentSampler
import com.bingbing.assistant.algo.RobustEvaluator
import com.bingbing.assistant.algo.WinMatrix
import com.bingbing.assistant.data.db.AppDatabase
import com.bingbing.assistant.data.repo.ActivityRepository
import com.bingbing.assistant.data.repo.BattleLogRepository
import com.bingbing.assistant.data.repo.HeroRepository
import com.bingbing.assistant.data.repo.MyPoolRepository
import com.bingbing.assistant.vision.OcrEngine

/**
 * 手写单例依赖容器（§2：不用 Hilt）。
 * 在 App.onCreate 中 init 一次；算法组件按需重建（数据可能被导入更新）。
 */
object AppGraph {

    private lateinit var appContext: Context

    lateinit var db: AppDatabase
        private set

    val heroRepo by lazy { HeroRepository(db) }
    val poolRepo by lazy { MyPoolRepository(db) }
    val activityRepo by lazy { ActivityRepository(db) }
    val battleLogRepo by lazy { BattleLogRepository(db) }

    val ocrEngine by lazy { OcrEngine(appContext) }

    fun init(context: Context) {
        appContext = context.applicationContext
        db = AppDatabase.get(appContext)
    }

    /** 英雄表快照（算法构建用） */
    suspend fun heroMap(): Map<String, com.bingbing.assistant.data.entity.HeroEntity> =
        heroRepo.getAll().associateBy { it.id }

    suspend fun skillMap(): Map<String, List<com.bingbing.assistant.data.entity.HeroSkillEntity>> =
        heroRepo.getAllSkills().groupBy { it.heroId }

    /** 组装整套巅峰/竞技场算法组件 */
    suspend fun buildAlgo(): AlgoBundle {
        val heroes = heroMap()
        val skills = skillMap()
        val sim = BattleSimulator(heroes, skills)
        return AlgoBundle(
            heroMap = heroes,
            simulator = sim,
            matrix = WinMatrix(sim),
            generator = CandidateGenerator(heroes),
            sampler = OpponentSampler(heroes,
                listOf("BRONZE", "SILVER", "GOLD", "LEGEND").flatMap { heroRepo.getUsagePriors(it) }),
            evaluator = RobustEvaluator(sim, WinMatrix(sim)) { heroes[it]?.role ?: "MID" },
            arena = ArenaRecommender(sim, heroes, heroRepo.getAllTemplates(), heroRepo.getAllCounterRules()),
        )
    }

    data class AlgoBundle(
        val heroMap: Map<String, com.bingbing.assistant.data.entity.HeroEntity>,
        val simulator: BattleSimulator,
        val matrix: WinMatrix,
        val generator: CandidateGenerator,
        val sampler: OpponentSampler,
        val evaluator: RobustEvaluator,
        val arena: ArenaRecommender,
    )
}
