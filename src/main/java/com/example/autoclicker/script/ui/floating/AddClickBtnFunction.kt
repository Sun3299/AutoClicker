package com.example.autoclicker.script.ui.floating

import android.content.Context
import android.graphics.Rect
import android.widget.Toast

class AddClickBtnFunction(
    private val context: Context,
    private val markDrawView: MarkDrawView,
    private val onComplete: (Rect) -> Unit
) {
    private var isAdding = false

    fun start() {
        if (isAdding) return
        isAdding = true
        markDrawView.isInAddClickMode = true
        markDrawView.addClickTempRect.setEmpty()

        markDrawView.onAddClickGesture = { isDown, isMove, isUp, rect ->
            if (isUp) {
                isAdding = false
                markDrawView.isInAddClickMode = false
                markDrawView.addClickTempRect.setEmpty()
                markDrawView.onAddClickGesture = null

                val validRect = rect.apply {
                    left = left.coerceAtLeast(0)
                    top = top.coerceAtLeast(0)
                    right = right.coerceAtMost(markDrawView.width)
                    bottom = bottom.coerceAtMost(markDrawView.height)
                }

                if (validRect.width() > 15 && validRect.height() > 15) {
                    onComplete(validRect)
                } else {
                    Toast.makeText(context, "区域太小，请重新框选", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}