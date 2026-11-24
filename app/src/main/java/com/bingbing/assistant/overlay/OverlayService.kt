package com.bingbing.assistant.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.bingbing.assistant.R
import com.bingbing.assistant.core.Logger
import com.bingbing.assistant.core.ServiceBus
import kotlinx.coroutines.launch

/**
 * 悬浮球宿主服务（§7.1）。
 * - TYPE_APPLICATION_OVERLAY 挂浮球 + 面板
 * - 前台服务 + START_STICKY：被系统杀死后重建浮球（T7）
 * - 战斗界面静默：识别到 BATTLE_RUNNING 时不弹任何提示（由 Flow 层控制）
 */
class OverlayService : Service(), LifecycleOwner {

    companion object {
        private const val CHANNEL_ID = "overlay"
        private const val NOTIFICATION_ID = 11

        @Volatile
        private var running = false

        val isRunning: Boolean get() = running

        fun start(context: Context) {
            if (running) return
            context.startForegroundService(Intent(context, OverlayService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }

    private lateinit var wm: WindowManager
    private var ballView: FloatingBallView? = null
    private var ballParams: WindowManager.LayoutParams? = null
    private var panel: PanelController? = null
    private val lifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        createChannel()
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        panel = PanelController(this, wm)
        addBall()

        lifecycleScope.launch {
            ServiceBus.events.collect { event ->
                when (event) {
                    ServiceBus.Event.BALL_CLICKED -> panel?.toggle()
                    else -> Unit   // 扫描事件由 feature Flow 层订阅
                }
            }
        }
        Logger.i("Overlay", "悬浮球已启动")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY   // §7.1 崩溃恢复

    private fun addBall() {
        val dp = resources.displayMetrics.density
        val size = (48 * dp).toInt()
        val ball = FloatingBallView(this)
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        // 贴右边缘 8dp，纵向初始 1/3 屏
        params.x = resources.displayMetrics.widthPixels - size - (8 * dp).toInt()
        params.y = (resources.displayMetrics.heightPixels / 3).toInt()

        ball.listener = object : FloatingBallView.Listener {
            private var lastX = 0f
            private var lastY = 0f
            override fun onDragStart(rawX: Float, rawY: Float) {
                lastX = rawX
                lastY = rawY
            }
            override fun onDragMove(rawX: Float, rawY: Float) {
                params.x += (rawX - lastX).toInt()
                params.y += (rawY - lastY).toInt()
                lastX = rawX
                lastY = rawY
                runCatching { wm.updateViewLayout(ballView, params) }
            }
            override fun onDragEnd(rawX: Float, rawY: Float) = snapToEdge(params)
            override fun onBallClick() = ServiceBus.post(ServiceBus.Event.BALL_CLICKED)
        }

        ballParams = params
        ballView = ball
        wm.addView(ball, params)
    }

    /** 松手吸附最近边缘（8dp 边距），底部避让手势条 24dp（§7.1） */
    private fun snapToEdge(params: WindowManager.LayoutParams) {
        val dp = resources.displayMetrics.density
        val size = (48 * dp).toInt()
        val margin = (8 * dp).toInt()
        val gestureBar = (24 * dp).toInt()
        val maxX = resources.displayMetrics.widthPixels - size
        val maxY = resources.displayMetrics.heightPixels - size - gestureBar
        val leftGap = params.x
        val rightGap = maxX - params.x
        params.x = if (leftGap <= rightGap) margin else maxX - margin
        params.y = params.y.coerceIn(0, maxY)
        runCatching { wm.updateViewLayout(ballView, params) }
        Logger.i("Overlay", "浮球吸附 x=${params.x} y=${params.y}")
    }

    override fun onDestroy() {
        panel?.dismiss()
        ballView?.let { runCatching { wm.removeView(it) } }
        ballView = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        running = false
        Logger.i("Overlay", "悬浮球停止")
        super.onDestroy()
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(R.drawable.ic_stat_capture)
            .setOngoing(true)
            .build()

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.overlay_channel_name), NotificationManager.IMPORTANCE_MIN)
        )
    }
}
