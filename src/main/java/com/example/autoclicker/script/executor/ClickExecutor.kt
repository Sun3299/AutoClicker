package com.example.autoclicker.script.executor

import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import com.example.autoclicker.script.model.ScriptAction
import com.example.autoclicker.script.ui.floating.MarkDrawView
import com.example.autoclicker.service.AutoClickAccessibilityService
import java.util.Random

object ClickExecutor {
    private val random = Random()
    private val mainHandler = Handler(Looper.getMainLooper())
    private const val FEEDBACK_DURATION = 300L
    private const val MIN_CLICK_DURATION = 10L

    fun execute(clickAction: ScriptAction.Click, drawView: MarkDrawView) {
        val point = getRandomPointInRect(clickAction.clickRect)

        mainHandler.post {
            drawView.showClickPoint(point.x, point.y)
        }

        realClick(point.x, point.y)

        mainHandler.postDelayed({
            drawView.hideClickPoint()
        }, FEEDBACK_DURATION)
    }

    private fun getRandomPointInRect(rect: android.graphics.Rect): Point {
        val x = if (rect.left == rect.right) rect.left
        else rect.left + random.nextInt(rect.right - rect.left)

        val y = if (rect.top == rect.bottom) rect.top
        else rect.top + random.nextInt(rect.bottom - rect.top)

        return Point(x, y)
    }

    private fun realClick(x: Int, y: Int) {
        val service = AutoClickAccessibilityService.instance ?: return

        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(path, 0, MIN_CLICK_DURATION)
            )
            .build()

        service.dispatchGesture(gesture, null, null)
    }
}