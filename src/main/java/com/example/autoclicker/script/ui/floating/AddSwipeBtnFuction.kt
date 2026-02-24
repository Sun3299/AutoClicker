package com.example.autoclicker.script.ui.floating

class AddSwipeBtnFunction(
    val onSelecting: (
        isSelectStart: Boolean,
        startX: Int?, startY: Int?,
        endX: Int?, endY: Int?
    ) -> Unit,
    val onSelected: (startX: Int, startY: Int, endX: Int, endY: Int) -> Unit
) {
    private var isSelectingStart = true
    private var startX: Int? = null
    private var startY: Int? = null

    fun onScreenClick(x: Int, y: Int) {
        if (isSelectingStart) {
            startX = x
            startY = y
            isSelectingStart = false
            onSelecting(false, startX, startY, null, null)
        } else {
            onSelecting(false, startX, startY, x, y)
            onSelected(startX ?: 0, startY ?: 0, x, y)
            reset()
        }
    }

    private fun reset() {
        isSelectingStart = true
        startX = null
        startY = null
    }
}