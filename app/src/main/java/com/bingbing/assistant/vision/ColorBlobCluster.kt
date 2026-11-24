package com.bingbing.assistant.vision

import android.graphics.Bitmap

/**
 * 活动色块聚类（§5.6 第 5 步）：cell 主色统计 → HSV 众数 → 聚成 k 种"活动色"。
 */
object ColorBlobCluster {

    data class Blob(
        val hue: Float,
        val sat: Float,
        val val_: Float,
        val memberCells: List<Pair<Int, Int>>   // (row, col)
    )

    /**
     * 输入每个格子的主色（HSV），输出 k 个色块簇（k 由色距自适应，上限 8）。
     */
    fun cluster(cellColors: List<Pair<Pair<Int, Int>, FloatArray>>): List<Blob> {
        // TODO(M5): 色距阈值 15°（Hue 环形距离）+ S/V 欧氏 0.15，单遍凝聚聚类即可（格子数 ≤ 42）
        return emptyList()
    }
}
