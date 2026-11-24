package com.bingbing.assistant.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bingbing.assistant.R
import com.bingbing.assistant.capture.ScreenCaptureService
import com.bingbing.assistant.core.Logger
import com.bingbing.assistant.core.PermissionHelper
import com.bingbing.assistant.core.RomAdapter
import com.bingbing.assistant.databinding.ActivityPermissionGuideBinding

/**
 * 三步权限引导（§8.1）：悬浮窗 / 截图授权（MediaProjection）/ 通知 + ROM 白名单。
 * 华为专项（§9.2）：应用启动管理三项全开、电池优化白名单入口。
 */
class PermissionGuideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionGuideBinding

    private val overlayLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refreshStates() }

    private val captureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                ScreenCaptureService.start(this, result.resultCode, result.data!!)
                binding.root.postDelayed({ refreshStates() }, 500)
            } else {
                Logger.i("Guide", "用户拒绝截图授权")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOverlay.setOnClickListener {
            overlayLauncher.launch(PermissionHelper.overlaySettingsIntent(this))
        }
        binding.btnCapture.setOnClickListener {
            captureLauncher.launch(PermissionHelper.screenCaptureRequestIntent(this))
        }
        binding.btnNotification.setOnClickListener { PermissionHelper.requestNotification(this) }
        binding.btnAutostart.setOnClickListener { launchAutostart() }
        binding.btnBattery.setOnClickListener {
            runCatching { startActivity(PermissionHelper.batterySettingsIntent()) }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStates()
    }

    private fun refreshStates() {
        val overlayOk = PermissionHelper.hasOverlay(this)
        val captureOk = ScreenCaptureService.isRunning
        val notifOk = PermissionHelper.hasNotificationPermission(this)

        bindState(binding.tvOverlayState, overlayOk)
        bindState(binding.tvCaptureState, captureOk)
        bindState(binding.tvNotificationState, notifOk)
    }

    private fun bindState(view: android.widget.TextView, ok: Boolean) {
        view.text = getString(if (ok) R.string.perm_granted else R.string.perm_not_granted)
        view.setTextColor(getColor(if (ok) R.color.success else R.color.risk))
    }

    /** §9.2：华为"应用启动管理 → 手动管理 → 三项全开"；组件失效时回退应用详情页 */
    private fun launchAutostart() {
        val rom = RomAdapter.detect()
        val intent = RomAdapter.autostartIntent(rom)
            ?: RomAdapter.appDetailsIntent(packageName)
        runCatching { startActivity(intent) }
            .onFailure { startActivity(RomAdapter.appDetailsIntent(packageName)) }
    }
}
