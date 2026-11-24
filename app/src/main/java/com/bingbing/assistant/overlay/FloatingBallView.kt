package com.bingbing.assistant.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View

/**
 * 悬浮球（§7.1）：
 * - 48dp 圆形，长按 200ms 进入拖动，松手吸附最近边缘
 * - 静置 3s 降透明度到 40%，触摸恢复
 * - 短按 = 点击，交 ServiceBus 打开面板
 */
class FloatingBallView(context: Context) : View(context) {

    interface Listener {
        /** 长按生效、进入拖动的第一帧（rawX/rawY 为该帧屏幕坐标） */
        fun onDragStart(rawX: Float, rawY: Float)
        fun onDragMove(rawX: Float, rawY: Float)
        fun onDragEnd(rawX: Float, rawY: Float)
        fun onBallClick()
    }

    var listener: Listener? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4C8DFF.toInt()   // §8.2 强调色
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 44f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val handler = Handler(Looper.getMainLooper())
    private var longPressed = false
    private var downRawX = 0f
    private var downRawY = 0f
    private var touchSlop = 0f

    private val idleRunnable = Runnable {
        animate().alpha(0.4f).setDuration(300).start()   // 3s 后透明 40%
    }
    private val longPressRunnable = Runnable {
        longPressed = true
        listener?.onDragStart(downRawX, downRawY)
    }

    init {
        touchSlop = 24f * resources.displayMetrics.density
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                longPressed = false
                animate().cancel()
                alpha = 1f
                handler.removeCallbacks(idleRunnable)
                handler.postDelayed(longPressRunnable, 200)
            }
            MotionEvent.ACTION_MOVE -> {
                if (longPressed) {
                    listener?.onDragMove(event.rawX, event.rawY)
                } else if (kotlin.math.hypot(
                        (event.rawX - downRawX).toDouble(),
                        (event.rawY - downRawY).toDouble()
                    ) > touchSlop
                ) {
                    handler.removeCallbacks(longPressRunnable)   // 提前滑动取消长按
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                if (longPressed && event.actionMasked == MotionEvent.ACTION_UP) {
                    listener?.onDragEnd(event.rawX, event.rawY)
                } else if (event.actionMasked == MotionEvent.ACTION_UP && !longPressed) {
                    listener?.onBallClick()
                }
                longPressed = false
                handler.postDelayed(idleRunnable, 3000)
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        val r = width / 2f
        canvas.drawCircle(r, r, r - 2f, paint)
        canvas.drawText("冰", r, r - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }
}
