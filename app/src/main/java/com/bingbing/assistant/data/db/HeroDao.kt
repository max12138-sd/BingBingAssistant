package com.bingbing.assistant.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bingbing.assistant.data.entity.CounterRuleEntity
import com.bingbing.assistant.data.entity.HeroEntity
import com.bingbing.assistant.data.entity.HeroSkillEntity
import com.bingbing.assistant.data.entity.HeroUsagePriorEntity
import com.bingbing.assistant.data.entity.LineupTemplateEntity
import kotlinx.coroutines.flow.Flow

/**
 * 英雄主档 + 技能 + 阵营模板 + 克制规则 + 段位热度先验。
 * （规格书 §3 只列 4 个 DAO，模板/规则/先验的查询并入 HeroDao，避免 DAO 碎片化。）
 */
@Dao
interface HeroDao {

    // ---- hero ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeroes(heroes: List<HeroEntity>)

    @Query("SELECT * FROM hero ORDER BY role, id")
    fun observeAll(): Flow<List<HeroEntity>>

    @Query("SELECT * FROM hero ORDER BY role, id")
    suspend fun getAll(): List<HeroEntity>

    @Query("SELECT * FROM hero WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<HeroEntity>

    @Query("SELECT * FROM hero WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): HeroEntity?

    @Query("SELECT COUNT(*) FROM hero")
    suspend fun count(): Int

    @Query("SELECT MAX(dataVersion) FROM hero")
    suspend fun maxDataVersion(): Int?

    // ---- hero_skill ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkills(skills: List<HeroSkillEntity>)

    @Query("SELECT * FROM hero_skill WHERE heroId = :heroId")
    suspend fun getSkills(heroId: String): List<HeroSkillEntity>

    @Query("SELECT * FROM hero_skill")
    suspend fun getAllSkills(): List<HeroSkillEntity>

    // ---- lineup_template ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineupTemplates(templates: List<LineupTemplateEntity>)

    @Query("SELECT * FROM lineup_template")
    suspend fun getAllTemplates(): List<LineupTemplateEntity>

    // ---- counter_rule ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCounterRules(rules: List<CounterRuleEntity>)

    @Query("SELECT * FROM counter_rule")
    suspend fun getAllCounterRules(): List<CounterRuleEntity>

    // ---- hero_usage_prior ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsagePriors(priors: List<HeroUsagePriorEntity>)

    @Query("SELECT * FROM hero_usage_prior WHERE bracket = :bracket ORDER BY weight DESC")
    suspend fun getUsagePriors(bracket: String): List<HeroUsagePriorEntity>

    @Query("SELECT COUNT(*) FROM hero_usage_prior")
    suspend fun countUsagePriors(): Int

    // ---- 维护 ----
    @Query("DELETE FROM hero WHERE id = :heroId")
    suspend fun deleteHero(heroId: String)

    @Query("DELETE FROM hero_skill WHERE heroId = :heroId")
    suspend fun deleteSkills(heroId: String)
}
