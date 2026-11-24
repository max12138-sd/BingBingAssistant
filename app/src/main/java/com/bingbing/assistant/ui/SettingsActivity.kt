package com.bingbing.assistant.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bingbing.assistant.R
import com.bingbing.assistant.core.AppGraph
import com.bingbing.assistant.core.Logger
import com.bingbing.assistant.data.import.CsvImporter
import com.bingbing.assistant.data.import.HeroLibDoctor
import com.bingbing.assistant.databinding.ActivitySettingsBinding
import com.bingbing.assistant.feature.peak.PeakFlow
import kotlinx.coroutines.launch

/**
 * 设置页（§8.1）：数据版本、校准度、导入导出、日志、算法自测。
 * 导入失败给出明确提示（§13 降级，不静默）。
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val csvPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { readUri(it) { text -> importHeroesCsv(text) } }
    }
    private val jsonPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { readUri(it) { text -> importActivitiesJson(text) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnImportCsv.setOnClickListener { csvPicker.launch(arrayOf("text/*", "text/csv")) }
        binding.btnImportJson.setOnClickListener { jsonPicker.launch(arrayOf("application/json", "text/*")) }
        binding.btnSelfTest.setOnClickListener { runSelfTest() }
        binding.btnDoctor.setOnClickListener { runDoctor() }
        binding.btnDumpLog.setOnClickListener { dumpLog() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        lifecycleScope.launch {
            binding.tvDataVersion.text =
                getString(R.string.settings_data_version_fmt, AppGraph.heroRepo.maxDataVersion())
            binding.tvCalibration.text = getString(
                R.string.settings_calibration_fmt,
                com.bingbing.assistant.algo.SimConfig.calibratedBattles
            )
        }
    }

    private fun readUri(uri: Uri, block: (String) -> Unit) {
        runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.onSuccess { text ->
            if (text.isNullOrBlank()) toast("文件为空") else block(text)
        }.onFailure {
            Logger.e("Settings", "读取文件失败", it)
            toast("读取失败：${it.message}")
        }
    }

    private fun importHeroesCsv(text: String) {
        lifecycleScope.launch {
            runCatching { AppGraph.heroRepo.importFromCsv(text) }
                .onSuccess { toast("已导入 $it 名英雄") ; refresh() }
                .onFailure {
                    Logger.e("Settings", "CSV 导入失败", it)
                    toast("CSV 导入失败：${it.message}")
                }
        }
    }

    private fun importActivitiesJson(text: String) {
        lifecycleScope.launch {
            runCatching { AppGraph.activityRepo.importManualJson(text, System.currentTimeMillis()) }
                .onSuccess { toast("已导入 $it 条活动") }
                .onFailure {
                    Logger.e("Settings", "活动导入失败", it)
                    toast("活动导入失败：${it.message}")
                }
        }
    }

    /** M4 验收：构造 6 可见 / 9 隐藏数据跑通算法链路 */
    private fun runSelfTest() {
        lifecycleScope.launch {
            val r = PeakFlow().selfTest()
            val msg = when (r.state) {
                PeakFlow.State.OK ->
                    "OK：${r.plans.size} 套方案，完整度 ${r.visibleCount}/${r.totalCount}，" +
                        "期望胜场 ${"%.2f".format(r.plans.firstOrNull()?.expectedWins ?: 0f)}"
                else -> r.message.ifBlank { "未通过：${r.state}" }
            }
            toast("巅峰自测：$msg")
        }
    }

    /** M1 验收：体检工具报缺图/缺字段项 */
    private fun runDoctor() {
        lifecycleScope.launch {
            val heroes = AppGraph.heroRepo.getAll()
            val skills = AppGraph.heroRepo.getAllSkills()
            val existingIcons = runCatching { assets.list("heroes")?.toSet() ?: emptySet() }
                .getOrDefault(emptySet())
            val report = HeroLibDoctor.examine(heroes, skills) { asset -> asset.substringAfterLast('/') in existingIcons }
            val msg = if (report.healthy) "体检通过：${report.heroCount} 名英雄无问题"
            else "发现问题 ${report.issues.size} 项（首 3 条）：\n" +
                report.issues.take(3).joinToString("\n") { "${it.heroId}: ${it.message}" }
            toast(msg)
        }
    }

    private fun dumpLog() {
        val file = Logger.dumpToFile()
        toast(if (file != null) "日志已导出：${file.absolutePath}" else "日志导出失败")
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
