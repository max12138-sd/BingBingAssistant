package com.bingbing.assistant.data.import

import com.bingbing.assistant.data.entity.HeroEntity

/**
 * CSV 导入英雄库（规格书 §3 / M1）。
 *
 * 首行必须为表头（顺序可变）：
 * id,name,role,faction,atk,hp,def,spd,rarity,counterTags,iconAsset,dataVersion
 *
 * 支持双引号包裹与 "" 转义；行内逗号需引号包裹。示例见 docs/导入模板-英雄.csv。
 */
object CsvImporter {

    val HEADER = listOf(
        "id", "name", "role", "faction", "atk", "hp", "def", "spd",
        "rarity", "counterTags", "iconAsset", "dataVersion"
    )

    class ImportException(message: String) : Exception(message)

    fun parse(text: String): List<HeroEntity> {
        val lines = text.lineSequence()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) throw ImportException("CSV 内容为空")

        val header = splitCsvLine(lines.first())
        val idx = HEADER.associateWith { header.indexOf(it) }
        val missing = HEADER.filter { idx[it] == -1 }
        if (missing.isNotEmpty()) throw ImportException("CSV 缺少列：${missing.joinToString()}")

        return lines.drop(1).mapIndexedNotNull { rowNo, line ->
            val cols = splitCsvLine(line)
            try {
                fun col(key: String) = cols[idx[key]!!].trim()
                HeroEntity(
                    id = col("id"),
                    name = col("name"),
                    role = col("role").uppercase(),
                    faction = col("faction").ifBlank { null },
                    atk = col("atk").toIntOrNull() ?: 0,
                    hp = col("hp").toIntOrNull() ?: 0,
                    def = col("def").toIntOrNull() ?: 0,
                    spd = col("spd").toIntOrNull() ?: 0,
                    rarity = col("rarity").toIntOrNull() ?: 3,
                    counterTags = col("counterTags"),
                    iconAsset = col("iconAsset").ifBlank { "heroes/${col("id")}.webp" },
                    dataVersion = col("dataVersion").toIntOrNull() ?: 1
                )
            } catch (e: Exception) {
                throw ImportException("第 ${rowNo + 2} 行解析失败：${e.message}")
            }
        }
    }

    /** 标准 CSV 切分：双引号包裹内允许逗号/换行转义，"" 表示字面引号 */
    internal fun splitCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    out.add(sb.toString()); sb.setLength(0)
                }
                else -> sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }
}
