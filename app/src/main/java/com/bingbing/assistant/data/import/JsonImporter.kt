package com.bingbing.assistant.data.import

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * JSON 导入器（规格书 §3 data/import）。
 * 用 org.json（Android 内置，不加第三方依赖）。解析失败抛 ImportException，带行级上下文。
 * 纯函数、无 Android 依赖，可 JVM 单测。
 */
object JsonImporter {

    class ImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

    // ---- heroes.json ----
    fun parseHeroes(json: String): List<SeedHero> = parseArray(json, "heroes") { o ->
        SeedHero(
            id = o.requiredString("id"),
            name = o.requiredString("name"),
            role = o.requiredString("role").uppercase(),
            faction = o.optStringOrNull("faction"),
            atk = o.requiredInt("atk"),
            hp = o.requiredInt("hp"),
            def = o.requiredInt("def"),
            spd = o.requiredInt("spd"),
            rarity = o.requiredInt("rarity"),
            counterTags = o.optString("counterTags", ""),
            iconAsset = o.optString("iconAsset", "heroes/${o.optString("id")}.webp"),
            dataVersion = o.optInt("dataVersion", 1),
            skills = o.optJSONArray("skills")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val s = arr.getJSONObject(i)
                    SeedSkill(
                        name = s.requiredString("name"),
                        type = s.requiredString("type").uppercase(),
                        coef = s.requiredDouble("coef").toFloat(),
                        cd = s.optInt("cd", 0),
                        unlockStar = s.optInt("unlockStar", 3)
                    )
                }
            } ?: emptyList()
        )
    }

    // ---- activities.json（手动导入同格式）----
    fun parseActivities(json: String): List<SeedActivity> = parseArray(json, "activities") { o ->
        SeedActivity(
            name = o.requiredString("name"),
            brief = o.optString("brief", ""),
            start = o.requiredString("start"),
            end = o.requiredString("end"),
            priority = o.optString("priority", "MID").uppercase(),
            source = o.optString("source", "MANUAL").uppercase()
        )
    }

    // ---- templates.json ---- TODO(SPEC-MISSING)：规格书未给格式，以下为约定格式
    fun parseTemplates(json: String): List<SeedTemplate> = parseArray(json, "templates") { o ->
        SeedTemplate(
            id = o.requiredString("id"),
            name = o.requiredString("name"),
            roles = o.requiredArray("roles").toStringList().map { it.uppercase() },
            coreHeroIds = o.optJSONArray("coreHeroIds")?.toStringList() ?: emptyList(),
            tags = o.optJSONArray("tags")?.toStringList() ?: emptyList(),
            dataVersion = o.optInt("dataVersion", 1)
        )
    }

    // ---- counter_rules.json ---- TODO(SPEC-MISSING)：同上，约定格式
    fun parseCounterRules(json: String): List<SeedCounterRule> = parseArray(json, "counter_rules") { o ->
        SeedCounterRule(
            id = o.requiredString("id"),
            attackerTag = o.requiredString("attackerTag"),
            targetTag = o.requiredString("targetTag"),
            modifier = o.requiredDouble("modifier").toFloat(),
            note = o.optString("note", "")
        )
    }

    // ---- usage_priors.json ----
    fun parseUsagePriors(json: String): List<SeedUsagePrior> = parseArray(json, "usage_priors") { o ->
        SeedUsagePrior(
            bracket = o.requiredString("bracket").uppercase(),
            heroId = o.requiredString("heroId"),
            weight = o.requiredDouble("weight").toFloat()
        )
    }

    // ---------------- 内部 ----------------

    private inline fun <T> parseArray(
        json: String,
        what: String,
        map: (JSONObject) -> T
    ): List<T> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i -> map(arr.getJSONObject(i)) }
    } catch (e: JSONException) {
        throw ImportException("种子数据($what) JSON 结构非法：${e.message}", e)
    } catch (e: ImportException) {
        throw e
    } catch (e: IllegalArgumentException) {
        throw ImportException("种子数据($what) 字段缺失或类型错误：${e.message}", e)
    }

    private fun JSONObject.requiredString(key: String): String {
        val v = optString(key, "")
        if (v.isBlank()) throw IllegalArgumentException("缺少必填字段 $key")
        return v
    }

    private fun JSONObject.requiredInt(key: String): Int {
        if (!has(key)) throw IllegalArgumentException("缺少必填字段 $key")
        return getInt(key)
    }

    private fun JSONObject.requiredDouble(key: String): Double {
        if (!has(key)) throw IllegalArgumentException("缺少必填字段 $key")
        return getDouble(key)
    }

    private fun JSONObject.requiredArray(key: String): JSONArray {
        if (!has(key)) throw IllegalArgumentException("缺少必填字段 $key")
        return getJSONArray(key)
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        val v = optString(key, "")
        return v.ifBlank { null }
    }

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }
}
