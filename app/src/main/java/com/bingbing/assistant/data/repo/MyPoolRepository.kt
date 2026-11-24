package com.bingbing.assistant.data.repo

import com.bingbing.assistant.data.db.AppDatabase
import com.bingbing.assistant.data.entity.MyHeroEntity
import kotlinx.coroutines.flow.Flow

/**
 * 我的英雄库（§7.2）—— 所有推荐的硬约束数据源。
 * 缺员校验：竞技场 5 人 / 巅峰3队 15 人 / 巅峰5队 25 人。
 */
class MyPoolRepository(private val db: AppDatabase) {

    companion object {
        const val NEED_ARENA = 5
        const val NEED_PEAK3 = 15
        const val NEED_PEAK5 = 25
    }

    fun observeAll(): Flow<List<MyHeroEntity>> = db.myPoolDao().observeAll()

    suspend fun getAll(): List<MyHeroEntity> = db.myPoolDao().getAll()

    suspend fun getById(heroId: String): MyHeroEntity? = db.myPoolDao().getById(heroId)

    suspend fun count(): Int = db.myPoolDao().count()

    /** 勾选拥有：默认 5 星起步（配合"一键全选 5 星"快速上手，§7.2） */
    suspend fun setOwned(heroId: String, star: Int = 5, gearLevel: Int = 0, power: Int? = null) {
        db.myPoolDao().upsert(MyHeroEntity(heroId, star, gearLevel, power))
    }

    suspend fun setStar(heroId: String, star: Int) {
        val cur = db.myPoolDao().getById(heroId) ?: MyHeroEntity(heroId, star, 0, null)
        db.myPoolDao().upsert(cur.copy(star = star.coerceIn(3, 5)))
    }

    suspend fun setUnowned(heroId: String) {
        db.myPoolDao().delete(heroId)
    }

    /** 「一键全选 5 星」：英雄库全员入库为 5 星 */
    suspend fun selectAllFiveStar(heroRepository: HeroRepository): Int {
        val all = heroRepository.getAll()
        db.myPoolDao().upsertAll(all.map { MyHeroEntity(it.id, 5, 0, null) })
        return all.size
    }

    suspend fun isEnoughFor(mode: PoolMode): Boolean = when (mode) {
        PoolMode.ARENA -> count() >= NEED_ARENA
        PoolMode.PEAK3 -> count() >= NEED_PEAK3
        PoolMode.PEAK5 -> count() >= NEED_PEAK5
    }
}

enum class PoolMode { ARENA, PEAK3, PEAK5 }
