package com.bingbing.assistant.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.bingbing.assistant.data.entity.BattleLogEntity
import kotlinx.coroutines.flow.Flow

/** 战果回填（§7.6）：记录 + 校准统计。 */
@Dao
interface BattleLogDao {

    @Insert
    suspend fun insert(log: BattleLogEntity): Long

    @Query("SELECT * FROM battle_log ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BattleLogEntity>>

    @Query("SELECT * FROM battle_log ORDER BY createdAt DESC")
    suspend fun getAll(): List<BattleLogEntity>

    @Query("SELECT COUNT(*) FROM battle_log")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM battle_log")
    suspend fun count(): Int

    /** §7.6 校准：取最近 30 条用于梯度修正 */
    @Query("SELECT * FROM battle_log ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<BattleLogEntity>
}
