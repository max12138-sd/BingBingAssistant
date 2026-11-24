package com.bingbing.assistant.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bingbing.assistant.data.entity.ActivityEntity
import kotlinx.coroutines.flow.Flow

/** 活动（§7.5）。合并去重策略在 ActivityRepository，DAO 只做存取。 */
@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(activities: List<ActivityEntity>)

    @Query("SELECT * FROM activity ORDER BY startDate")
    fun observeAll(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activity ORDER BY startDate")
    suspend fun getAll(): List<ActivityEntity>

    @Query("SELECT * FROM activity WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): ActivityEntity?

    @Query("DELETE FROM activity")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM activity")
    suspend fun count(): Int
}
