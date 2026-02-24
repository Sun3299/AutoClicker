package com.example.autoclicker.script.executor

import android.os.Handler
import android.os.Looper
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import com.example.autoclicker.script.model.ScriptAction
import com.example.autoclicker.script.ui.floating.MarkDrawView
import com.example.autoclicker.service.AutoClickAccessibilityService
import java.util.Random

object SwipeExecutor {
    private val random = Random()
    private val mainHandler = Handler(Looper.getMainLooper())
    private const val FEEDBACK_DURATION = 500L
    private const val MIN_DURATION = 10L

    fun execute(swipeAction: ScriptAction.Swipe, drawView: MarkDrawView) {
        // 1. 安全区间：确保最小 >=10，最大不小于最小
        val safeMin = swipeAction.durationMinMs.coerceAtLeast(MIN_DURATION)
        val safeMax = swipeAction.durationMaxMs.coerceAtLeast(safeMin)

        // 2. 严格生成 [safeMin, safeMax] 之间的随机数
        val duration = nextLong(random, safeMin, safeMax)

        // 3. 显示滑动路径
        mainHandler.post {
            drawView.showSwipePath(
                startX = swipeAction.startX,
                startY = swipeAction.startY,
                endX = swipeAction.endX,
                endY = swipeAction.endY
            )
        }

        // 4. 执行滑动
        realSwipe(
            startX = swipeAction.startX,
            startY = swipeAction.startY,
            endX = swipeAction.endX,
            endY = swipeAction.endY,
            duration = duration
        )

        // 5. 隐藏反馈
        mainHandler.postDelayed({
            drawView.hideSwipePath()
        }, FEEDBACK_DURATION)
    }

    private fun realSwipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long) {
        if (duration <= 0) return
        val service = AutoClickAccessibilityService.instance ?: return

        val path = Path().apply {
            moveTo(startX.toFloat(), startY.toFloat())
            lineTo(endX.toFloat(), endY.toFloat())
        }

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(path, 0, duration)
            )
            .build()

        service.dispatchGesture(gesture, null, null)
    }

    /**
     * 严格生成 [min, max] 闭区间内的随机数
     * 100% 不会超范围、不会为0、不会负数
     */
    private fun nextLong(random: Random, min: Long, max: Long): Long {
        if (min == max) return min
        val range = max - min + 1
        return min + Math.abs(random.nextLong() % range)
    }
}