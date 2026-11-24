package com.bingbing.assistant.data.repo

import com.bingbing.assistant.data.db.AppDatabase
import com.bingbing.assistant.data.entity.BattleLogEntity
import kotlinx.coroutines.flow.Flow

/** 战果回填仓储（§7.6）。校准触发逻辑在 algo.Calibrator。 */
class BattleLogRepository(private val db: AppDatabase) {

    suspend fun addResult(
        mode: String,              // ARENA / PEAK3 / PEAK5
        planId: String,
        predictedWinRate: Float,
        actualWins: Int
    ): Long = db.battleLogDao().insert(
        BattleLogEntity(
            mode = mode,
            planId = planId,
            predictedWinRate = predictedWinRate,
            actualWins = actualWins,
            createdAt = System.currentTimeMillis()
        )
    )

    fun observeAll(): Flow<List<BattleLogEntity>> = db.battleLogDao().observeAll()

    fun observeCount(): Flow<Int> = db.battleLogDao().observeCount()

    suspend fun count(): Int = db.battleLogDao().count()

    suspend fun recent(limit: Int): List<BattleLogEntity> = db.battleLogDao().getRecent(limit)
}
