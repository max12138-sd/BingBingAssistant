package com.bingbing.assistant.overlay

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.bingbing.assistant.core.ServiceBus
import com.bingbing.assistant.ui.MyPoolActivity
import com.bingbing.assistant.ui.SettingsActivity

/**
 * 面板控制器（§7.1 / §8.3）：展开/收起/内容路由。
 * M2：先做 5 入口骨架页（宽 280dp、#CC12141A、高 ≤ 屏高 70%）。
 * M3+：竞技场结果/巅峰结果/活动列表/修正面板等子页在 ui/overlay 包实现后挂进这里。
 */
class PanelController(private val service: Service, private val wm: WindowManager) {

    private var panelView: LinearLayout? = null

    fun toggle() {
        if (panelView != null) dismiss() else show()
    }

    fun show() {
        if (panelView != null) return
        val ctx = service
        val dp = ctx.resources.displayMetrics.density
        val title = TextView(ctx).apply {
            text = "冰冰助手"
            textSize = 18f
            setTextColor(0xFFE8EAF0.toInt())
            typeface = Typeface.DEFAULT_BOLD
            setPadding((12 * dp).toInt(), (10 * dp).toInt(), (12 * dp).toInt(), (6 * dp).toInt())
        }
        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xCC12141A.toInt())           // §8.2 面板底色
                cornerRadius = 12 * dp                  // 圆角 12dp
            }
            addView(title)
            addView(entry(ctx, "竞技场阵容") { ServiceBus.post(ServiceBus.Event.SCAN_ARENA) })
            addView(entry(ctx, "巅峰方案") { ServiceBus.post(ServiceBus.Event.SCAN_PEAK) })
            addView(entry(ctx, "7日活动") { ServiceBus.post(ServiceBus.Event.SCAN_CALENDAR) })
            addView(entry(ctx, "我的英雄库") {
                ctx.startActivity(
                    Intent(ctx, MyPoolActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                dismiss()
            })
            addView(entry(ctx, "设置") {
                ctx.startActivity(
                    Intent(ctx, SettingsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                dismiss()
            })
        }

        val params = WindowManager.LayoutParams(
            (280 * dp).toInt(),                        // §7.1 宽 280dp
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            Color.TRANSPARENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        panelView = body
        wm.addView(body, params)
    }

    fun dismiss() {
        panelView?.let { runCatching { wm.removeView(it) } }
        panelView = null
    }

    private fun entry(ctx: android.content.Context, text: String, onClick: () -> Unit): View =
        TextView(ctx).apply {
            this.text = text
            textSize = 15f
            setTextColor(0xFFE8EAF0.toInt())
            val pad = (12 * dpOf(ctx)).toInt()
            setPadding(pad, pad / 2, pad, pad / 2)
            setOnClickListener { onClick() }
        }

    private fun dpOf(ctx: android.content.Context) = ctx.resources.displayMetrics.density
}
