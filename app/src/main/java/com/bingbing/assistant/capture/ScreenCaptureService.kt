package com.bingbing.assistant.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.bingbing.assistant.R
import com.bingbing.assistant.core.Logger
import com.bingbing.assistant.core.ServiceBus

/**
 * 截图采集前台服务（M2）。
 * - MediaProjection 免 root 截屏（§2）
 * - 断流自动重连提示（§9.2）：授权失效时发 CAPTURE_LOST，UI 引导重新授权（T6）
 * - 全程本地，无网络（L1）
 */
class ScreenCaptureService : Service() {

    companion object {
        private const val CHANNEL_ID = "capture"
        private const val NOTIFICATION_ID = 10

        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"

        @Volatile
        private var instance: ScreenCaptureService? = null

        val isRunning: Boolean get() = instance != null

        /** 单帧获取入口（FrameGrabber 委托）；服务未运行 → null */
        fun grabFrame(): Bitmap? = instance?.grabOnce()

        fun start(context: Context, resultCode: Int, resultData: Intent) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenCaptureService::class.java))
        }
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var screenW = 0
    private var screenH = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
        val resultData = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        if (resultCode == null || resultData == null || resultCode == Int.MIN_VALUE) {
            Logger.w("Capture", "缺少授权数据，服务退出")
            stopSelf()
            return START_NOT_STICKY
        }

        val metrics: DisplayMetrics = resources.displayMetrics
        screenW = metrics.widthPixels
        screenH = metrics.heightPixels

        // Android 14（targetSdk 34）必须声明 mediaProjection 类型启动前台服务
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )

        handlerThread = HandlerThread("capture").also { it.start() }
        handler = Handler(handlerThread!!.looper)

        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val proj = mpm.getMediaProjection(resultCode, resultData) ?: run {
            Logger.w("Capture", "授权无效")
            stopSelf()
            return START_NOT_STICKY
        }
        projection = proj
        proj.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Logger.w("Capture", "投影停止（授权失效）")
                ServiceBus.post(ServiceBus.Event.CAPTURE_LOST)
                stopSelf()
            }
        }, handler)

        imageReader = ImageReader.newInstance(screenW, screenH, PixelFormat.RGBA_8888, 2)
        virtualDisplay = proj.createVirtualDisplay(
            "bingbing-capture", screenW, screenH, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, handler
        )
        Logger.i("Capture", "采集就绪 ${screenW}x${screenH}")
        return START_STICKY
    }

    /**
     * 抓取当前帧（§3 FrameGrabber.grab()）。
     * 调用方在 Dispatchers.IO 上调用；无投影/无新帧 → null（§13 降级：提示重新授权）。
     */
    fun grabOnce(): Bitmap? {
        val reader = imageReader ?: return null
        val image: Image = reader.acquireLatestImage() ?: return null
        return try {
            val plane = image.planes[0]
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val rowBytes = screenW * pixelStride
            val bitmap = Bitmap.createBitmap(screenW, screenH, Bitmap.Config.ARGB_8888)
            val buffer = plane.buffer
            val rowPadding = rowStride - rowBytes
            if (rowPadding == 0) {
                bitmap.copyPixelsFromBuffer(buffer)
            } else {
                // rowStride 可能宽于 width*pixelStride：逐行拷贝到紧凑缓冲，避免花屏
                val staging = java.nio.ByteBuffer.allocateDirect(rowBytes * screenH)
                for (row in 0 until screenH) {
                    buffer.position(row * rowStride)
                    buffer.limit(row * rowStride + rowBytes)
                    staging.put(buffer)
                }
                staging.position(0)
                bitmap.copyPixelsFromBuffer(staging)
            }
            bitmap
        } catch (e: Exception) {
            Logger.e("Capture", "抓帧失败", e)
            null
        } finally {
            image.close()
        }
    }

    override fun onDestroy() {
        runCatching {
            virtualDisplay?.release()
            imageReader?.close()
            projection?.stop()
        }
        handlerThread?.quitSafely()
        instance = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.capture_notification_title))
            .setContentText(getString(R.string.capture_notification_text))
            .setSmallIcon(R.drawable.ic_stat_capture)
            .setOngoing(true)
            .build()

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.capture_channel_name), NotificationManager.IMPORTANCE_LOW)
        )
    }
}
