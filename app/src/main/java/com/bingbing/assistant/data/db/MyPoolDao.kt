package com.bingbing.assistant.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bingbing.assistant.data.entity.MyHeroEntity
import kotlinx.coroutines.flow.Flow

/** 我的英雄库（§7.2），推荐的硬约束数据源。 */
@Dao
interface MyPoolDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(hero: MyHeroEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(heroes: List<MyHeroEntity>)

    @Query("DELETE FROM my_hero WHERE heroId = :heroId")
    suspend fun delete(heroId: String)

    @Query("DELETE FROM my_hero")
    suspend fun clear()

    @Query("SELECT * FROM my_hero")
    fun observeAll(): Flow<List<MyHeroEntity>>

    @Query("SELECT * FROM my_hero")
    suspend fun getAll(): List<MyHeroEntity>

    @Query("SELECT * FROM my_hero WHERE heroId = :heroId LIMIT 1")
    suspend fun getById(heroId: String): MyHeroEntity?

    @Query("SELECT COUNT(*) FROM my_hero")
    suspend fun count(): Int
}
