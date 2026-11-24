package com.bingbing.assistant.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bingbing.assistant.data.entity.ActivityEntity
import com.bingbing.assistant.data.entity.BattleLogEntity
import com.bingbing.assistant.data.entity.CounterRuleEntity
import com.bingbing.assistant.data.entity.HeroEntity
import com.bingbing.assistant.data.entity.HeroSkillEntity
import com.bingbing.assistant.data.entity.HeroUsagePriorEntity
import com.bingbing.assistant.data.entity.LineupTemplateEntity
import com.bingbing.assistant.data.entity.MyHeroEntity

/**
 * 全库单例（规格书 §2：手写单例，不用 Hilt）。
 * schema 导出到 app/schemas/，实体变更时保留迁移历史。
 */
@Database(
    entities = [
        HeroEntity::class,
        HeroSkillEntity::class,
        MyHeroEntity::class,
        HeroUsagePriorEntity::class,
        ActivityEntity::class,
        BattleLogEntity::class,
        LineupTemplateEntity::class,
        CounterRuleEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun heroDao(): HeroDao
    abstract fun myPoolDao(): MyPoolDao
    abstract fun activityDao(): ActivityDao
    abstract fun battleLogDao(): BattleLogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bingbing.db"
                ).build().also { instance = it }
            }
    }
}
