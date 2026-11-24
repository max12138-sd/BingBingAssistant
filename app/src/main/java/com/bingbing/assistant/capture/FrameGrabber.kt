package com.bingbing.assistant.capture

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 单帧获取（§3 capture/FrameGrabber.kt）：suspend fun grab(): Bitmap?
 * 需先经 PermissionGuide 完成投影授权并启动 ScreenCaptureService。
 */
class FrameGrabber(private val context: Context) {

    val isReady: Boolean get() = ScreenCaptureService.isRunning

    suspend fun grab(): Bitmap? = withContext(Dispatchers.IO) {
        ScreenCaptureService.grabFrame()
    }

    /** M2 验收辅助：存一张调试截图到应用专属目录（app/debug-capture/，不进相册） */
    suspend fun grabAndSave(): File? {
        val bmp = grab() ?: return null
        return withContext(Dispatchers.IO) {
            val dir = File(context.getExternalFilesDir(null), "debug-capture").apply { mkdirs() }
            val out = File(dir, "capture_${System.currentTimeMillis()}.png")
            runCatching {
                out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }.map { out }.getOrNull()
        }
    }
}
