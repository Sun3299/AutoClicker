package com.example.autoclicker.script.ui.floating
import android.graphics.Rect
import android.view.View

// MarkAreaItem.kt
data class MarkAreaItem(
    val rect: Rect,
    val actionType: ActionType,
    var actionNumber: Int,
    var actionIndex: Int,
    val swipeEndRect: Rect = Rect(),
    // 拖动相关临时变量
    var isDragging: Boolean = false,
    var dragStartX: Float = 0f,
    var dragStartY: Float = 0f,
    var rectStartLeft: Int = 0,
    var rectStartTop: Int = 0,
    var isEndDragging: Boolean = false,
    var endDragStartX: Float = 0f,
    var endDragStartY: Float = 0f,
    var endRectStartLeft: Int = 0,
    var endRectStartTop: Int = 0,
    // 视图相关（已移除，绘制层不再依赖View）
    var swipeStartView: View? = null,
    var swipeEndView: View? = null,
    var viewX: Int = 0,
    var viewY: Int = 0,
    var endViewX: Int = 0,
    var endViewY: Int = 0
) {
    enum class ActionType {
        CLICK, SWIPE
    }
}