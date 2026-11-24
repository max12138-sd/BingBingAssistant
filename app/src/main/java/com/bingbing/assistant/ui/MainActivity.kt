package com.bingbing.assistant.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bingbing.assistant.R
import com.bingbing.assistant.core.AppGraph
import com.bingbing.assistant.core.PermissionHelper
import com.bingbing.assistant.databinding.ActivityMainBinding
import com.bingbing.assistant.feature.activity.ActivityFlow
import com.bingbing.assistant.feature.arena.ArenaFlow
import com.bingbing.assistant.feature.peak.PeakFlow
import com.bingbing.assistant.overlay.OverlayService
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 首页（§8.1）：开关悬浮窗、三大功能入口、状态卡。
 * 功能入口在 M2 完成后依赖悬浮面板；此处按钮提供无悬浮球时的直接演示路径。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOverlayToggle.setOnClickListener { toggleOverlay() }
        binding.btnArena.setOnClickListener { runArenaDemo() }
        binding.btnPeak.setOnClickListener { runPeakSelfTest() }
        binding.btnActivity.setOnClickListener { showSevenDaySummary() }
        binding.btnPool.setOnClickListener { startActivity(android.content.Intent(this, MyPoolActivity::class.java)) }
        binding.btnGuide.setOnClickListener { startActivity(android.content.Intent(this, PermissionGuideActivity::class.java)) }
        binding.btnSettings.setOnClickListener { startActivity(android.content.Intent(this, SettingsActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val running = OverlayService.isRunning
        binding.tvOverlayStatus.text = getString(
            if (running) R.string.status_overlay_on else R.string.status_overlay_off
        )
        binding.btnOverlayToggle.text = getString(if (running) R.string.btn_stop_overlay else R.string.btn_start_overlay)
        lifecycleScope.launch {
            val heroCount = AppGraph.heroRepo.getAll().size
            val owned = AppGraph.poolRepo.count()
            binding.tvPoolStatus.text = getString(R.string.status_pool_fmt, heroCount, owned)
        }
    }

    private fun toggleOverlay() {
        if (!PermissionHelper.hasOverlay(this)) {
            startActivity(android.content.Intent(this, PermissionGuideActivity::class.java))
            return
        }
        if (OverlayService.isRunning) {
            OverlayService.stop(this)
        } else {
            OverlayService.start(this)
        }
        binding.root.postDelayed({ refreshStatus() }, 300)
    }

    /** 竞技场演示：识别模块（M3）就绪前提示走面板路径 */
    private fun runArenaDemo() {
        Toast.makeText(this, "竞技场识别在 M3 实装：请先完成权限引导与截图授权", Toast.LENGTH_LONG).show()
    }

    /** 巅峰算法自测（M4 验收：构造 6 可见/9 隐藏数据跑通算法链路） */
    private fun runPeakSelfTest() {
        lifecycleScope.launch {
            val result = PeakFlow().selfTest()
            val msg = when (result.state) {
                PeakFlow.State.OK ->
                    "自测通过：${result.plans.size} 套方案，信息完整度 ${result.visibleCount}/${result.totalCount}\n" +
                        result.plans.joinToString("\n") {
                            "${it.style}：期望胜场 ${"%.2f".format(it.expectedWins)}，达标概率 ${"%.0f".format(it.probAtLeastNeed * 100)}%"
                        }
                else -> result.message.ifBlank { "自测未通过：${result.state}" }
            }
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
        }
    }

    /** 活动演示：种子/导入数据直接分组展示（M5 接入日历识别） */
    private fun showSevenDaySummary() {
        lifecycleScope.launch {
            val view = ActivityFlow().sevenDayView()
            val msg = "进行中 ${view.groups[ActivityFlow.Group.RUNNING]!!.size} 条 / " +
                "即将开始 ${view.groups[ActivityFlow.Group.UPCOMING]!!.size} 条 / " +
                "今日到期 ${view.groups[ActivityFlow.Group.ENDING_TODAY]!!.size} 条" +
                if (view.stale) "\n缓存已过期，待 M5 日历识别刷新" else ""
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
        }
    }
}
