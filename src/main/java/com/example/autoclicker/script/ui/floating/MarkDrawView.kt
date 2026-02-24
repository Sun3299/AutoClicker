package com.example.autoclicker.script.ui.floating

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class MarkDrawView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val normalPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val swipeLinePaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val arrowPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 4f
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val tempPaint = Paint().apply {
        color = Color.argb(150, 0, 255, 0)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val maskPaint = Paint().apply {
        color = Color.argb(180, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private val crossLinePaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 1f
        isAntiAlias = true
    }

    private var clickFeedbackPoint: PointF? = null
    private val clickFeedbackPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val clickFeedbackCrossPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 1f
        isAntiAlias = true
    }

    // ==================== 新增：滑动反馈相关变量 ====================
    private var swipeFeedbackStartX: Int? = null
    private var swipeFeedbackStartY: Int? = null
    private var swipeFeedbackEndX: Int? = null
    private var swipeFeedbackEndY: Int? = null
    private val swipeFeedbackPaint = Paint().apply {
        color = Color.argb(200, 0, 255, 0) // 更亮的绿色，突出反馈
        strokeWidth = 5f
        isAntiAlias = true
    }
    private val swipeFeedbackArrowPaint = Paint().apply {
        color = Color.argb(200, 0, 255, 0)
        strokeWidth = 6f
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    // ==============================================================

    // ==================== 核心修改：添加真实屏幕尺寸属性 ====================
    private var realScreenWidth = 0
    private var realScreenHeight = 0

    // 设置真实屏幕尺寸的方法
    fun setRealScreenSize(width: Int, height: Int) {
        this.realScreenWidth = width
        this.realScreenHeight = height
        // 更新View的布局参数为真实屏幕尺寸
        val layoutParams = this.layoutParams
        if (layoutParams != null) {
            layoutParams.width = width
            layoutParams.height = height
            this.layoutParams = layoutParams
        }
        requestLayout()
        invalidate()
    }
    // ======================================================================

    var markList: List<MarkAreaItem> = emptyList()

    var previewStartX: Int? = null
    var previewStartY: Int? = null
    var previewEndX: Int? = null
    var previewEndY: Int? = null

    var isInSwipeMode = false
    var swipeTipText = "请点击起点位置"

    var isInAddClickMode = false
    private var startX = 0
    private var startY = 0
    val addClickTempRect = Rect()

    // ==================== 修改：使用真实屏幕密度计算，避免尺寸偏差 ====================
    private val halfCirclePx by lazy {
        (resources.displayMetrics.density * 18).toInt()
    }
    private val arrowSize by lazy {
        resources.displayMetrics.density * 12
    }
    // ===============================================================================

    var onMarkClicked: ((item: MarkAreaItem, isEndPoint: Boolean) -> Unit)? = null

    interface OnMarkDragListener {
        fun onDragStart(item: MarkAreaItem, isDragEnd: Boolean)
        fun onDragMove(item: MarkAreaItem, newLeft: Int, newTop: Int, isDragEnd: Boolean)
        fun onDragEnd()
    }

    var dragListener: OnMarkDragListener? = null
    var onScreenTap: ((x: Float, y: Float) -> Unit)? = null
    var onAddClickGesture: ((isDown: Boolean, isMove: Boolean, isUp: Boolean, rect: Rect) -> Unit)? = null

    private var currentDragItem: MarkAreaItem? = null
    private var isDragEndPoint = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var origLeft = 0
    private var origTop = 0

    private val touchSlop = 10
    private var isDragging = false

    // ==================== 核心：局部穿透 ====================
    fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isInAddClickMode || isInSwipeMode) {
            return true
        }
        val x = ev.x
        val y = ev.y
        val (hitItem, _) = findTouchedItem(x, y)
        return hitItem != null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        if (isInAddClickMode || isInSwipeMode) {
            if (isInAddClickMode) {
                handleAddClickTouch(event)
            } else if (event.action == MotionEvent.ACTION_UP) {
                onScreenTap?.invoke(x, y)
            }
            return true
        }

        val (hitItem, _) = findTouchedItem(x, y)
        if (hitItem != null) {
            return handleNormalTouch(event)
        }

        // 空白区域：不消费 → 穿透
        return false
    }
    // ======================================================

    // ==================== 核心修改：重写onMeasure，强制使用真实屏幕尺寸 ====================
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (realScreenWidth > 0 && realScreenHeight > 0) {
            // 使用真实屏幕尺寸作为View的尺寸
            setMeasuredDimension(realScreenWidth, realScreenHeight)
        } else {
            // 兼容模式：使用默认测量
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
    // ====================================================================================

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // ==================== 关键：确保绘制区域是全屏（包括导航栏） ====================
        val drawWidth = if (realScreenWidth > 0) realScreenWidth else width
        val drawHeight = if (realScreenHeight > 0) realScreenHeight else height
        // ===============================================================================

        if (isInAddClickMode || isInSwipeMode) {
            // 绘制全屏遮罩（包括导航栏）
            canvas.drawRect(0f, 0f, drawWidth.toFloat(), drawHeight.toFloat(), maskPaint)
        }

        if (isInAddClickMode) {
            canvas.drawText("框选点击区域", drawWidth / 2f, drawHeight / 2f - 100, textPaint)
        }
        if (isInSwipeMode) {
            canvas.drawText(swipeTipText, drawWidth / 2f, drawHeight / 2f - 100, textPaint)
        }

        markList.forEach { item ->
            when (item.actionType) {
                MarkAreaItem.ActionType.CLICK -> {
                    canvas.drawRect(item.rect, normalPaint)
                    val cx = item.rect.exactCenterX()
                    val cy = item.rect.exactCenterY()
                    canvas.drawText(item.actionNumber.toString(), cx, cy + textPaint.textSize / 3, textPaint)
                }

                MarkAreaItem.ActionType.SWIPE -> {
                    val sx = item.rect.exactCenterX()
                    val sy = item.rect.exactCenterY()
                    val r = item.rect.width() / 2f

                    canvas.drawCircle(sx, sy, r, normalPaint)
                    canvas.drawLine(sx - r, sy, sx + r, sy, crossLinePaint)
                    canvas.drawLine(sx, sy - r, sx, sy + r, crossLinePaint)
                    canvas.drawText(item.actionNumber.toString(), sx, sy + textPaint.textSize / 3, textPaint)

                    val ex = item.swipeEndRect.exactCenterX()
                    val ey = item.swipeEndRect.exactCenterY()
                    canvas.drawCircle(ex, ey, r, normalPaint)
                    canvas.drawLine(ex - r, ey, ex + r, ey, crossLinePaint)
                    canvas.drawLine(ex, ey - r, ex, ey + r, crossLinePaint)

                    canvas.drawLine(sx, sy, ex, ey, swipeLinePaint)
                    drawArrow(canvas, sx, sy, ex, ey, arrowPaint, arrowSize)
                }
            }
        }

        if (isInAddClickMode && addClickTempRect.width() > 0 && addClickTempRect.height() > 0) {
            canvas.drawRect(addClickTempRect, tempPaint)
        }

        val sx = previewStartX
        val sy = previewStartY
        val ex = previewEndX
        val ey = previewEndY
        if (sx != null && sy != null) {
            val r = halfCirclePx.toFloat()
            canvas.drawCircle(sx.toFloat(), sy.toFloat(), r, normalPaint)
            canvas.drawLine(sx - r, sy.toFloat(), sx + r, sy.toFloat(), crossLinePaint)
            canvas.drawLine(sx.toFloat(), sy - r, sx.toFloat(), sy + r, crossLinePaint)

            if (ex != null && ey != null) {
                canvas.drawCircle(ex.toFloat(), ey.toFloat(), r, normalPaint)
                canvas.drawLine(ex - r, ey.toFloat(), ex + r, ey.toFloat(), crossLinePaint)
                canvas.drawLine(ex.toFloat(), ey - r, ex.toFloat(), ey + r, crossLinePaint)
                canvas.drawLine(sx.toFloat(), sy.toFloat(), ex.toFloat(), ey.toFloat(), swipeLinePaint)
                drawArrow(canvas, sx.toFloat(), sy.toFloat(), ex.toFloat(), ey.toFloat(), arrowPaint, arrowSize)
            }
        }

        // 绘制点击反馈
        clickFeedbackPoint?.let { p ->
            val r = halfCirclePx.toFloat()
            canvas.drawCircle(p.x, p.y, r, clickFeedbackPaint)
            canvas.drawLine(p.x - r, p.y, p.x + r, p.y, clickFeedbackCrossPaint)
            canvas.drawLine(p.x, p.y - r, p.x, p.y + r, clickFeedbackCrossPaint)
        }

        // ==================== 新增：绘制滑动反馈 ====================
        val sfSx = swipeFeedbackStartX
        val sfSy = swipeFeedbackStartY
        val sfEx = swipeFeedbackEndX
        val sfEy = swipeFeedbackEndY
        if (sfSx != null && sfSy != null && sfEx != null && sfEy != null) {
            val r = halfCirclePx.toFloat()
            // 起点圆圈
            canvas.drawCircle(sfSx.toFloat(), sfSy.toFloat(), r, swipeFeedbackPaint)
            canvas.drawLine(sfSx - r, sfSy.toFloat(), sfSx + r, sfSy.toFloat(), crossLinePaint)
            canvas.drawLine(sfSx.toFloat(), sfSy - r, sfSx.toFloat(), sfSy + r, crossLinePaint)

            // 终点圆圈
            canvas.drawCircle(sfEx.toFloat(), sfEy.toFloat(), r, swipeFeedbackPaint)
            canvas.drawLine(sfEx - r, sfEy.toFloat(), sfEx + r, sfEy.toFloat(), crossLinePaint)
            canvas.drawLine(sfEx.toFloat(), sfEy - r, sfEx.toFloat(), sfEy + r, crossLinePaint)

            // 滑动轨迹线 + 箭头
            canvas.drawLine(sfSx.toFloat(), sfSy.toFloat(), sfEx.toFloat(), sfEy.toFloat(), swipeFeedbackPaint)
            drawArrow(canvas, sfSx.toFloat(), sfSy.toFloat(), sfEx.toFloat(), sfEy.toFloat(), swipeFeedbackArrowPaint, arrowSize * 1.2f)
        }
        // ==========================================================
    }

    // ==================== 优化：抽离drawArrow方法，支持自定义画笔和大小 ====================
    private fun drawArrow(
        canvas: Canvas,
        sx: Float,
        sy: Float,
        ex: Float,
        ey: Float,
        paint: Paint,
        arrowSize: Float
    ) {
        val angle = kotlin.math.atan2(ey - sy, ex - sx)
        val arrowLen = arrowSize

        val p1x = ex - arrowLen * cos(angle - Math.PI / 6).toFloat()
        val p1y = ey - arrowLen * sin(angle - Math.PI / 6).toFloat()
        val p2x = ex - arrowLen * cos(angle + Math.PI / 6).toFloat()
        val p2y = ey - arrowLen * sin(angle + Math.PI / 6).toFloat()

        val path = Path()
        path.moveTo(ex, ey)
        path.lineTo(p1x, p1y)
        path.lineTo(p2x, p2y)
        path.close()
        canvas.drawPath(path, paint)
    }
    // ====================================================================================

    // ==================== 新增：滑动反馈控制方法（供SwipeExecutor调用） ====================
    fun showSwipePath(startX: Int, startY: Int, endX: Int, endY: Int) {
        swipeFeedbackStartX = startX
        swipeFeedbackStartY = startY
        swipeFeedbackEndX = endX
        swipeFeedbackEndY = endY
        invalidate() // 触发重绘，显示反馈
    }

    fun hideSwipePath() {
        swipeFeedbackStartX = null
        swipeFeedbackStartY = null
        swipeFeedbackEndX = null
        swipeFeedbackEndY = null
        invalidate() // 触发重绘，隐藏反馈
    }
    // ====================================================================================

    private fun handleNormalTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val (hitItem, isEnd) = findTouchedItem(x = event.x, y = event.y)
                if (hitItem != null) {
                    currentDragItem = hitItem
                    isDragEndPoint = isEnd
                    dragStartX = event.x
                    dragStartY = event.y
                    origLeft = if (isEnd) hitItem.swipeEndRect.left else hitItem.rect.left
                    origTop = if (isEnd) hitItem.swipeEndRect.top else hitItem.rect.top
                    dragListener?.onDragStart(hitItem, isEnd)
                    isDragging = false
                    return true
                }
                currentDragItem = null
                isDragging = false
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                currentDragItem?.let { item ->
                    val dx = event.x - dragStartX
                    val dy = event.y - dragStartY
                    if (!isDragging && (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val newLeft = (origLeft + dx).toInt()
                        val newTop = (origTop + dy).toInt()
                        dragListener?.onDragMove(item, newLeft, newTop, isDragEndPoint)
                        return true
                    }
                }
                return false
            }

            MotionEvent.ACTION_UP -> {
                currentDragItem?.let { item ->
                    if (!isDragging) {
                        val (_, isEnd) = findTouchedItem(event.x, event.y)
                        onMarkClicked?.invoke(item, isEnd)
                    }
                    dragListener?.onDragEnd()
                    currentDragItem = null
                    isDragging = false
                    return true
                }
                isDragging = false
                return false
            }

            MotionEvent.ACTION_CANCEL -> {
                currentDragItem = null
                isDragging = false
                return false
            }
        }
        return false
    }

    private fun findTouchedItem(x: Float, y: Float): Pair<MarkAreaItem?, Boolean> {
        markList.forEach { item ->
            if (item.actionType == MarkAreaItem.ActionType.SWIPE) {
                if (item.swipeEndRect.contains(x.toInt(), y.toInt())) {
                    return Pair(item, true)
                }
            }
        }
        markList.forEach { item ->
            if (item.rect.contains(x.toInt(), y.toInt())) {
                return Pair(item, false)
            }
        }
        return Pair(null, false)
    }

    private fun handleAddClickTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x.toInt()
                startY = event.y.toInt()
                addClickTempRect.set(startX, startY, startX, startY)
                onAddClickGesture?.invoke(true, false, false, Rect(addClickTempRect))
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val ex = event.x.toInt()
                val ey = event.y.toInt()
                val l = startX.coerceAtMost(ex)
                val t = startY.coerceAtMost(ey)
                val r = startX.coerceAtLeast(ex)
                val b = startY.coerceAtLeast(ey)
                addClickTempRect.set(l, t, r, b)
                onAddClickGesture?.invoke(false, true, false, Rect(addClickTempRect))
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onAddClickGesture?.invoke(false, false, true, Rect(addClickTempRect))
                resetAddClickMode()
                invalidate()
            }
        }
    }

    fun resetAddClickMode() {
        isInAddClickMode = false
        addClickTempRect.setEmpty()
        startX = 0
        startY = 0
    }

    fun resetSwipeMode() {
        isInSwipeMode = false
        swipeTipText = "请点击起点位置"
        previewStartX = null
        previewStartY = null
        previewEndX = null
        previewEndY = null
    }

    fun forceResetAllModes() {
        resetAddClickMode()
        resetSwipeMode()
        clickFeedbackPoint = null
        // 新增：重置滑动反馈
        hideSwipePath()
        currentDragItem = null
        isDragging = false
        invalidate()
    }

    fun showClickPoint(x: Int, y: Int) {
        clickFeedbackPoint = PointF(x.toFloat(), y.toFloat())
        invalidate()
    }

    fun hideClickPoint() {
        clickFeedbackPoint = null
        invalidate()
    }
}