package com.bingbing.assistant.data.repo

import com.bingbing.assistant.data.db.AppDatabase
import com.bingbing.assistant.data.entity.ActivityEntity
import com.bingbing.assistant.data.import.JsonImporter
import com.bingbing.assistant.data.import.SeedActivity
import kotlinx.coroutines.flow.Flow

/**
 * 活动仓储（§7.5）。
 * 合并策略：同名以新为准；日期冲突时取 dateConfidence 高的一方。
 */
class ActivityRepository(private val db: AppDatabase) {

    fun observeAll(): Flow<List<ActivityEntity>> = db.activityDao().observeAll()

    suspend fun getAll(): List<ActivityEntity> = db.activityDao().getAll()

    suspend fun getByName(name: String): ActivityEntity? = db.activityDao().getByName(name)

    /**
     * 日历扫描结果合并入库（§5.6 第 9 步 / §7.5）。
     * @return 实际写入条数
     */
    suspend fun mergeFromScan(scanned: List<ActivityEntity>, now: Long): Int {
        var written = 0
        for (incoming in scanned) {
            val existing = db.activityDao().getByName(incoming.name)
            val merged = when (existing) {
                null -> incoming
                else -> if (incoming.dateConfidence >= existing.dateConfidence) {
                    incoming.copy(id = existing.id)   // 同名以新为准 + 高置信度日期
                } else {
                    existing.copy(
                        brief = incoming.brief.ifBlank { existing.brief },
                        updatedAt = now
                    )
                }
            }
            db.activityDao().upsertAll(listOf(merged))
            written++
        }
        return written
    }

    /** 手动 JSON 导入（降级路径 §13：日历网格检不出） */
    suspend fun importManualJson(json: String, now: Long): Int {
        val seeds: List<SeedActivity> = JsonImporter.parseActivities(json)
        val entities = seeds.map { it.toEntity(now).copy(source = "MANUAL") }
        db.activityDao().upsertAll(entities)
        return entities.size
    }

    suspend fun clear() = db.activityDao().clear()
}
