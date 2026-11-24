package com.bingbing.assistant.data.import

import com.bingbing.assistant.data.entity.HeroEntity
import com.bingbing.assistant.data.entity.HeroSkillEntity

/**
 * 英雄库体检（M1 验收项：能报出缺图/缺字段项）。
 * 资产存在性通过回调注入，纯逻辑可 JVM 单测。
 */
object HeroLibDoctor {

    enum class IssueType { MISSING_ICON, MISSING_FIELD, INVALID_VALUE, NO_SKILL }

    data class Issue(
        val heroId: String,
        val type: IssueType,
        val message: String
    )

    data class Report(
        val heroCount: Int,
        val issues: List<Issue>
    ) {
        val healthy: Boolean get() = issues.isEmpty()
    }

    private val ROLES = setOf("FRONT", "MID", "BACK")

    fun examine(
        heroes: List<HeroEntity>,
        skills: List<HeroSkillEntity>,
        iconExists: (assetPath: String) -> Boolean
    ): Report {
        val issues = mutableListOf<Issue>()
        val skillHeroIds = skills.map { it.heroId }.toSet()
        val ids = mutableSetOf<String>()

        for (h in heroes) {
            if (!ids.add(h.id)) {
                issues += Issue(h.id, IssueType.MISSING_FIELD, "英雄 ID 重复：${h.id}")
            }
            if (h.name.isBlank()) issues += Issue(h.id, IssueType.MISSING_FIELD, "缺少名称")
            if (h.role !in ROLES) issues += Issue(h.id, IssueType.INVALID_VALUE, "定位非法：${h.role}")
            if (h.atk <= 0 || h.hp <= 0) {
                issues += Issue(h.id, IssueType.INVALID_VALUE, "属性异常 atk=${h.atk} hp=${h.hp}")
            }
            if (h.rarity !in 1..5) issues += Issue(h.id, IssueType.INVALID_VALUE, "星级非法：${h.rarity}")
            if (!iconExists(h.iconAsset)) issues += Issue(h.id, IssueType.MISSING_ICON, "缺头像模板：${h.iconAsset}")
            if (h.id !in skillHeroIds) issues += Issue(h.id, IssueType.NO_SKILL, "未配置任何技能")
        }
        return Report(heroes.size, issues)
    }
}
