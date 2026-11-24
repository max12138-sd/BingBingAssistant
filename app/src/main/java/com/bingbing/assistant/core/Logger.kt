package com.bingbing.assistant.core

import android.content.Context
import android.util.Log
import java.io.File

/**
 * 本地环形日志（L1：绝不联网）。
 * - 内存环形缓冲 500 条，设置页可复制/导出到 cacheDir/bingbing_log.txt
 * - 同时镜像到 logcat（tag 统一前缀便于 adb 过滤）
 */
object Logger {

    private const val TAG = "BingBing"
    private const val CAPACITY = 500

    private val ring = ArrayDeque<String>(CAPACITY)
    private var appDir: File? = null

    fun init(context: Context) {
        appDir = context.cacheDir
    }

    fun i(tag: String, msg: String) = log('I', tag, msg)
    fun w(tag: String, msg: String) = log('W', tag, msg)
    fun e(tag: String, msg: String, tr: Throwable? = null) = log('E', tag, msg + (tr?.let { " :: ${it.message}" } ?: ""))

    private fun log(level: Char, tag: String, msg: String) {
        val line = "${'['}$level] ${ts()} $tag: $msg"
        synchronized(ring) {
            if (ring.size >= CAPACITY) ring.removeFirst()
            ring.addLast(line)
        }
        when (level) {
            'I' -> Log.i(TAG, "[$tag] $msg")
            'W' -> Log.w(TAG, "[$tag] $msg")
            else -> Log.e(TAG, "[$tag] $msg")
        }
    }

    fun dump(): String = synchronized(ring) { ring.joinToString("\n") }

    /** 导出到应用缓存目录（用户手动分享，无自动外发） */
    fun dumpToFile(): File? {
        val dir = appDir ?: return null
        return runCatching {
            File(dir, "bingbing_log.txt").apply { writeText(dump()) }
        }.getOrNull()
    }

    private fun ts(): String =
        java.text.SimpleDateFormat("MM-dd HH:mm:ss.SSS", java.util.Locale.US)
            .format(java.util.Date())
}
