package com.bingbing.assistant.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * 极简事件总线（§3 core/ServiceBus.kt）：SharedFlow 单例。
 * 只用于悬浮球 / 截图服务 / 面板之间的解耦通知，禁止拿来传大数据。
 */
object ServiceBus {

    enum class Event {
        /** 截图授权流程相关 */
        CAPTURE_AUTHORIZED,
        CAPTURE_LOST,

        /** 悬浮球 ↔ 面板 */
        BALL_CLICKED,
        PANEL_DISMISSED,

        /** 功能入口（面板按钮 → Flow 处理） */
        SCAN_ARENA,
        SCAN_PEAK,
        SCAN_CALENDAR,

        /** 数据变化 */
        POOL_CHANGED,
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 16)
    val events: SharedFlow<Event> = _events

    fun post(event: Event) {
        _events.tryEmit(event)
    }
}
